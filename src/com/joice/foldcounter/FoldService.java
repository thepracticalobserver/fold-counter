package com.joice.foldcounter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

/**
 * Foreground service that watches the hinge-angle sensor and counts a fold
 * when the hinge closes and an unfold when it opens again (with hysteresis).
 */
public class FoldService extends Service implements SensorEventListener {
    private static final String CHANNEL = "fold_counter";
    /** At or below this angle the phone counts as closed. */
    static final float CLOSED_MAX = 15f;
    /** At or above this angle the phone counts as opened. */
    static final float OPEN_MIN = 45f;

    private SensorManager sm;
    private final BroadcastReceiver dateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { FoldWidgetBase.updateAll(c); }
    };

    static void start(Context c) {
        try {
            c.getApplicationContext().startForegroundService(new Intent(c, FoldService.class));
        } catch (RuntimeException e) {
            // Background start not allowed right now; the app or next boot will start it.
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Fold counter",
                NotificationManager.IMPORTANCE_MIN));
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Counting folds")
                .setContentText("Listening to the hinge")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        sm = getSystemService(SensorManager.class);
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE, true);
        if (s == null) s = sm.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE);
        FoldStore.setHasSensor(this, s != null);
        if (s != null) sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_DATE_CHANGED);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(dateReceiver, f);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onSensorChanged(SensorEvent e) {
        float angle = e.values[0];
        FoldStore.setLastAngle(this, angle);
        Boolean open = FoldStore.isOpen(this);
        if (open == null) {
            // First reading: learn the current state without counting it.
            FoldStore.setOpen(this, angle > CLOSED_MAX);
        } else if (open && angle <= CLOSED_MAX) {
            FoldStore.setOpen(this, false);
            FoldStore.record(this, true);
        } else if (!open && angle >= OPEN_MIN) {
            FoldStore.setOpen(this, true);
            FoldStore.record(this, false);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override public void onDestroy() {
        if (sm != null) sm.unregisterListener(this);
        try { unregisterReceiver(dateReceiver); } catch (RuntimeException ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
