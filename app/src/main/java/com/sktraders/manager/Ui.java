package com.sktraders.manager;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** UI toolkit that recreates the original S.K TRADERS look (navy / gold, rounded cards). */
final class Ui {
    private Ui() {}

    static final int NAVY = 0xFF101D35, GOLD = 0xFFC9A85B, BG = 0xFFF3F5F8, INK = 0xFF182338,
            MUTED = 0xFF718096, LINE = 0xFFE4E9F0, GREEN = 0xFF16845B, RED = 0xFFC64B4B,
            LABEL = 0xFF536174;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    static final int TXT = InputType.TYPE_CLASS_TEXT;
    static final int PHONE = InputType.TYPE_CLASS_PHONE;
    static final int NUM = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
    static final int NUMS = NUM | InputType.TYPE_NUMBER_FLAG_SIGNED;
    static final int MULTI = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;

    private static float density = 1f;

    static void init(Context c) {
        density = c.getResources().getDisplayMetrics().density;
    }

    static int dp(float v) {
        return Math.round(v * density);
    }

    // ------------------------------------------------------------------ basics

    static GradientDrawable rect(int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (stroke != 0) g.setStroke(dp(1), stroke);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    static TextView tv(Context c, String t, float sp, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static LinearLayout.LayoutParams lpm(int w, int h, float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    static View hline(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(1)));
        return v;
    }

    // ------------------------------------------------------------------ buttons / badges

    /** style: 0 default, 1 primary (navy), 2 gold, 3 danger */
    static TextView btn(Context c, String text, int style, boolean small, View.OnClickListener l) {
        int fill = Color.WHITE, stroke = LINE, txt = INK;
        if (style == 1) {
            fill = NAVY;
            stroke = NAVY;
            txt = Color.WHITE;
        } else if (style == 2) {
            fill = GOLD;
            stroke = GOLD;
            txt = 0xFF17243A;
        } else if (style == 3) {
            txt = RED;
            stroke = 0xFFEFCCCC;
        }
        TextView t = tv(c, text, small ? 12 : 14, txt, true);
        t.setGravity(Gravity.CENTER);
        if (small) t.setPadding(dp(9), dp(6), dp(9), dp(6));
        else t.setPadding(dp(13), dp(9), dp(13), dp(9));
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),
                rect(fill, stroke, 9), rect(Color.WHITE, 0, 9)));
        t.setClickable(true);
        if (l != null) t.setOnClickListener(l);
        return t;
    }

    /** kind: 0 neutral, 1 good (green), 2 warn (amber), 3 bad (red) */
    static TextView badge(Context c, String text, int kind) {
        int bg = 0xFFEDF2F7, fg = LABEL;
        if (kind == 1) {
            bg = 0xFFE5F5EE;
            fg = GREEN;
        } else if (kind == 2) {
            bg = 0xFFFFF3DF;
            fg = 0xFF9A6813;
        } else if (kind == 3) {
            bg = 0xFFFCEAEA;
            fg = RED;
        }
        TextView t = tv(c, text, 11, fg, true);
        t.setPadding(dp(8), dp(4), dp(8), dp(4));
        t.setBackground(rect(bg, 0, 20));
        return t;
    }

    // ------------------------------------------------------------------ containers

    static LinearLayout panel(Context c) {
        LinearLayout l = vbox(c);
        l.setBackground(rect(Color.WHITE, LINE, 14));
        l.setPadding(dp(13), dp(13), dp(13), dp(13));
        l.setElevation(dp(2));
        l.setLayoutParams(lpm(MATCH, WRAP, 0, 0, 0, 16));
        return l;
    }

    static View statCard(Context c, String label, String value, String hint) {
        LinearLayout l = vbox(c);
        l.setBackground(rect(Color.WHITE, LINE, 14));
        l.setPadding(dp(12), dp(12), dp(12), dp(12));
        l.setElevation(dp(1));
        l.addView(tv(c, label, 12, MUTED, true));
        TextView v = tv(c, value, 19, NAVY, true);
        v.setPadding(0, dp(6), 0, 0);
        l.addView(v);
        if (hint != null && !hint.isEmpty()) {
            TextView h = tv(c, hint, 11, MUTED, false);
            h.setPadding(0, dp(4), 0, 0);
            l.addView(h);
        }
        return l;
    }

    /** stats entries are {label, value, hint}. */
    static LinearLayout statGrid(Context c, List<String[]> stats, int cols) {
        LinearLayout grid = vbox(c);
        for (int i = 0; i < stats.size(); i += cols) {
            LinearLayout row = hbox(c);
            for (int k = 0; k < cols; k++) {
                LinearLayout.LayoutParams p = lpm(0, WRAP, k == 0 ? 0 : 5, 0, k == cols - 1 ? 0 : 5, 0);
                p.weight = 1;
                if (i + k < stats.size()) {
                    String[] s = stats.get(i + k);
                    row.addView(statCard(c, s[0], s[1], s.length > 2 ? s[2] : ""), p);
                } else {
                    row.addView(new View(c), p);
                }
            }
            grid.addView(row, lpm(MATCH, WRAP, 0, 0, 0, 10));
        }
        return grid;
    }

    static LinearLayout pageHead(Context c, String title, String sub, View... actions) {
        LinearLayout h = vbox(c);
        h.addView(tv(c, title, 23, NAVY, true));
        if (sub != null && !sub.isEmpty()) {
            TextView s = tv(c, sub, 14, MUTED, false);
            s.setPadding(0, dp(4), 0, 0);
            h.addView(s);
        }
        if (actions != null && actions.length > 0) {
            LinearLayout row = hbox(c);
            for (int i = 0; i < actions.length; i++) {
                row.addView(actions[i], lpm(WRAP, WRAP, 0, 0, 8, 0));
            }
            h.addView(row, lpm(WRAP, WRAP, 0, 12, 0, 0));
        }
        h.setLayoutParams(lpm(MATCH, WRAP, 0, 0, 0, 18));
        return h;
    }

    // ------------------------------------------------------------------ tables

    static TextView cell(Context c, String t, boolean right, boolean bold) {
        TextView v = tv(c, t, 13, INK, bold);
        v.setSingleLine();
        v.setPadding(dp(10), dp(11), dp(10), dp(11));
        if (right) v.setGravity(Gravity.END);
        return v;
    }

    /** Wraps any view as a padded table cell. */
    static View cellOf(Context c, View inner) {
        LinearLayout l = vbox(c);
        l.setPadding(dp(10), dp(8), dp(10), dp(8));
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.addView(inner, lp(WRAP, WRAP));
        return l;
    }

    static View actions(Context c, View... btns) {
        LinearLayout l = hbox(c);
        l.setPadding(dp(8), dp(6), dp(8), dp(6));
        l.setGravity(Gravity.CENTER_VERTICAL);
        for (View b : btns) l.addView(b, lpm(WRAP, WRAP, 0, 0, 6, 0));
        return l;
    }

    static View table(Context c, String[] heads, boolean[] right, List<View[]> rows, String empty) {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setFillViewport(true);
        hs.setHorizontalScrollBarEnabled(false);
        TableLayout t = new TableLayout(c);
        t.setStretchAllColumns(true);

        TableRow hr = new TableRow(c);
        hr.setBackgroundColor(0xFFFAFBFD);
        for (int i = 0; i < heads.length; i++) {
            TextView h = tv(c, heads[i].toUpperCase(), 11, MUTED, true);
            h.setSingleLine();
            h.setLetterSpacing(0.04f);
            h.setPadding(dp(10), dp(11), dp(10), dp(11));
            if (right != null && right[i]) h.setGravity(Gravity.END);
            hr.addView(h, new TableRow.LayoutParams(MATCH, WRAP));
        }
        t.addView(hr, new TableLayout.LayoutParams(MATCH, WRAP));
        t.addView(hline(c), new TableLayout.LayoutParams(MATCH, dp(1)));

        if (rows.isEmpty()) {
            TableRow er = new TableRow(c);
            TextView e = tv(c, empty, 13, MUTED, false);
            e.setGravity(Gravity.CENTER);
            e.setPadding(dp(10), dp(30), dp(10), dp(30));
            TableRow.LayoutParams p = new TableRow.LayoutParams();
            p.span = heads.length;
            er.addView(e, p);
            t.addView(er, new TableLayout.LayoutParams(MATCH, WRAP));
        } else {
            for (View[] r : rows) {
                TableRow tr = new TableRow(c);
                for (View v : r) tr.addView(v, new TableRow.LayoutParams(MATCH, WRAP));
                t.addView(tr, new TableLayout.LayoutParams(MATCH, WRAP));
                t.addView(hline(c), new TableLayout.LayoutParams(MATCH, dp(1)));
            }
        }
        hs.addView(t, new ViewGroup.LayoutParams(WRAP, WRAP));
        return hs;
    }

    // ------------------------------------------------------------------ form widgets

    static EditText edit(Context c, String text, String hint, int inputType) {
        EditText e = new EditText(c);
        e.setInputType(inputType);
        if ((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) {
            e.setMinLines(2);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setSingleLine();
        }
        e.setText(text == null ? "" : text);
        e.setHint(hint == null ? "" : hint);
        e.setHintTextColor(0xFFA0AEC0);
        e.setTextColor(INK);
        e.setTextSize(14);
        e.setBackground(rect(Color.WHITE, 0xFFDCE3EC, 8));
        e.setPadding(dp(11), dp(10), dp(11), dp(10));
        return e;
    }

    static void watch(EditText e, final Runnable r) {
        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                r.run();
            }
        });
    }

    static LinearLayout field(Context c, String label, View input) {
        LinearLayout f = vbox(c);
        f.addView(tv(c, label, 12, LABEL, true));
        LinearLayout.LayoutParams p = lpm(MATCH, WRAP, 0, 5, 0, 0);
        f.addView(input, p);
        f.setLayoutParams(lpm(MATCH, WRAP, 0, 0, 0, 12));
        return f;
    }

    /** A drop-down style chooser that looks like the input boxes. */
    static class Select extends TextView {
        final List<String> labels;
        final List<String> values;
        int idx = 0;
        Runnable onChange;

        Select(Context c, List<String> labels, List<String> values) {
            super(c);
            this.labels = labels;
            this.values = values;
            setTextSize(14);
            setTextColor(INK);
            setSingleLine();
            setEllipsize(android.text.TextUtils.TruncateAt.END);
            setBackground(rect(Color.WHITE, 0xFFDCE3EC, 8));
            setPadding(dp(11), dp(11), dp(11), dp(11));
            setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open();
                }
            });
            refresh();
        }

        static Select of(Context c, String... opts) {
            List<String> l = new ArrayList<>(Arrays.asList(opts));
            return new Select(c, l, l);
        }

        void refresh() {
            setText(labels.isEmpty() ? "" : labels.get(idx));
        }

        String value() {
            return values.isEmpty() ? "" : values.get(idx);
        }

        void setValue(String v) {
            int i = values.indexOf(v);
            if (i >= 0) {
                idx = i;
                refresh();
            }
        }

        void open() {
            new AlertDialog.Builder(getContext())
                    .setItems(labels.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int which) {
                            idx = which;
                            refresh();
                            if (onChange != null) onChange.run();
                        }
                    }).show();
        }
    }

    /** Tap-to-pick date field. Value is kept as yyyy-MM-dd in {@link #iso}. */
    static class DateField extends TextView {
        String iso;

        DateField(Context c, String iso) {
            super(c);
            setTextSize(14);
            setTextColor(INK);
            setSingleLine();
            setBackground(rect(Color.WHITE, 0xFFDCE3EC, 8));
            setPadding(dp(11), dp(11), dp(11), dp(11));
            setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_my_calendar, 0);
            set(iso);
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pick();
                }
            });
        }

        void set(String s) {
            iso = s;
            setText(Util.fmtDate(s));
        }

        void pick() {
            int y = 2000, m = 1, d = 1;
            try {
                y = Integer.parseInt(iso.substring(0, 4));
                m = Integer.parseInt(iso.substring(5, 7));
                d = Integer.parseInt(iso.substring(8, 10));
            } catch (Exception ignored) {
            }
            new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(android.widget.DatePicker view, int yy, int mm, int dd) {
                    set(String.format(Locale.US, "%04d-%02d-%02d", yy, mm + 1, dd));
                }
            }, y, m - 1, d).show();
        }
    }

    static View totalRow(Context c, String label, String value, boolean big) {
        LinearLayout r = hbox(c);
        r.setPadding(0, dp(big ? 10 : 5), 0, dp(5));
        TextView l = tv(c, label, big ? 17 : 14, big ? NAVY : INK, big);
        TextView v = tv(c, value, big ? 17 : 14, big ? NAVY : INK, true);
        v.setGravity(Gravity.END);
        r.addView(l, lp(0, WRAP));
        ((LinearLayout.LayoutParams) l.getLayoutParams()).weight = 1;
        r.addView(v, lp(WRAP, WRAP));
        if (big) {
            LinearLayout w = vbox(c);
            w.addView(hline(c));
            w.addView(r);
            return w;
        }
        return r;
    }

    static LinearLayout bar(Context c, View... views) {
        LinearLayout b = hbox(c);
        b.setGravity(Gravity.END);
        for (View v : views) b.addView(v, lpm(WRAP, WRAP, 8, 0, 0, 0));
        b.setLayoutParams(lpm(MATCH, WRAP, 0, 15, 0, 0));
        return b;
    }

    // ------------------------------------------------------------------ dialog

    private static class MaxScroll extends ScrollView {
        private final int max;

        MaxScroll(Context c, int max) {
            super(c);
            this.max = max;
        }

        @Override
        protected void onMeasure(int w, int h) {
            int avail = View.MeasureSpec.getSize(h);
            int limit = avail > 0 ? Math.min(max, avail) : max;
            super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(limit, View.MeasureSpec.AT_MOST));
        }
    }

    /** White rounded dialog with a title and ✕ close button, like the original modal. */
    static class Modal {
        final Dialog dialog;
        final LinearLayout body;

        Modal(Context c, String title) {
            dialog = new Dialog(c);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            LinearLayout root = vbox(c);
            root.setBackground(rect(Color.WHITE, 0, 15));
            root.setPadding(dp(18), dp(16), dp(18), dp(16));

            LinearLayout head = hbox(c);
            head.setGravity(Gravity.CENTER_VERTICAL);
            TextView t = tv(c, title, 17, NAVY, true);
            head.addView(t, lp(0, WRAP));
            ((LinearLayout.LayoutParams) t.getLayoutParams()).weight = 1;
            TextView x = tv(c, "✕", 14, INK, true);
            x.setBackground(rect(0xFFEEF1F5, 0, 8));
            x.setPadding(dp(10), dp(7), dp(10), dp(7));
            x.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            head.addView(x, lp(WRAP, WRAP));
            root.addView(head, lpm(MATCH, WRAP, 0, 0, 0, 14));

            int max = Math.round(c.getResources().getDisplayMetrics().heightPixels * 0.72f);
            MaxScroll sv = new MaxScroll(c, max);
            body = vbox(c);
            sv.addView(body, new ViewGroup.LayoutParams(MATCH, WRAP));
            root.addView(sv, lp(MATCH, WRAP));

            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new InsetDrawable(new ColorDrawable(Color.TRANSPARENT), dp(12)));
                w.setLayout(MATCH, WRAP);
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }

        void show() {
            dialog.show();
        }

        void dismiss() {
            dialog.dismiss();
        }
    }
}
