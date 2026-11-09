package com.joice.foldcounter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Draws the two tiles (2x2 and 4x2) as bitmaps, in the style of the unlock-counter widgets. */
final class FoldRenderer {
    private static int BG, HEAD, WHITE, FOLD, DIM, GRAY, UP, DOWN;

    private static void apply(boolean light) {
        if (light) {
            BG = 0xFFF5F2EB; HEAD = 0xFF4A8497; WHITE = 0xFF2B2E33; FOLD = 0xFF5BA396;
            DIM = 0xFFD0CCC3; GRAY = 0xFF858A8E; UP = 0xFF3F917F; DOWN = 0xFFC9695F;
        } else {
            BG = 0xFF000000; HEAD = 0xFF86C5E0; WHITE = 0xFFFFFFFF; FOLD = 0xFF7ED3C4;
            DIM = 0xFF3A4046; GRAY = 0xFF6B737A; UP = 0xFF7ED3C4; DOWN = 0xFFFF8A7A;
        }
    }

    private FoldRenderer() {}

    private static Paint paint(int color, float size, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setTextAlign(align);
        p.setTypeface(bold ? Typeface.create("sans-serif", Typeface.BOLD)
                : Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return p;
    }

    /** Draws text, shrinking it until it fits maxWidth. */
    private static void fitText(Canvas c, String s, float x, float y, float maxW, Paint p) {
        while (p.measureText(s) > maxW && p.getTextSize() > 12) p.setTextSize(p.getTextSize() - 2);
        c.drawText(s, x, y, p);
    }

    private static void header(Canvas c, float x, float y) {
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(4);
        stroke.setColor(HEAD);
        c.drawRoundRect(new RectF(x, y - 34, x + 24, y + 6), 6, 6, stroke);
        Paint t = paint(HEAD, 30, false, Paint.Align.LEFT);
        t.setLetterSpacing(0.06f);
        fitText(c, DeviceName.get() + " UNFOLDS", x + 40, y, c.getWidth() - x - 40 - 48, t);
    }

    static synchronized Bitmap small(Context ctx, boolean light) {
        apply(light);
        Bitmap b = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(BG);
        c.drawRoundRect(new RectF(0, 0, 512, 512), 120, 120, bg);
        header(c, 48, 104);

        int today = FoldStore.count(ctx, false, 0);
        int yest = FoldStore.count(ctx, false, 1);
        fitText(c, String.valueOf(today), 256, 285, 400, paint(WHITE, 190, true, Paint.Align.CENTER));

        Paint l1 = paint(HEAD, 38, false, Paint.Align.LEFT);
        c.drawText("Today", 44, 455, l1);
        float w = l1.measureText("Today ");
        c.drawText(String.valueOf(today), 44 + w, 455, paint(WHITE, 38, true, Paint.Align.LEFT));

        int d = today - yest;
        String arrow = d > 0 ? "\u2191" + d : d < 0 ? "\u2193" + (-d) : "\u2013 0";
        c.drawText(arrow, 468, 428, paint(d < 0 ? DOWN : UP, 36, true, Paint.Align.RIGHT));
        c.drawText("vs yesterday", 468, 460, paint(GRAY, 26, false, Paint.Align.RIGHT));
        return b;
    }

    static synchronized Bitmap wide(Context ctx, boolean light) {
        apply(light);
        Bitmap b = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(BG);
        c.drawRoundRect(new RectF(0, 0, 1024, 512), 120, 120, bg);
        header(c, 64, 116);

        int total = FoldStore.total(ctx, false);
        fitText(c, String.valueOf(total), 56, 300, 440, paint(WHITE, 200, true, Paint.Align.LEFT));
        c.drawText("all time", 64, 350, paint(HEAD, 30, false, Paint.Align.LEFT));

        Paint l1 = paint(HEAD, 34, false, Paint.Align.LEFT);
        c.drawText("This week", 60, 450, l1);
        float w = l1.measureText("This week ");
        c.drawText(String.valueOf(FoldStore.weekTotal(ctx, false)), 60 + w, 450,
                paint(WHITE, 34, true, Paint.Align.LEFT));

        int today = FoldStore.todayIndex();
        int max = 1;
        int[] v = new int[7];
        for (int i = 0; i <= today; i++) {
            v[i] = FoldStore.weekCount(ctx, false, i);
            max = Math.max(max, v[i]);
        }
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        float left = 560, step = 62, barW = 26, base = 400, top = 140;
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < 7; i++) {
            float cx = left + i * step + step / 2;
            float x = cx - barW / 2;
            if (i > today || v[i] == 0) {
                fill.setColor(DIM);
                c.drawRoundRect(new RectF(x, base - 4, x + barW, base), 2, 2, fill);
            } else {
                float h = Math.max(16, (base - top) * v[i] / max);
                fill.setColor(FOLD);
                c.drawRoundRect(new RectF(x, base - h, x + barW, base), 13, 13, fill);
            }
            if (i == today) {
                float h = v[i] == 0 ? 4 : Math.max(16, (base - top) * v[i] / max);
                c.drawText(String.valueOf(v[i]), cx, base - h - 12, paint(WHITE, 30, true, Paint.Align.CENTER));
            }
            c.drawText(days[i], cx, 452, paint(i == today ? WHITE : GRAY, 30, i == today, Paint.Align.CENTER));
        }
        return b;
    }
}
