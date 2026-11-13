package com.sktraders.manager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Very small document model + renderer used for invoice / statement / report printing. */
class PdfDoc {
    static final int TEXT = 0, KV = 1, RULE = 2, GAP = 3, HEAD = 4, ROW = 5;
    static final int INK = 0xFF182338, NAVY = 0xFF101D35, GOLD = 0xFFC9A85B, MUTED = 0xFF718096;

    static class Item {
        int kind;
        String a = "", b = "";
        String[] cells;
        float[] w;
        boolean[] right;
        boolean bold;
        float size = 10;
        int color = INK;
        int align;       // 0 left, 1 center, 2 right
        float indent;    // 0..1 fraction of width (KV)
        float h;
        int page;
        float y;
    }

    final List<Item> items = new ArrayList<>();

    PdfDoc text(String t, float size, boolean bold, int color, int align) {
        Item i = new Item();
        i.kind = TEXT;
        i.a = t;
        i.size = size;
        i.bold = bold;
        i.color = color;
        i.align = align;
        items.add(i);
        return this;
    }

    PdfDoc text(String t, float size, boolean bold) {
        return text(t, size, bold, INK, 0);
    }

    PdfDoc kv(String left, String right, boolean bold, float size, float indent) {
        Item i = new Item();
        i.kind = KV;
        i.a = left;
        i.b = right;
        i.bold = bold;
        i.size = size;
        i.indent = indent;
        items.add(i);
        return this;
    }

    PdfDoc rule(int color) {
        Item i = new Item();
        i.kind = RULE;
        i.color = color;
        items.add(i);
        return this;
    }

    PdfDoc gap(float h) {
        Item i = new Item();
        i.kind = GAP;
        i.h = h;
        items.add(i);
        return this;
    }

    PdfDoc head(String[] cells, float[] w, boolean[] right) {
        Item i = new Item();
        i.kind = HEAD;
        i.cells = cells;
        i.w = w;
        i.right = right;
        items.add(i);
        return this;
    }

    PdfDoc row(String[] cells, float[] w, boolean[] right) {
        Item i = new Item();
        i.kind = ROW;
        i.cells = cells;
        i.w = w;
        i.right = right;
        items.add(i);
        return this;
    }

    private static float height(Item it) {
        switch (it.kind) {
            case TEXT:
            case KV:
                return it.size * 1.5f;
            case RULE:
                return 6;
            case GAP:
                return it.h;
            default:
                return 17;
        }
    }

    /** Assigns every item to a page; returns the page count. */
    int paginate(int pageW, int pageH) {
        float top = 36, bottom = pageH - 36, y = top;
        int page = 0;
        for (Item it : items) {
            float h = height(it);
            if (y + h > bottom && y > top) {
                page++;
                y = top;
            }
            it.page = page;
            it.y = y;
            it.h = h;
            y += h;
        }
        return page + 1;
    }

    private static String fit(Paint p, String s, float avail) {
        if (s == null) return "";
        if (p.measureText(s) <= avail) return s;
        String t = s;
        while (t.length() > 1 && p.measureText(t + "…") > avail) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    void draw(Canvas c, int page, int pageW) {
        float left = 36, right = pageW - 36, width = right - left;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (Item it : items) {
            if (it.page != page) continue;
            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.LEFT);
            switch (it.kind) {
                case TEXT: {
                    p.setTextSize(it.size);
                    p.setTypeface(it.bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    p.setColor(it.color);
                    float base = it.y + it.size * 1.1f;
                    if (it.align == 1) {
                        p.setTextAlign(Paint.Align.CENTER);
                        c.drawText(it.a, left + width / 2, base, p);
                    } else if (it.align == 2) {
                        p.setTextAlign(Paint.Align.RIGHT);
                        c.drawText(it.a, right, base, p);
                    } else {
                        c.drawText(fit(p, it.a, width), left, base, p);
                    }
                    break;
                }
                case KV: {
                    p.setTextSize(it.size);
                    p.setTypeface(it.bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    p.setColor(it.color);
                    float base = it.y + it.size * 1.1f;
                    float x0 = left + it.indent * width;
                    c.drawText(fit(p, it.a, (right - x0) * 0.6f), x0, base, p);
                    p.setTextAlign(Paint.Align.RIGHT);
                    c.drawText(fit(p, it.b, (right - x0) * 0.6f), right, base, p);
                    break;
                }
                case RULE: {
                    p.setColor(it.color);
                    p.setStrokeWidth(1.2f);
                    c.drawLine(left, it.y + 2, right, it.y + 2, p);
                    break;
                }
                case GAP:
                    break;
                default: { // HEAD, ROW
                    boolean head = it.kind == HEAD;
                    float tot = 0;
                    for (float f : it.w) tot += f;
                    if (head) {
                        p.setColor(0xFFF3F5F8);
                        c.drawRect(left, it.y, right, it.y + 17, p);
                    }
                    p.setTextSize(head ? 8.5f : 9.5f);
                    p.setTypeface(head ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                    p.setColor(head ? 0xFF536174 : INK);
                    float x = left;
                    for (int i = 0; i < it.cells.length; i++) {
                        float cw = width * it.w[i] / tot;
                        String s = head ? it.cells[i].toUpperCase() : it.cells[i];
                        s = fit(p, s, cw - 8);
                        if (it.right != null && it.right[i]) {
                            p.setTextAlign(Paint.Align.RIGHT);
                            c.drawText(s, x + cw - 4, it.y + 12, p);
                        } else {
                            p.setTextAlign(Paint.Align.LEFT);
                            c.drawText(s, x + 4, it.y + 12, p);
                        }
                        x += cw;
                    }
                    if (!head) {
                        p.setColor(0xFFDDDDDD);
                        p.setStrokeWidth(0.7f);
                        c.drawLine(left, it.y + 16, right, it.y + 16, p);
                    }
                    break;
                }
            }
        }
    }

    /** Android print framework adapter: shows the system print preview / "Save as PDF". */
    static class Adapter extends PrintDocumentAdapter {
        private final Context ctx;
        private final PdfDoc doc;
        private final String name;
        private PrintedPdfDocument pdf;
        private int pages = 1;

        Adapter(Context ctx, PdfDoc doc, String name) {
            this.ctx = ctx;
            this.doc = doc;
            this.name = name.replaceAll("[^A-Za-z0-9._ -]", "_");
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                             CancellationSignal cancellationSignal, LayoutResultCallback callback,
                             Bundle extras) {
            pdf = new PrintedPdfDocument(ctx, newAttributes);
            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }
            pages = doc.paginate(pdf.getPageWidth(), pdf.getPageHeight());
            PrintDocumentInfo info = new PrintDocumentInfo.Builder(name + ".pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pages)
                    .build();
            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor destination,
                            CancellationSignal cancellationSignal, WriteResultCallback callback) {
            try {
                for (int i = 0; i < pages; i++) {
                    PdfDocument.Page page = pdf.startPage(i);
                    doc.draw(page.getCanvas(), i, pdf.getPageWidth());
                    pdf.finishPage(page);
                }
                pdf.writeTo(new FileOutputStream(destination.getFileDescriptor()));
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch (IOException e) {
                callback.onWriteFailed(e.toString());
            } finally {
                pdf.close();
                pdf = null;
            }
        }
    }
}
