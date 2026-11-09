package com.joice.foldcounter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

abstract class FoldWidgetBase extends AppWidgetProvider {
    abstract boolean wide();
    boolean light() { return false; }

    @Override public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        render(c, mgr, ids, wide(), light());
    }

    @Override public void onEnabled(Context c) {
        FoldService.start(c);
    }

    @Override public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            FoldService.start(c);
        }
    }

    static void render(Context c, AppWidgetManager mgr, int[] ids, boolean wide, boolean light) {
        if (ids.length == 0) return;
        Bitmap bmp = wide ? FoldRenderer.wide(c, light) : FoldRenderer.small(c, light);
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget);
            rv.setImageViewBitmap(R.id.img, bmp);
            rv.setOnClickPendingIntent(R.id.root, open);
            mgr.updateAppWidget(id, rv);
        }
    }

    static void updateAll(Context c) {
        Context app = c.getApplicationContext();
        AppWidgetManager mgr = AppWidgetManager.getInstance(app);
        render(app, mgr, mgr.getAppWidgetIds(new ComponentName(app, FoldWidgetSmall.class)), false, false);
        render(app, mgr, mgr.getAppWidgetIds(new ComponentName(app, FoldWidgetWide.class)), true, false);
        render(app, mgr, mgr.getAppWidgetIds(new ComponentName(app, FoldWidgetSmallLight.class)), false, true);
        render(app, mgr, mgr.getAppWidgetIds(new ComponentName(app, FoldWidgetWideLight.class)), true, true);
    }
}
