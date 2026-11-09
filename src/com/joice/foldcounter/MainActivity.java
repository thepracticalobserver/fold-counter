package com.joice.foldcounter;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Main screen, styled like the Unlock Counter app. */
public class MainActivity extends Activity {
    private boolean light;
    private ScrollView root;

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // palette
    private int bg() { return light ? 0xFFF1EFEA : 0xFF000000; }
    private int card() { return light ? 0xFFE2DFD7 : 0xFF111113; }
    private int stroke() { return light ? 0xFFD3CFC5 : 0xFF1F1F22; }
    private int text() { return light ? 0xFF2B2E33 : 0xFFFFFFFF; }
    private int dim() { return light ? 0xFF7F8488 : 0xFF8A8F94; }
    private int accent() { return light ? 0xFF6FAE7F : 0xFFA8D88A; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        light = prefs().getBoolean("app_light", false);
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private SharedPreferences prefs() { return getSharedPreferences("ui", MODE_PRIVATE); }

    @Override protected void onResume() {
        super.onResume();
        FoldService.start(this);
        build();
        // The service reports whether it found a sensor; refresh shortly after first start.
        getWindow().getDecorView().postDelayed(this::build, 500);
    }

    private GradientDrawable round(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView tv(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private void build() {
        Context c = this;
        getWindow().setStatusBarColor(bg());
        getWindow().setNavigationBarColor(bg());
        getWindow().getDecorView().setSystemUiVisibility(light
                ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(64), dp(20), dp(32));

        // Title row with theme toggle
        FrameLayout top = new FrameLayout(c);
        LinearLayout titles = new LinearLayout(c);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(tv("Unfold Counter", 34, text(), false));
        titles.addView(tv("Every unfold, counted live", 15, dim(), false));
        top.addView(titles);
        TextView toggle = tv(light ? "☀" : "☾", 22, light ? 0xFFE0A030 : 0xFFF0A860, false);
        toggle.setGravity(Gravity.CENTER);
        toggle.setBackground(round(card(), stroke(), 24));
        toggle.setOnClickListener(v -> {
            light = !light;
            prefs().edit().putBoolean("app_light", light).apply();
            build();
        });
        top.addView(toggle, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.END | Gravity.TOP));
        col.addView(top);

        // Preview card
        FrameLayout prev = new FrameLayout(c);
        prev.setBackground(round(card(), stroke(), 28));
        ImageView tile = new ImageView(c);
        tile.setImageBitmap(FoldRenderer.small(c, light));
        prev.addView(tile, new FrameLayout.LayoutParams(dp(190), dp(190), Gravity.CENTER));
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(250));
        pl.topMargin = dp(20);
        col.addView(prev, pl);

        // Section
        TextView sec = tv("TODAY", 13, dim(), false);
        sec.setLetterSpacing(0.15f);
        sec.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sl.topMargin = dp(28);
        sl.bottomMargin = dp(10);
        col.addView(sec, sl);

        int today = FoldStore.count(c, false, 0), yest = FoldStore.count(c, false, 1);
        int d = today - yest;
        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(round(card(), stroke(), 24));
        row(list, "Unfolds today", String.valueOf(today), text(), false);
        row(list, "Yesterday", String.valueOf(yest), text(), false);
        row(list, "Change", (d > 0 ? "↑ " : d < 0 ? "↓ " : "") + Math.abs(d),
                d < 0 ? (light ? 0xFFC9695F : 0xFFFF8A7A) : (light ? 0xFF3F917F : 0xFFA8D88A), false);
        row(list, "All time", String.valueOf(FoldStore.total(c, false)), text(), false);
        row(list, "This week", String.valueOf(FoldStore.weekTotal(c, false)), text(), true);
        col.addView(list);

        // Add widget button
        Button add = new Button(c);
        add.setAllCaps(false);
        add.setText("Add widget to home screen");
        add.setTextSize(18);
        add.setTextColor(0xFF10140F);
        add.setBackground(round(accent(), 0, 32));
        add.setOnClickListener(v -> pin(light ? FoldWidgetSmallLight.class : FoldWidgetSmall.class));
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64));
        al.topMargin = dp(24);
        col.addView(add, al);

        TextView wideLink = tv("Or add the 4×2 weekly widget", 15, dim(), false);
        wideLink.setGravity(Gravity.CENTER);
        wideLink.setPadding(0, dp(16), 0, dp(8));
        wideLink.setOnClickListener(v -> pin(light ? FoldWidgetWideLight.class : FoldWidgetWide.class));
        col.addView(wideLink);

        boolean has = FoldStore.hasSensor(c);
        float a = FoldStore.lastAngle(c);
        TextView status = tv(has
                ? "Hinge sensor active" + (a >= 0 ? " – angle " + Math.round(a) + "°" : "")
                : "No hinge sensor on this phone, so unfolds can't be detected here.", 13, dim(), false);
        status.setGravity(Gravity.CENTER);
        col.addView(status);
        if (!has) {
            Button t = new Button(c);
            t.setAllCaps(false);
            t.setText("Test: +1 unfold");
            t.setTextSize(15);
            t.setTextColor(dim());
            t.setStateListAnimator(null);
            t.setBackground(round(card(), stroke(), 26));
            t.setOnClickListener(v -> { FoldStore.record(this, false); build(); });
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
            tl.topMargin = dp(16);
            col.addView(t, tl);
        }

        if (root == null) root = new ScrollView(c);
        root.removeAllViews();
        root.addView(col);
        root.setBackgroundColor(bg());
        if (root.getParent() == null) setContentView(root);
    }

    private void row(LinearLayout list, String label, String value, int valueColor, boolean last) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(24), 0, dp(24), 0);
        TextView l = tv(label, 18, text(), false);
        r.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        r.addView(tv(value, 18, valueColor, true));
        list.addView(r, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(62)));
        if (!last) {
            View div = new View(this);
            div.setBackgroundColor(stroke());
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            dl.leftMargin = dp(24);
            list.addView(div, dl);
        }
    }

    private void pin(Class<?> cls) {
        AppWidgetManager m = getSystemService(AppWidgetManager.class);
        if (m != null && m.isRequestPinAppWidgetSupported()) {
            m.requestPinAppWidget(new ComponentName(this, cls), null, null);
        } else {
            Toast.makeText(this, "Long-press the home screen → Widgets → Fold Counter", Toast.LENGTH_LONG).show();
        }
    }
}
