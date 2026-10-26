package io.github.manorcompanion;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(17,29,34), PANEL = Color.rgb(27,43,49);
    static final int TEXT = Color.rgb(236,239,231), MUTED = Color.rgb(168,185,186);
    static final int ACCENT = Color.rgb(167,216,202), LINE = Color.rgb(50,70,76);
    static int dp(Context c, float n) { return Math.round(n * c.getResources().getDisplayMetrics().density); }
    static GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    static LinearLayout column(Context c) {
        LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v;
    }
    static TextView text(Context c, String value, int size, int color) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setIncludeFontPadding(false); return t;
    }
    static TextView heading(Context c, String value, int size) {
        TextView t = text(c, value, size, TEXT); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    static Button button(Context c, String label, boolean primary, View.OnClickListener listener) {
        Button b = new Button(c); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTextColor(primary ? BG : ACCENT); b.setMinHeight(dp(c,48)); b.setMinimumWidth(0);
        b.setBackground(shape(primary ? ACCENT : LINE, dp(c,12)));
        b.setPadding(dp(c,12),dp(c,4),dp(c,12),dp(c,4)); b.setOnClickListener(listener); return b;
    }
    static void gap(LinearLayout parent, int height) {
        View v = new View(parent.getContext()); parent.addView(v, new LinearLayout.LayoutParams(1, dp(parent.getContext(),height)));
    }
    static LinearLayout card(Context c) {
        LinearLayout v = column(c); int p = dp(c,18); v.setPadding(p,p,p,p);
        v.setBackground(shape(PANEL,dp(c,20))); return v;
    }
    static void weighted(LinearLayout row, View v) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.setMargins(dp(row.getContext(),3),0,dp(row.getContext(),3),0); row.addView(v,p);
    }
}
