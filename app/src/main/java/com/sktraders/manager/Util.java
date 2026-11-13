package com.sktraders.manager;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class Util {
    private Util() {}

    static String uid() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Long.toString((long) (Math.random() * 2176782336L), 36);
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static String monthNow() {
        return today().substring(0, 7);
    }

    static String nowIso() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }

    /** yyyy-MM-dd -> dd-MM-yyyy */
    static String fmtDate(String s) {
        if (s == null || s.length() < 10) return "—";
        return s.substring(8, 10) + "-" + s.substring(5, 7) + "-" + s.substring(0, 4);
    }

    static String money(double n) {
        NumberFormat f = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return "₹" + f.format(n);
    }

    /** Number without useless trailing zeros (50, 2.5). */
    static String num(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return "0";
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }

    static double parse(String s) {
        if (s == null) return 0;
        s = s.trim();
        if (s.isEmpty()) return 0;
        try {
            double v = Double.parseDouble(s);
            return (Double.isNaN(v) || Double.isInfinite(v)) ? 0 : v;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String pad(int n, int width) {
        StringBuilder s = new StringBuilder(String.valueOf(n));
        while (s.length() < width) s.insert(0, '0');
        return s.toString();
    }

    static String csv(String v) {
        return "\"" + (v == null ? "" : v.replace("\"", "\"\"")) + "\"";
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    /** Saves bytes to the phone's Downloads folder. Returns a short description of the location. */
    static String saveDownload(Context c, String name, String mime, byte[] data) throws IOException {
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver cr = c.getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
            v.put(MediaStore.Downloads.MIME_TYPE, mime);
            v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IOException("Could not create file");
            OutputStream os = cr.openOutputStream(uri);
            if (os == null) throw new IOException("Could not open file");
            try {
                os.write(data);
            } finally {
                os.close();
            }
            return "Downloads folder";
        } else {
            File dir = c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) throw new IOException("Storage not available");
            File f = new File(dir, safe);
            FileOutputStream os = new FileOutputStream(f);
            try {
                os.write(data);
            } finally {
                os.close();
            }
            return f.getAbsolutePath();
        }
    }
}
