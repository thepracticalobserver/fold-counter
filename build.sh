#!/bin/bash
# Builds FoldCounter.apk without Gradle (aapt2 + javac + d8 + apksigner).
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}
BT=$SDK/build-tools/${BT_VER:-36.0.0}
JAR=$SDK/platforms/android-${PLATFORM:-36}/android.jar
export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home -v 17)}
export PATH=$JAVA_HOME/bin:$PATH
rm -rf build && mkdir -p build/{res,gen,classes,dex}
$BT/aapt2 compile --dir res -o build/res
$BT/aapt2 link -o build/base.apk -I $JAR --manifest AndroidManifest.xml \
  --min-sdk-version 31 --target-sdk-version 34 --version-code 1 --version-name 1.0.0 \
  --java build/gen build/res/*.flat
javac -source 8 -target 8 -Xlint:-options -classpath $JAR -d build/classes \
  $(find src build/gen -name '*.java')
$BT/d8 --lib $JAR --min-api 31 --output build/dex $(find build/classes -name '*.class')
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q -j ../unsigned.apk classes.dex)
$BT/zipalign -f 4 build/unsigned.apk build/aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias key -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=FoldCounter" >/dev/null 2>&1
$BT/apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out FoldCounter.apk build/aligned.apk
echo BUILT: $(pwd)/FoldCounter.apk
