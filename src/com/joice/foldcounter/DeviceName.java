package com.joice.foldcounter;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.Locale;

/** Short, upper-case marketing name of this phone, e.g. "S24 ULTRA" or "FOLD8 ULTRA". */
final class DeviceName {
    private static String cached;

    private DeviceName() {}

    /** Samsung model-number prefixes (without region suffix) -> short name. */
    private static final String[][] SAMSUNG = {
            {"SM-F976", "FOLD8 ULTRA"}, {"SM-F966", "FOLD7"}, {"SM-F956", "FOLD6"},
            {"SM-F946", "FOLD5"}, {"SM-F936", "FOLD4"}, {"SM-F926", "FOLD3"},
            {"SM-F766", "FLIP7"}, {"SM-F741", "FLIP6"}, {"SM-F731", "FLIP5"},
            {"SM-S938", "S25 ULTRA"}, {"SM-S936", "S25+"}, {"SM-S931", "S25"},
            {"SM-S928", "S24 ULTRA"}, {"SM-S926", "S24+"}, {"SM-S921", "S24"},
            {"SM-S918", "S23 ULTRA"}, {"SM-S916", "S23+"}, {"SM-S911", "S23"},
    };

    static synchronized String get() {
        if (cached != null) return cached;
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        String name = null;
        String up = model.toUpperCase(Locale.US);
        for (String[] row : SAMSUNG) {
            if (up.startsWith(row[0])) { name = row[1]; break; }
        }
        if (name == null) name = clean(prop("ro.product.marketname"));
        if (name == null) name = clean(model);
        cached = name == null ? "PHONE" : name.toUpperCase(Locale.US);
        return cached;
    }

    private static String clean(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        String l = s.toLowerCase(Locale.US);
        if (l.startsWith("samsung ")) s = s.substring(8);
        if (s.toLowerCase(Locale.US).startsWith("galaxy ")) s = s.substring(7);
        return s.trim().isEmpty() ? null : s.trim();
    }

    private static String prop(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method m = sp.getMethod("get", String.class);
            return (String) m.invoke(null, key);
        } catch (Throwable t) {
            return null;
        }
    }
}
