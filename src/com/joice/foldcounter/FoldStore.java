package com.joice.foldcounter;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

/** Persists fold/unfold counts per day plus all-time totals. */
final class FoldStore {
    private static final String PREFS = "fold_counts";

    private FoldStore() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String key(boolean fold, Calendar day) {
        return String.format(Locale.US, "%s_%04d-%02d-%02d", fold ? "f" : "u",
                day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
    }

    static synchronized void record(Context c, boolean fold) {
        SharedPreferences p = sp(c);
        String k = key(fold, Calendar.getInstance());
        String total = fold ? "total_f" : "total_u";
        p.edit().putInt(k, p.getInt(k, 0) + 1).putInt(total, p.getInt(total, 0) + 1).apply();
        FoldWidgetBase.updateAll(c);
    }

    static int count(Context c, boolean fold, Calendar day) {
        return sp(c).getInt(key(fold, day), 0);
    }

    /** daysAgo = 0 is today. */
    static int count(Context c, boolean fold, int daysAgo) {
        Calendar d = Calendar.getInstance();
        d.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return count(c, fold, d);
    }

    static int total(Context c, boolean fold) {
        return sp(c).getInt(fold ? "total_f" : "total_u", 0);
    }

    /** Monday of the current week (Mon-Sun weeks). */
    static Calendar weekStart() {
        Calendar d = Calendar.getInstance();
        d.setFirstDayOfWeek(Calendar.MONDAY);
        d.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return d;
    }

    /** 0 = Monday ... 6 = Sunday. */
    static int todayIndex() {
        return (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    static int weekCount(Context c, boolean fold, int index) {
        Calendar d = weekStart();
        d.add(Calendar.DAY_OF_YEAR, index);
        return count(c, fold, d);
    }

    static int weekTotal(Context c, boolean fold) {
        int sum = 0;
        for (int i = 0; i <= todayIndex(); i++) sum += weekCount(c, fold, i);
        return sum;
    }

    // --- hinge state bookkeeping ---
    static Boolean isOpen(Context c) {
        SharedPreferences p = sp(c);
        return p.contains("open") ? p.getBoolean("open", true) : null;
    }

    static void setOpen(Context c, boolean open) {
        sp(c).edit().putBoolean("open", open).apply();
    }

    static boolean hasSensor(Context c) {
        return sp(c).getBoolean("has_sensor", false);
    }

    static void setHasSensor(Context c, boolean has) {
        sp(c).edit().putBoolean("has_sensor", has).apply();
    }

    static float lastAngle(Context c) {
        return sp(c).getFloat("angle", -1f);
    }

    static void setLastAngle(Context c, float a) {
        sp(c).edit().putFloat("angle", a).apply();
    }
}
