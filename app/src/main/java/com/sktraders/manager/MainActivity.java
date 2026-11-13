package com.sktraders.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * S.K TRADERS – Wholesale Business Manager (native Java UI, no HTML / WebView).
 */
public class MainActivity extends Activity {

    private static final int REQ_RESTORE = 2001;
    private static final String[] MODES = {"Cash", "UPI", "Bank Transfer", "Credit"};
    private static final String[] EXPENSE_CATS = {"Rent", "Electricity", "Transport", "Salary",
            "Packaging", "Telephone", "Repairs", "Other"};
    private static final String[][] NAV = {
            {"dashboard", "▦", "Dashboard"}, {"billing", "▤", "Billing"},
            {"products", "▣", "Products"}, {"purchases", "⇧", "Purchases"},
            {"customers", "♙", "Customers"}, {"expenses", "₹", "Expenses"},
            {"reports", "▥", "Reports"}, {"settings", "⚙", "Settings"}};

    private Store st;
    private LinearLayout main, navBar;
    private ScrollView scroll;
    private TextView brandName, brandSub;
    private String page = "dashboard";
    private String reportFrom = "", reportTo = "";

    // ============================================================ lifecycle / shell

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        st = new Store(this);
        st.load();
        getWindow().setStatusBarColor(Ui.NAVY);
        getWindow().setNavigationBarColor(Color.WHITE);

        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.BG);
        root.addView(buildHeader(), Ui.lp(Ui.MATCH, Ui.WRAP));

        scroll = new ScrollView(this);
        main = Ui.vbox(this);
        main.setPadding(dp(13), dp(16), dp(13), dp(20));
        scroll.addView(main, new ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));
        root.addView(scroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        navBar = Ui.hbox(this);
        navBar.setBackgroundColor(Color.WHITE);
        navBar.setElevation(dp(8));
        root.addView(navBar, Ui.lp(Ui.MATCH, Ui.WRAP));

        setContentView(root);
        reportFrom = Util.monthNow() + "-01";
        reportTo = Util.today();
        render();
    }

    @Override
    public void onBackPressed() {
        if (!"dashboard".equals(page)) navigate("dashboard");
        else super.onBackPressed();
    }

    private View buildHeader() {
        LinearLayout h = Ui.hbox(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(15), dp(13), dp(15), dp(13));
        h.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Ui.NAVY, 0xFF213657}));
        h.setElevation(dp(4));

        TextView mark = Ui.tv(this, "SK", 17, Ui.GOLD, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(Ui.rect(0, 0x88C9A85B, 12));
        h.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout names = Ui.vbox(this);
        brandName = Ui.tv(this, st.name, 17, Color.WHITE, true);
        brandName.setLetterSpacing(0.07f);
        brandSub = Ui.tv(this, st.address, 12, 0xFFBDC8D9, false);
        names.addView(brandName);
        names.addView(brandSub);
        LinearLayout.LayoutParams np = Ui.lpm(0, Ui.WRAP, 12, 0, 8, 0);
        np.weight = 1;
        h.addView(names, np);

        h.addView(Ui.btn(this, "⚙ Settings", 0, true, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigate("settings");
            }
        }), Ui.lp(Ui.WRAP, Ui.WRAP));
        return h;
    }

    private void buildNav() {
        navBar.removeAllViews();
        for (final String[] n : NAV) {
            boolean active = n[0].equals(page);
            int col = active ? Ui.NAVY : 0xFF68778B;
            LinearLayout item = Ui.vbox(this);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(0, dp(7), 0, dp(7));
            TextView ic = Ui.tv(this, n[1], 17, col, true);
            ic.setGravity(Gravity.CENTER);
            TextView lb = Ui.tv(this, n[2], 9, col, active);
            lb.setSingleLine();
            lb.setIncludeFontPadding(false);
            lb.setGravity(Gravity.CENTER);
            item.addView(ic, Ui.lp(Ui.WRAP, Ui.WRAP));
            item.addView(lb, Ui.lp(Ui.WRAP, Ui.WRAP));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigate(n[0]);
                }
            });
            navBar.addView(item, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        }
    }

    private void navigate(String p) {
        page = p;
        render();
    }

    private void render() {
        brandName.setText(st.name);
        brandSub.setText(st.address);
        buildNav();
        main.removeAllViews();
        switch (page) {
            case "billing": billing(); break;
            case "products": products(); break;
            case "purchases": purchases(); break;
            case "customers": customers(); break;
            case "expenses": expenses(); break;
            case "reports": reports(); break;
            case "settings": settings(); break;
            default: dash();
        }
        scroll.scrollTo(0, 0);
    }

    // ============================================================ small helpers

    private int dp(float v) {
        return Ui.dp(v);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void add(View v) {
        main.addView(v);
    }

    private TextView nb(String t, int style, View.OnClickListener l) {
        return Ui.btn(this, t, style, false, l);
    }

    private TextView sb(String t, int style, View.OnClickListener l) {
        return Ui.btn(this, t, style, true, l);
    }

    private TextView h3(String t) {
        TextView v = Ui.tv(this, t, 15, Ui.NAVY, true);
        v.setPadding(0, dp(12), 0, dp(10));
        return v;
    }

    private EditText ed(String v, int type) {
        return Ui.edit(this, v, "", type);
    }

    private String txt(EditText e) {
        return e.getText().toString();
    }

    private void confirm(String msg, final Runnable yes) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        yes.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(Runnable r) {
        confirm("Delete this record? This action cannot be undone.", r);
    }

    private boolean isLow(Product p) {
        return p.stock <= st.lowStock;
    }

    private View custCell(String name, String phone) {
        LinearLayout c = Ui.vbox(this);
        c.setPadding(dp(10), dp(8), dp(10), dp(8));
        c.addView(Ui.tv(this, name, 13, Ui.INK, false));
        if (phone != null && !phone.isEmpty()) c.addView(Ui.tv(this, phone, 11, Ui.MUTED, false));
        return c;
    }

    private View labelValue(String label, String value) {
        LinearLayout r = Ui.hbox(this);
        r.setPadding(0, dp(3), 0, dp(3));
        r.addView(Ui.tv(this, label, 14, Ui.INK, false));
        TextView v = Ui.tv(this, value, 14, Ui.INK, true);
        v.setPadding(dp(6), 0, 0, 0);
        r.addView(v);
        return r;
    }

    private final Comparator<String> dateDesc = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            return b.compareTo(a);
        }
    };

    // ============================================================ DASHBOARD

    private static class Tx {
        String date = "", created = "", type = "", ref = "", party = "";
        double amount;
    }

    private void dash() {
        String today = Util.today(), mo = Util.monthNow();
        double todaySales = 0, monthSales = 0, todayPur = 0, monthPur = 0;
        int nTodayS = 0, nMonthS = 0, nTodayP = 0, nMonthP = 0;
        for (Invoice x : st.invoices) {
            if (x.date.equals(today)) {
                todaySales += x.total;
                nTodayS++;
            }
            if (x.date.startsWith(mo)) {
                monthSales += x.total;
                nMonthS++;
            }
        }
        for (Purchase x : st.purchases) {
            if (x.date.equals(today)) {
                todayPur += x.total;
                nTodayP++;
            }
            if (x.date.startsWith(mo)) {
                monthPur += x.total;
                nMonthP++;
            }
        }
        int low = 0;
        double stockQty = 0;
        for (Product p : st.products) {
            stockQty += p.stock;
            if (isLow(p)) low++;
        }

        add(Ui.pageHead(this, "Dashboard", "Business overview · " + Util.fmtDate(today),
                nb("＋ New Bill", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigate("billing");
                    }
                }),
                nb("＋ Purchase", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigate("purchases");
                    }
                })));

        List<String[]> stats = new ArrayList<>();
        stats.add(new String[]{"Today's Sales", Util.money(todaySales), nTodayS + " invoices"});
        stats.add(new String[]{"Monthly Sales", Util.money(monthSales), nMonthS + " invoices this month"});
        stats.add(new String[]{"Today's Purchases", Util.money(todayPur), nTodayP + " entries"});
        stats.add(new String[]{"Monthly Purchases", Util.money(monthPur), nMonthP + " entries"});
        stats.add(new String[]{"Total Customers", String.valueOf(st.customers.size()), "Saved customer accounts"});
        stats.add(new String[]{"Total Products", String.valueOf(st.products.size()), "Product catalogue"});
        stats.add(new String[]{"Total Stock Quantity", Util.num(stockQty), "Across all products"});
        stats.add(new String[]{"Customer Outstanding", Util.money(st.outstanding()), "Unpaid invoice balances"});
        stats.add(new String[]{"Total Invoices", String.valueOf(st.invoices.size()), "All billing records"});
        stats.add(new String[]{"Low Stock Items", String.valueOf(low), "At or below alert level"});
        add(Ui.statGrid(this, stats, 2));

        // recent transactions
        List<Tx> txs = new ArrayList<>();
        for (Invoice x : st.invoices) {
            Tx t = new Tx();
            t.date = x.date;
            t.created = x.created;
            t.type = "Sale";
            t.ref = x.number;
            t.party = x.customer.isEmpty() ? "—" : x.customer;
            t.amount = x.total;
            txs.add(t);
        }
        for (Purchase x : st.purchases) {
            Tx t = new Tx();
            t.date = x.date;
            t.created = x.created;
            t.type = "Purchase";
            t.ref = x.number;
            t.party = x.supplier.isEmpty() ? "—" : x.supplier;
            t.amount = x.total;
            txs.add(t);
        }
        Collections.sort(txs, new Comparator<Tx>() {
            @Override
            public int compare(Tx a, Tx b) {
                String ka = a.created.isEmpty() ? a.date : a.created;
                String kb = b.created.isEmpty() ? b.date : b.created;
                return kb.compareTo(ka);
            }
        });
        List<View[]> rows = new ArrayList<>();
        for (int i = 0; i < txs.size() && i < 8; i++) {
            Tx t = txs.get(i);
            rows.add(new View[]{
                    Ui.cell(this, Util.fmtDate(t.date), false, false),
                    Ui.cellOf(this, Ui.badge(this, t.type, t.type.equals("Sale") ? 1 : 0)),
                    Ui.cell(this, t.ref, false, false),
                    Ui.cell(this, t.party, false, false),
                    Ui.cell(this, Util.money(t.amount), true, false)});
        }
        LinearLayout p1 = Ui.panel(this);
        p1.setPadding(dp(13), dp(13), dp(13), dp(4));
        p1.addView(h3("Recent Transactions"));
        p1.addView(Ui.table(this, new String[]{"Date", "Type", "Reference", "Party", "Amount"},
                new boolean[]{false, false, false, false, true}, rows, "No records yet"));
        add(p1);

        List<View[]> lrows = new ArrayList<>();
        for (Product p : st.products) {
            if (!isLow(p)) continue;
            lrows.add(new View[]{
                    Ui.cell(this, p.name, false, false),
                    Ui.cell(this, Util.num(p.stock), false, false),
                    Ui.cellOf(this, Ui.badge(this, "Restock", 2))});
        }
        LinearLayout p2 = Ui.panel(this);
        p2.setPadding(dp(13), dp(13), dp(13), dp(4));
        p2.addView(h3("Low Stock Alerts"));
        p2.addView(Ui.table(this, new String[]{"Product", "Stock", "Alert"}, null, lrows,
                "All products are above the low-stock threshold."));
        add(p2);
    }

    // ============================================================ BILLING

    private void billing() {
        add(Ui.pageHead(this, "Billing", "Create and manage GST, Non-GST and NIL invoices",
                nb("＋ Create Invoice", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new InvoiceForm().show();
                    }
                })));
        LinearLayout panel = Ui.panel(this);
        final EditText q = Ui.edit(this, "", "Invoice, customer or phone", Ui.TXT);
        final Ui.Select type = new Ui.Select(this,
                java.util.Arrays.asList("All types", "GST", "Non-GST", "NIL"),
                java.util.Arrays.asList("", "GST", "Non-GST", "NIL"));
        final LinearLayout host = Ui.vbox(this);
        LinearLayout row = Ui.hbox(this);
        LinearLayout.LayoutParams p1 = Ui.lpm(0, Ui.WRAP, 0, 0, 5, 0);
        p1.weight = 1.4f;
        LinearLayout.LayoutParams p2 = Ui.lpm(0, Ui.WRAP, 5, 0, 0, 0);
        p2.weight = 1;
        row.addView(Ui.field(this, "Search invoices", q), p1);
        row.addView(Ui.field(this, "Invoice type", type), p2);
        panel.addView(row);
        panel.addView(host);
        add(panel);
        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                fillInvoices(host, txt(q).toLowerCase(Locale.ROOT).trim(), type.value());
            }
        };
        Ui.watch(q, refresh);
        type.onChange = refresh;
        refresh.run();
    }

    private void fillInvoices(LinearLayout host, String q, String type) {
        host.removeAllViews();
        List<Invoice> list = new ArrayList<>();
        for (Invoice x : st.invoices) {
            if (!type.isEmpty() && !x.type.equals(type)) continue;
            String hay = (x.number + " " + x.customer + " " + x.phone).toLowerCase(Locale.ROOT);
            if (!hay.contains(q)) continue;
            list.add(x);
        }
        Collections.sort(list, new Comparator<Invoice>() {
            @Override
            public int compare(Invoice a, Invoice b) {
                return b.date.compareTo(a.date);
            }
        });
        List<View[]> rows = new ArrayList<>();
        for (final Invoice x : list) {
            View acts = Ui.actions(this,
                    sb("View / Print", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewInvoice(x);
                        }
                    }),
                    sb("Delete", 3, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            confirmDelete(new Runnable() {
                                @Override
                                public void run() {
                                    st.deleteInvoice(x.id);
                                    render();
                                    toast("Record deleted");
                                }
                            });
                        }
                    }));
            rows.add(new View[]{
                    Ui.cell(this, x.number, false, true),
                    Ui.cell(this, Util.fmtDate(x.date), false, false),
                    custCell(x.customer.isEmpty() ? "Walk-in" : x.customer, x.phone),
                    Ui.cellOf(this, Ui.badge(this, x.type, 0)),
                    Ui.cell(this, Util.money(x.total), true, false),
                    Ui.cell(this, Util.money(x.paid), true, false),
                    Ui.cell(this, Util.money(x.balance), true, false),
                    acts});
        }
        host.addView(Ui.table(this,
                new String[]{"Invoice", "Date", "Customer", "Type", "Total", "Paid", "Balance", "Actions"},
                new boolean[]{false, false, false, false, true, true, true, false}, rows, "No records yet"));
    }

    // ---------------------------------------------------------------- create invoice

    private class InvLine {
        Ui.Select prod;
        EditText qty, rate, tax;
        LinearLayout view;
    }

    private View col(String label, View v, float weight) {
        LinearLayout c = Ui.vbox(this);
        c.addView(Ui.tv(this, label, 11, Ui.MUTED, true));
        c.addView(v, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 3, 0, 0));
        LinearLayout.LayoutParams p = Ui.lpm(0, Ui.WRAP, 0, 0, 6, 0);
        p.weight = weight;
        c.setLayoutParams(p);
        return c;
    }

    private class InvoiceForm {
        Ui.Modal m;
        Ui.Select type, customer, mode;
        Ui.DateField dateF;
        EditText number, name, phone, address, discount, paid;
        LinearLayout itemsBox, totals;
        final List<InvLine> rows = new ArrayList<>();

        void show() {
            if (st.products.isEmpty()) {
                toast("Add products before billing");
                return;
            }
            m = new Ui.Modal(MainActivity.this, "Create Invoice");
            final Runnable recalc = new Runnable() {
                @Override
                public void run() {
                    recalc();
                }
            };
            type = Ui.Select.of(MainActivity.this, "GST", "Non-GST", "NIL");
            type.onChange = recalc;
            number = ed(st.nextInvoiceNumber(), Ui.TXT);
            dateF = new Ui.DateField(MainActivity.this, Util.today());
            List<String> cl = new ArrayList<>(), cv = new ArrayList<>();
            cl.add("Walk-in customer");
            cv.add("");
            for (Customer c : st.customers) {
                cl.add(c.name + " · " + c.phone);
                cv.add(c.id);
            }
            customer = new Ui.Select(MainActivity.this, cl, cv);
            name = ed("", Ui.TXT);
            phone = ed("", Ui.PHONE);
            address = ed("", Ui.TXT);
            customer.onChange = new Runnable() {
                @Override
                public void run() {
                    Customer c = st.customer(customer.value());
                    if (c != null) {
                        name.setText(c.name);
                        phone.setText(c.phone);
                        address.setText(c.address);
                    }
                }
            };
            m.body.addView(Ui.field(MainActivity.this, "Billing type", type));
            m.body.addView(Ui.field(MainActivity.this, "Invoice number", number));
            m.body.addView(Ui.field(MainActivity.this, "Invoice date", dateF));
            m.body.addView(Ui.field(MainActivity.this, "Customer (optional)", customer));
            m.body.addView(Ui.field(MainActivity.this, "Customer name", name));
            m.body.addView(Ui.field(MainActivity.this, "Phone", phone));
            m.body.addView(Ui.field(MainActivity.this, "Address", address));

            TextView items = h3("Items");
            items.setPadding(0, dp(6), 0, dp(8));
            m.body.addView(items);
            itemsBox = Ui.vbox(MainActivity.this);
            m.body.addView(itemsBox);
            m.body.addView(sb("＋ Add Item", 0, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addLine();
                }
            }));

            discount = ed("0", Ui.NUM);
            paid = ed("0", Ui.NUM);
            mode = Ui.Select.of(MainActivity.this, MODES);
            Ui.watch(discount, recalc);
            Ui.watch(paid, recalc);
            LinearLayout.LayoutParams top = Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 14, 0, 0);
            LinearLayout extra = Ui.vbox(MainActivity.this);
            extra.addView(Ui.field(MainActivity.this, "Discount (₹)", discount));
            extra.addView(Ui.field(MainActivity.this, "Payment mode", mode));
            extra.addView(Ui.field(MainActivity.this, "Amount paid (₹)", paid));
            m.body.addView(extra, top);

            totals = Ui.vbox(MainActivity.this);
            m.body.addView(totals);
            m.body.addView(Ui.bar(MainActivity.this,
                    nb("Cancel", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            m.dismiss();
                        }
                    }),
                    nb("Save Invoice", 1, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            save();
                        }
                    })));
            addLine();
            m.show();
        }

        void addLine() {
            final InvLine r = new InvLine();
            List<String> labels = new ArrayList<>(), vals = new ArrayList<>();
            labels.add("Select product");
            vals.add("");
            for (Product p : st.products) {
                labels.add(p.name + " (Stock " + Util.num(p.stock) + ")");
                vals.add(p.id);
            }
            r.prod = new Ui.Select(MainActivity.this, labels, vals);
            r.qty = Ui.edit(MainActivity.this, "1", "Qty", Ui.NUM);
            r.rate = Ui.edit(MainActivity.this, "", "Rate", Ui.NUM);
            r.tax = Ui.edit(MainActivity.this, "0", "GST %", Ui.NUM);
            Runnable recalc = new Runnable() {
                @Override
                public void run() {
                    recalc();
                }
            };
            Ui.watch(r.qty, recalc);
            Ui.watch(r.rate, recalc);
            Ui.watch(r.tax, recalc);
            r.prod.onChange = new Runnable() {
                @Override
                public void run() {
                    Product p = st.product(r.prod.value());
                    if (p != null) {
                        r.rate.setText(Util.num(p.price));
                        r.tax.setText("0");
                    }
                    recalc();
                }
            };
            LinearLayout v = Ui.vbox(MainActivity.this);
            v.setPadding(dp(10), dp(10), dp(10), dp(10));
            v.setBackground(Ui.rect(0xFFF8FAFC, Ui.LINE, 10));
            v.addView(r.prod, Ui.lp(Ui.MATCH, Ui.WRAP));
            LinearLayout row = Ui.hbox(MainActivity.this);
            row.setGravity(Gravity.BOTTOM);
            row.addView(col("Qty", r.qty, 1f));
            row.addView(col("Rate", r.rate, 1.3f));
            row.addView(col("GST %", r.tax, 1f));
            row.addView(sb("✕", 3, new View.OnClickListener() {
                @Override
                public void onClick(View x) {
                    rows.remove(r);
                    itemsBox.removeView(r.view);
                    recalc();
                }
            }), Ui.lp(Ui.WRAP, Ui.WRAP));
            v.addView(row, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 8, 0, 0));
            r.view = v;
            rows.add(r);
            itemsBox.addView(v, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
            recalc();
        }

        List<Line> lines() {
            List<Line> out = new ArrayList<>();
            for (InvLine r : rows) {
                Product p = st.product(r.prod.value());
                double q = Util.parse(txt(r.qty));
                if (p == null || q <= 0) continue;
                Line l = new Line();
                l.productId = p.id;
                l.name = p.name;
                l.qty = q;
                l.rate = Util.parse(txt(r.rate));
                l.tax = Util.parse(txt(r.tax));
                l.cost = p.cost;
                out.add(l);
            }
            return out;
        }

        void recalc() {
            if (totals == null) return;
            String t = type.value();
            double base = 0, tax = 0;
            for (Line l : lines()) {
                base += l.qty * l.rate;
                if (t.equals("GST")) tax += l.qty * l.rate * l.tax / 100;
            }
            double disc = Util.parse(txt(discount));
            double total = Math.max(0, base + tax - disc);
            double pd = Util.parse(txt(paid));
            totals.removeAllViews();
            totals.addView(Ui.totalRow(MainActivity.this, "Subtotal", Util.money(base), false));
            totals.addView(Ui.totalRow(MainActivity.this, "GST", Util.money(tax), false));
            totals.addView(Ui.totalRow(MainActivity.this, "Discount", Util.money(disc), false));
            totals.addView(Ui.totalRow(MainActivity.this, "Grand Total", Util.money(total), true));
            totals.addView(Ui.totalRow(MainActivity.this, "Balance Due", Util.money(Math.max(0, total - pd)), false));
        }

        void save() {
            String t = type.value();
            List<Line> items = lines();
            if (items.isEmpty()) {
                toast("Add at least one valid item");
                return;
            }
            Map<String, Double> need = new HashMap<>();
            for (Line l : items) {
                Double cur = need.get(l.productId);
                need.put(l.productId, (cur == null ? 0 : cur) + l.qty);
            }
            for (Map.Entry<String, Double> e : need.entrySet()) {
                Product p = st.product(e.getKey());
                if (p == null || p.stock < e.getValue()) {
                    toast("Insufficient stock for one or more items");
                    return;
                }
            }
            double subtotal = 0, tax = 0;
            for (Line l : items) {
                subtotal += l.qty * l.rate;
                if (t.equals("GST")) tax += l.qty * l.rate * l.tax / 100;
            }
            double disc = Util.parse(txt(discount));
            double total = Math.max(0, subtotal + tax - disc);
            double pd = Util.parse(txt(paid));
            if (pd < 0 || pd > total) {
                toast("Paid amount must be between ₹0 and total");
                return;
            }
            String num = txt(number).trim();
            if (num.isEmpty()) {
                toast("Enter invoice number");
                return;
            }
            if (st.invoiceNumberExists(num)) {
                toast("Invoice number already exists");
                return;
            }
            String nm = txt(name).trim();
            Customer c = st.customer(customer.value());
            if (c == null && !nm.isEmpty()) {
                c = new Customer();
                c.id = Util.uid();
                c.name = nm;
                c.phone = txt(phone);
                c.address = txt(address);
                st.customers.add(c);
            }
            Invoice inv = new Invoice();
            inv.id = Util.uid();
            inv.number = num;
            inv.date = dateF.iso;
            inv.type = t;
            inv.customerId = c != null ? c.id : "";
            inv.customer = !nm.isEmpty() ? nm : (c != null ? c.name : "Walk-in");
            inv.phone = !txt(phone).isEmpty() ? txt(phone) : (c != null ? c.phone : "");
            inv.address = !txt(address).isEmpty() ? txt(address) : (c != null ? c.address : "");
            inv.items = items;
            inv.subtotal = subtotal;
            inv.tax = tax;
            inv.discount = disc;
            inv.total = total;
            inv.paid = pd;
            inv.balance = total - pd;
            inv.mode = mode.value();
            inv.created = Util.nowIso();
            for (Line l : items) {
                Product p = st.product(l.productId);
                if (p != null) p.stock -= l.qty;
            }
            st.invoices.add(inv);
            try {
                int n = Integer.parseInt(num.substring(num.lastIndexOf('-') + 1));
                st.nextInvoice = Math.max(st.nextInvoice, n + 1);
            } catch (NumberFormatException ignored) {
            }
            st.save();
            m.dismiss();
            render();
            toast("Invoice saved successfully");
        }
    }

    // ---------------------------------------------------------------- view invoice

    private void viewInvoice(final Invoice x) {
        final Ui.Modal m = new Ui.Modal(this, "Invoice · " + x.number);
        LinearLayout hd = Ui.vbox(this);
        hd.setPadding(0, 0, 0, dp(10));
        hd.addView(Ui.tv(this, st.name, 20, Ui.NAVY, true));
        hd.addView(Ui.tv(this, st.address, 13, Ui.INK, false));
        hd.addView(Ui.tv(this, "Phone: " + st.phone + " · WhatsApp: " + st.whatsapp, 13, Ui.INK, false));
        m.body.addView(hd);
        View gold = new View(this);
        gold.setBackgroundColor(Ui.GOLD);
        m.body.addView(gold, Ui.lp(Ui.MATCH, dp(2)));

        LinearLayout info = Ui.hbox(this);
        LinearLayout left = Ui.vbox(this);
        left.addView(Ui.tv(this, x.type + " TAX INVOICE", 14, Ui.INK, true));
        left.addView(Ui.tv(this, "Invoice: " + x.number, 13, Ui.INK, false));
        left.addView(Ui.tv(this, "Date: " + Util.fmtDate(x.date), 13, Ui.INK, false));
        LinearLayout right = Ui.vbox(this);
        right.setGravity(Gravity.END);
        right.addView(Ui.tv(this, "Bill To", 14, Ui.INK, true));
        right.addView(Ui.tv(this, x.customer, 13, Ui.INK, false));
        if (!x.phone.isEmpty()) right.addView(Ui.tv(this, x.phone, 13, Ui.INK, false));
        if (!x.address.isEmpty()) right.addView(Ui.tv(this, x.address, 13, Ui.INK, false));
        LinearLayout.LayoutParams lpl = Ui.lp(0, Ui.WRAP);
        lpl.weight = 1;
        LinearLayout.LayoutParams lpr = Ui.lp(0, Ui.WRAP);
        lpr.weight = 1;
        info.addView(left, lpl);
        info.addView(right, lpr);
        m.body.addView(info, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 14, 0, 12));

        List<View[]> rows = new ArrayList<>();
        for (Line l : x.items) {
            rows.add(new View[]{
                    Ui.cell(this, l.name, false, false),
                    Ui.cell(this, Util.num(l.qty), true, false),
                    Ui.cell(this, Util.money(l.rate), true, false),
                    Ui.cell(this, Util.num(l.tax) + "%", true, false),
                    Ui.cell(this, Util.money(l.qty * l.rate), true, false)});
        }
        m.body.addView(Ui.table(this, new String[]{"Product", "Qty", "Rate", "GST", "Amount"},
                new boolean[]{false, true, true, true, true}, rows, "No items"));

        LinearLayout tot = Ui.vbox(this);
        tot.addView(Ui.totalRow(this, "Subtotal", Util.money(x.subtotal), false));
        tot.addView(Ui.totalRow(this, "GST", Util.money(x.tax), false));
        tot.addView(Ui.totalRow(this, "Discount", Util.money(x.discount), false));
        tot.addView(Ui.totalRow(this, "Grand Total", Util.money(x.total), true));
        tot.addView(Ui.totalRow(this, "Amount Paid", Util.money(x.paid), false));
        tot.addView(Ui.totalRow(this, "Balance Due", Util.money(x.balance), false));
        m.body.addView(tot, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        m.body.addView(Ui.tv(this, "Payment mode: " + x.mode, 14, Ui.INK, false), Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 10, 0, 0));
        TextView thanks = Ui.tv(this, "Thank you for your business.", 13, Ui.MUTED, false);
        thanks.setGravity(Gravity.CENTER);
        m.body.addView(thanks, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 22, 0, 0));

        m.body.addView(Ui.bar(this,
                sb("Close", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                    }
                }),
                sb("WhatsApp Share", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shareInvoice(x);
                    }
                }),
                sb("Print / Save PDF", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        printDoc(invoiceDoc(x), "Invoice " + x.number);
                    }
                })));
        m.show();
    }

    private void shareInvoice(Invoice x) {
        String txt = st.name + "\nInvoice: " + x.number + "\nDate: " + Util.fmtDate(x.date)
                + "\nCustomer: " + x.customer + "\nTotal: " + Util.money(x.total)
                + "\nPaid: " + Util.money(x.paid) + "\nBalance: " + Util.money(x.balance)
                + "\nThank you.";
        String digits = st.whatsapp.replaceAll("\\D", "");
        if (digits.length() == 10) digits = "91" + digits; // India country code
        try {
            String url = "https://wa.me/" + digits + "?text=" + Uri.encode(txt);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            toast("No app found to open WhatsApp link");
        }
    }

    // ============================================================ PRODUCTS

    private void products() {
        add(Ui.pageHead(this, "Products & Stock", "Manage catalogue, pricing and inventory",
                nb("＋ Add Product", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        productForm(null);
                    }
                })));
        LinearLayout panel = Ui.panel(this);
        final EditText q = Ui.edit(this, "", "Product name or category", Ui.TXT);
        final Ui.Select sf = new Ui.Select(this,
                java.util.Arrays.asList("All stock", "Low stock", "In stock"),
                java.util.Arrays.asList("", "low", "ok"));
        final LinearLayout host = Ui.vbox(this);
        LinearLayout row = Ui.hbox(this);
        LinearLayout.LayoutParams p1 = Ui.lpm(0, Ui.WRAP, 0, 0, 5, 0);
        p1.weight = 1.4f;
        LinearLayout.LayoutParams p2 = Ui.lpm(0, Ui.WRAP, 5, 0, 0, 0);
        p2.weight = 1;
        row.addView(Ui.field(this, "Search products", q), p1);
        row.addView(Ui.field(this, "Stock status", sf), p2);
        panel.addView(row);
        panel.addView(host);
        add(panel);
        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                fillProducts(host, txt(q).toLowerCase(Locale.ROOT).trim(), sf.value());
            }
        };
        Ui.watch(q, refresh);
        sf.onChange = refresh;
        refresh.run();
    }

    private void fillProducts(LinearLayout host, String q, String f) {
        host.removeAllViews();
        List<View[]> rows = new ArrayList<>();
        for (final Product p : st.products) {
            if (!(p.name + " " + p.category).toLowerCase(Locale.ROOT).contains(q)) continue;
            if (f.equals("low") && !isLow(p)) continue;
            if (f.equals("ok") && isLow(p)) continue;
            View acts = Ui.actions(this,
                    sb("Edit", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            productForm(p);
                        }
                    }),
                    sb("Adjust", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            adjustStock(p);
                        }
                    }),
                    sb("Delete", 3, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            confirmDelete(new Runnable() {
                                @Override
                                public void run() {
                                    st.deleteProduct(p.id);
                                    render();
                                    toast("Record deleted");
                                }
                            });
                        }
                    }));
            rows.add(new View[]{
                    Ui.cell(this, p.name, false, true),
                    Ui.cell(this, p.category.isEmpty() ? "—" : p.category, false, false),
                    Ui.cell(this, Util.money(p.cost), true, false),
                    Ui.cell(this, Util.money(p.price), true, false),
                    Ui.cellOf(this, Ui.badge(this, Util.num(p.stock), isLow(p) ? 2 : 1)),
                    acts});
        }
        host.addView(Ui.table(this,
                new String[]{"Product", "Category", "Purchase Price", "Selling Price", "Stock", "Actions"},
                new boolean[]{false, false, true, true, false, false}, rows, "No records yet"));
    }

    private void productForm(final Product existing) {
        final Ui.Modal m = new Ui.Modal(this, existing == null ? "Add Product" : "Edit Product");
        final EditText name = ed(existing == null ? "" : existing.name, Ui.TXT);
        final EditText cat = ed(existing == null ? "" : existing.category, Ui.TXT);
        final EditText cost = ed(existing == null ? "0" : Util.num(existing.cost), Ui.NUM);
        final EditText price = ed(existing == null ? "0" : Util.num(existing.price), Ui.NUM);
        final EditText stock = ed(existing == null ? "0" : Util.num(existing.stock), Ui.NUMS);
        m.body.addView(Ui.field(this, "Product name", name));
        m.body.addView(Ui.field(this, "Category", cat));
        m.body.addView(Ui.field(this, "Purchase price (₹)", cost));
        m.body.addView(Ui.field(this, "Selling price (₹)", price));
        m.body.addView(Ui.field(this, "Current stock quantity", stock));
        m.body.addView(Ui.bar(this,
                nb("Cancel", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                    }
                }),
                nb("Save Product", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String n = txt(name).trim();
                        if (n.isEmpty()) {
                            toast("Enter product name");
                            return;
                        }
                        Product p = existing;
                        if (p == null) {
                            p = new Product();
                            p.id = Util.uid();
                            st.products.add(p);
                        }
                        p.name = n;
                        p.category = txt(cat);
                        p.cost = Util.parse(txt(cost));
                        p.price = Util.parse(txt(price));
                        p.stock = Util.parse(txt(stock));
                        st.save();
                        m.dismiss();
                        render();
                        toast("Product saved");
                    }
                })));
        m.show();
    }

    private void adjustStock(final Product p) {
        final Ui.Modal m = new Ui.Modal(this, "Stock Adjustment");
        m.body.addView(Ui.tv(this, p.name + " · Current stock: " + Util.num(p.stock), 14, Ui.INK, true),
                Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));
        final EditText adj = ed("0", Ui.NUMS);
        final EditText reason = ed("Physical count correction", Ui.TXT);
        m.body.addView(Ui.field(this, "Adjustment quantity (+/-)", adj));
        m.body.addView(Ui.field(this, "Reason", reason));
        m.body.addView(Ui.bar(this,
                nb("Cancel", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                    }
                }),
                nb("Save Adjustment", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        double q = Util.parse(txt(adj));
                        if (q == 0) {
                            toast("Enter a non-zero quantity");
                            return;
                        }
                        p.stock += q;
                        Adjustment a = new Adjustment();
                        a.id = Util.uid();
                        a.productId = p.id;
                        a.qty = q;
                        a.reason = txt(reason);
                        a.date = Util.today();
                        st.adjustments.add(a);
                        st.save();
                        m.dismiss();
                        render();
                        toast("Stock adjusted");
                    }
                })));
        m.show();
    }

    // ============================================================ PURCHASES

    private void purchases() {
        add(Ui.pageHead(this, "Purchases", "Supplier purchases and stock-in records",
                nb("＋ Add Purchase", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new PurchaseForm().show();
                    }
                })));
        List<Purchase> list = new ArrayList<>(st.purchases);
        Collections.sort(list, new Comparator<Purchase>() {
            @Override
            public int compare(Purchase a, Purchase b) {
                return b.date.compareTo(a.date);
            }
        });
        List<View[]> rows = new ArrayList<>();
        for (final Purchase x : list) {
            View acts = Ui.actions(this,
                    sb("View", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewPurchase(x);
                        }
                    }),
                    sb("Delete", 3, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            confirmDelete(new Runnable() {
                                @Override
                                public void run() {
                                    st.deletePurchase(x.id);
                                    render();
                                    toast("Record deleted");
                                }
                            });
                        }
                    }));
            rows.add(new View[]{
                    Ui.cell(this, x.number, false, false),
                    Ui.cell(this, Util.fmtDate(x.date), false, false),
                    Ui.cell(this, x.supplier, false, false),
                    Ui.cell(this, Util.money(x.total), true, false),
                    Ui.cell(this, Util.money(x.paid), true, false),
                    Ui.cell(this, Util.money(x.balance), true, false),
                    acts});
        }
        LinearLayout panel = Ui.panel(this);
        panel.addView(Ui.table(this,
                new String[]{"Purchase No.", "Date", "Supplier", "Total", "Paid", "Payable", "Actions"},
                new boolean[]{false, false, false, true, true, true, false}, rows, "No records yet"));
        add(panel);
    }

    private class PurLine {
        Ui.Select prod;
        EditText qty, rate;
        TextView amount;
        LinearLayout view;
    }

    private class PurchaseForm {
        Ui.Modal m;
        Ui.DateField dateF;
        EditText number, supplier, sphone, charges, paid;
        LinearLayout itemsBox, totals;
        final List<PurLine> rows = new ArrayList<>();

        void show() {
            if (st.products.isEmpty()) {
                toast("Add products before purchases");
                return;
            }
            m = new Ui.Modal(MainActivity.this, "New Purchase");
            final Runnable recalc = new Runnable() {
                @Override
                public void run() {
                    recalc();
                }
            };
            number = ed(st.nextPurchaseNumber(), Ui.TXT);
            dateF = new Ui.DateField(MainActivity.this, Util.today());
            supplier = ed("", Ui.TXT);
            sphone = ed("", Ui.PHONE);
            m.body.addView(Ui.field(MainActivity.this, "Purchase number", number));
            m.body.addView(Ui.field(MainActivity.this, "Purchase date", dateF));
            m.body.addView(Ui.field(MainActivity.this, "Supplier name", supplier));
            m.body.addView(Ui.field(MainActivity.this, "Supplier phone", sphone));
            TextView hd = h3("Purchase items");
            hd.setPadding(0, dp(6), 0, dp(8));
            m.body.addView(hd);
            itemsBox = Ui.vbox(MainActivity.this);
            m.body.addView(itemsBox);
            m.body.addView(sb("＋ Add Item", 0, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addLine();
                }
            }));
            charges = ed("0", Ui.NUM);
            paid = ed("0", Ui.NUM);
            Ui.watch(charges, recalc);
            Ui.watch(paid, recalc);
            LinearLayout extra = Ui.vbox(MainActivity.this);
            extra.addView(Ui.field(MainActivity.this, "Additional charges (₹)", charges));
            extra.addView(Ui.field(MainActivity.this, "Amount paid (₹)", paid));
            m.body.addView(extra, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 14, 0, 0));
            totals = Ui.vbox(MainActivity.this);
            m.body.addView(totals);
            m.body.addView(Ui.bar(MainActivity.this,
                    nb("Cancel", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            m.dismiss();
                        }
                    }),
                    nb("Save Purchase", 1, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            save();
                        }
                    })));
            addLine();
            m.show();
        }

        void addLine() {
            final PurLine r = new PurLine();
            List<String> labels = new ArrayList<>(), vals = new ArrayList<>();
            labels.add("Select product");
            vals.add("");
            for (Product p : st.products) {
                labels.add(p.name);
                vals.add(p.id);
            }
            r.prod = new Ui.Select(MainActivity.this, labels, vals);
            r.qty = Ui.edit(MainActivity.this, "1", "Qty", Ui.NUM);
            r.rate = Ui.edit(MainActivity.this, "", "Cost", Ui.NUM);
            r.amount = Ui.tv(MainActivity.this, Util.money(0), 13, Ui.INK, true);
            r.amount.setPadding(0, dp(11), 0, dp(11));
            Runnable recalc = new Runnable() {
                @Override
                public void run() {
                    recalc();
                }
            };
            Ui.watch(r.qty, recalc);
            Ui.watch(r.rate, recalc);
            r.prod.onChange = recalc;
            LinearLayout v = Ui.vbox(MainActivity.this);
            v.setPadding(dp(10), dp(10), dp(10), dp(10));
            v.setBackground(Ui.rect(0xFFF8FAFC, Ui.LINE, 10));
            v.addView(r.prod, Ui.lp(Ui.MATCH, Ui.WRAP));
            LinearLayout row = Ui.hbox(MainActivity.this);
            row.setGravity(Gravity.BOTTOM);
            row.addView(col("Qty", r.qty, 1f));
            row.addView(col("Cost", r.rate, 1.3f));
            row.addView(col("Amount", r.amount, 1.4f));
            row.addView(sb("✕", 3, new View.OnClickListener() {
                @Override
                public void onClick(View x) {
                    rows.remove(r);
                    itemsBox.removeView(r.view);
                    recalc();
                }
            }), Ui.lp(Ui.WRAP, Ui.WRAP));
            v.addView(row, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 8, 0, 0));
            r.view = v;
            rows.add(r);
            itemsBox.addView(v, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
            recalc();
        }

        List<Line> lines() {
            List<Line> out = new ArrayList<>();
            for (PurLine r : rows) {
                Product p = st.product(r.prod.value());
                double q = Util.parse(txt(r.qty));
                if (p == null || q <= 0) continue;
                Line l = new Line();
                l.productId = p.id;
                l.name = p.name;
                l.qty = q;
                l.rate = Util.parse(txt(r.rate));
                out.add(l);
            }
            return out;
        }

        void recalc() {
            if (totals == null) return;
            for (PurLine r : rows) {
                r.amount.setText(Util.money(Util.parse(txt(r.qty)) * Util.parse(txt(r.rate))));
            }
            double sub = 0;
            for (Line l : lines()) sub += l.qty * l.rate;
            double ch = Util.parse(txt(charges));
            double total = sub + ch;
            double pd = Util.parse(txt(paid));
            totals.removeAllViews();
            totals.addView(Ui.totalRow(MainActivity.this, "Items total", Util.money(sub), false));
            totals.addView(Ui.totalRow(MainActivity.this, "Additional charges", Util.money(ch), false));
            totals.addView(Ui.totalRow(MainActivity.this, "Total", Util.money(total), true));
            totals.addView(Ui.totalRow(MainActivity.this, "Payable", Util.money(Math.max(0, total - pd)), false));
        }

        void save() {
            List<Line> items = lines();
            if (items.isEmpty()) {
                toast("Add purchase items");
                return;
            }
            double sub = 0;
            for (Line l : items) sub += l.qty * l.rate;
            double ch = Util.parse(txt(charges));
            double total = sub + ch;
            double pd = Util.parse(txt(paid));
            String num = txt(number).trim();
            if (num.isEmpty()) {
                toast("Enter purchase number");
                return;
            }
            if (st.purchaseNumberExists(num)) {
                toast("Purchase number already exists");
                return;
            }
            if (pd < 0 || pd > total) {
                toast("Paid amount cannot exceed total");
                return;
            }
            for (Line l : items) {
                Product p = st.product(l.productId);
                if (p != null) {
                    p.stock += l.qty;
                    p.cost = l.rate;
                }
            }
            Purchase x = new Purchase();
            x.id = Util.uid();
            x.number = num;
            x.date = dateF.iso;
            x.supplier = txt(supplier);
            x.phone = txt(sphone);
            x.items = items;
            x.subtotal = sub;
            x.charges = ch;
            x.total = total;
            x.paid = pd;
            x.balance = total - pd;
            x.created = Util.nowIso();
            st.purchases.add(x);
            st.save();
            m.dismiss();
            render();
            toast("Purchase saved and stock updated");
        }
    }

    private void viewPurchase(final Purchase x) {
        final Ui.Modal m = new Ui.Modal(this, "Purchase · " + x.number);
        m.body.addView(Ui.tv(this, x.supplier + (x.phone.isEmpty() ? "" : " · " + x.phone), 14, Ui.INK, true));
        m.body.addView(Ui.tv(this, "Date: " + Util.fmtDate(x.date), 14, Ui.INK, false),
                Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 4, 0, 10));
        List<View[]> rows = new ArrayList<>();
        for (Line l : x.items) {
            rows.add(new View[]{
                    Ui.cell(this, l.name, false, false),
                    Ui.cell(this, Util.num(l.qty), true, false),
                    Ui.cell(this, Util.money(l.rate), true, false),
                    Ui.cell(this, Util.money(l.qty * l.rate), true, false)});
        }
        m.body.addView(Ui.table(this, new String[]{"Product", "Qty", "Rate", "Amount"},
                new boolean[]{false, true, true, true}, rows, "No items"));
        LinearLayout tot = Ui.vbox(this);
        tot.addView(Ui.totalRow(this, "Total", Util.money(x.total), true));
        tot.addView(Ui.totalRow(this, "Paid", Util.money(x.paid), false));
        tot.addView(Ui.totalRow(this, "Payable", Util.money(x.balance), false));
        m.body.addView(tot, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        m.body.addView(Ui.bar(this,
                nb("Close", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                    }
                }),
                nb("Print", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        printDoc(purchaseDoc(x), "Purchase " + x.number);
                    }
                })));
        m.show();
    }

    // ============================================================ CUSTOMERS

    private void customers() {
        add(Ui.pageHead(this, "Customers", "Customer accounts, sales history and ledger",
                nb("＋ Add Customer", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        customerForm(null);
                    }
                })));
        LinearLayout panel = Ui.panel(this);
        final EditText q = Ui.edit(this, "", "Name or phone", Ui.TXT);
        final LinearLayout host = Ui.vbox(this);
        panel.addView(Ui.field(this, "Search customers", q));
        panel.addView(host);
        add(panel);
        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                fillCustomers(host, txt(q).toLowerCase(Locale.ROOT).trim());
            }
        };
        Ui.watch(q, refresh);
        refresh.run();
    }

    private void fillCustomers(LinearLayout host, String q) {
        host.removeAllViews();
        List<View[]> rows = new ArrayList<>();
        for (final Customer c : st.customers) {
            if (!(c.name + " " + c.phone).toLowerCase(Locale.ROOT).contains(q)) continue;
            double total = 0, paid = 0, bal = 0;
            for (Invoice x : st.invoices) {
                if (!x.customerId.equals(c.id)) continue;
                total += x.total;
                paid += x.paid;
                bal += x.balance;
            }
            View acts = Ui.actions(this,
                    sb("Ledger", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ledger(c);
                        }
                    }),
                    sb("Edit", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            customerForm(c);
                        }
                    }),
                    sb("Delete", 3, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            confirmDelete(new Runnable() {
                                @Override
                                public void run() {
                                    st.deleteCustomer(c.id);
                                    render();
                                    toast("Record deleted");
                                }
                            });
                        }
                    }));
            rows.add(new View[]{
                    Ui.cell(this, c.name, false, true),
                    Ui.cell(this, c.phone, false, false),
                    Ui.cell(this, c.address.isEmpty() ? "—" : c.address, false, false),
                    Ui.cell(this, Util.money(total), false, false),
                    Ui.cell(this, Util.money(paid), false, false),
                    Ui.cell(this, Util.money(bal), false, false),
                    acts});
        }
        host.addView(Ui.table(this,
                new String[]{"Customer", "Phone", "Address", "Sales", "Paid", "Outstanding", "Actions"},
                null, rows, "No records yet"));
    }

    private void customerForm(final Customer existing) {
        final Ui.Modal m = new Ui.Modal(this, existing == null ? "Add Customer" : "Edit Customer");
        final EditText name = ed(existing == null ? "" : existing.name, Ui.TXT);
        final EditText phone = ed(existing == null ? "" : existing.phone, Ui.PHONE);
        final EditText address = ed(existing == null ? "" : existing.address, Ui.MULTI);
        m.body.addView(Ui.field(this, "Customer name", name));
        m.body.addView(Ui.field(this, "Phone", phone));
        m.body.addView(Ui.field(this, "Address", address));
        m.body.addView(Ui.bar(this,
                nb("Cancel", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                    }
                }),
                nb("Save Customer", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String n = txt(name).trim();
                        if (n.isEmpty()) {
                            toast("Enter customer name");
                            return;
                        }
                        Customer c = existing;
                        if (c == null) {
                            c = new Customer();
                            c.id = Util.uid();
                            st.customers.add(c);
                        }
                        c.name = n;
                        c.phone = txt(phone);
                        c.address = txt(address);
                        st.save();
                        m.dismiss();
                        render();
                        toast("Customer saved");
                    }
                })));
        m.show();
    }

    private void ledger(final Customer c) {
        final Ui.Modal m = new Ui.Modal(this, "Customer Ledger · " + c.name);
        m.body.addView(Ui.tv(this, c.phone + " · " + c.address, 14, Ui.INK, false),
                Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        List<Invoice> inv = new ArrayList<>();
        double sales = 0, paid = 0, due = 0;
        for (Invoice x : st.invoices) {
            if (!x.customerId.equals(c.id)) continue;
            inv.add(x);
            sales += x.total;
            paid += x.paid;
            due += x.balance;
        }
        List<String[]> stats = new ArrayList<>();
        stats.add(new String[]{"Sales", Util.money(sales), ""});
        stats.add(new String[]{"Invoice Paid", Util.money(paid), ""});
        stats.add(new String[]{"Outstanding", Util.money(due), ""});
        m.body.addView(Ui.statGrid(this, stats, 2));

        m.body.addView(h3("Invoices"));
        List<View[]> rows = new ArrayList<>();
        for (Invoice x : inv) {
            rows.add(new View[]{
                    Ui.cell(this, Util.fmtDate(x.date), false, false),
                    Ui.cell(this, x.number, false, false),
                    Ui.cell(this, Util.money(x.total), false, false),
                    Ui.cell(this, Util.money(x.paid), false, false),
                    Ui.cell(this, Util.money(x.balance), false, false)});
        }
        m.body.addView(Ui.table(this, new String[]{"Date", "Invoice", "Total", "Paid", "Due"}, null, rows,
                "No records yet"));

        m.body.addView(h3("Payments recorded"));
        List<View[]> prow = new ArrayList<>();
        for (Payment p : st.payments) {
            if (!p.customerId.equals(c.id)) continue;
            prow.add(new View[]{
                    Ui.cell(this, Util.fmtDate(p.date), false, false),
                    Ui.cell(this, Util.money(p.amount), false, false),
                    Ui.cell(this, p.mode, false, false),
                    Ui.cell(this, p.note, false, false)});
        }
        m.body.addView(Ui.table(this, new String[]{"Date", "Amount", "Mode", "Note"}, null, prow,
                "No records yet"));

        m.body.addView(Ui.bar(this,
                sb("Close", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                    }
                }),
                sb("＋ Record Payment", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                        recordPayment(c);
                    }
                }),
                sb("Print Statement", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        printDoc(ledgerDoc(c), "Statement " + c.name);
                    }
                })));
        m.show();
    }

    private void recordPayment(final Customer c) {
        final Ui.Modal m = new Ui.Modal(this, "Record Customer Payment");
        final Ui.DateField date = new Ui.DateField(this, Util.today());
        final EditText amount = ed("", Ui.NUM);
        final Ui.Select mode = Ui.Select.of(this, "Cash", "UPI", "Bank Transfer");
        final EditText note = ed("", Ui.TXT);
        m.body.addView(Ui.field(this, "Date", date));
        m.body.addView(Ui.field(this, "Amount (₹)", amount));
        m.body.addView(Ui.field(this, "Payment mode", mode));
        m.body.addView(Ui.field(this, "Note", note));
        m.body.addView(Ui.bar(this,
                nb("Cancel", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                        ledger(c);
                    }
                }),
                nb("Save Payment", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        double a = Util.parse(txt(amount));
                        if (a == 0) {
                            toast("Enter payment amount");
                            return;
                        }
                        Payment p = new Payment();
                        p.id = Util.uid();
                        p.customerId = c.id;
                        p.date = date.iso;
                        p.amount = a;
                        p.mode = mode.value();
                        p.note = txt(note);
                        double left = st.applyCustomerPayment(p);
                        m.dismiss();
                        render();
                        ledger(c);
                        toast(left > 0 ? "Payment saved; excess recorded in ledger"
                                : "Payment allocated to oldest due invoices");
                    }
                })));
        m.show();
    }

    // ============================================================ EXPENSES

    private void expenses() {
        add(Ui.pageHead(this, "Expenses", "Record day-to-day operating expenses",
                nb("＋ Add Expense", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        expenseForm(null);
                    }
                })));
        String today = Util.today(), mo = Util.monthNow();
        double t = 0, mth = 0, all = 0;
        for (Expense e : st.expenses) {
            all += e.amount;
            if (e.date.equals(today)) t += e.amount;
            if (e.date.startsWith(mo)) mth += e.amount;
        }
        LinearLayout panel = Ui.panel(this);
        List<String[]> stats = new ArrayList<>();
        stats.add(new String[]{"Today", Util.money(t), ""});
        stats.add(new String[]{"This Month", Util.money(mth), ""});
        stats.add(new String[]{"All Expenses", Util.money(all), ""});
        panel.addView(Ui.statGrid(this, stats, 2));

        List<Expense> list = new ArrayList<>(st.expenses);
        Collections.sort(list, new Comparator<Expense>() {
            @Override
            public int compare(Expense a, Expense b) {
                return b.date.compareTo(a.date);
            }
        });
        List<View[]> rows = new ArrayList<>();
        for (final Expense e : list) {
            View acts = Ui.actions(this,
                    sb("Edit", 0, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            expenseForm(e);
                        }
                    }),
                    sb("Delete", 3, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            confirmDelete(new Runnable() {
                                @Override
                                public void run() {
                                    st.deleteExpense(e.id);
                                    render();
                                    toast("Record deleted");
                                }
                            });
                        }
                    }));
            rows.add(new View[]{
                    Ui.cell(this, Util.fmtDate(e.date), false, false),
                    Ui.cell(this, e.category, false, false),
                    Ui.cell(this, e.description, false, false),
                    Ui.cell(this, Util.money(e.amount), true, false),
                    acts});
        }
        panel.addView(Ui.table(this, new String[]{"Date", "Category", "Description", "Amount", "Actions"},
                new boolean[]{false, false, false, true, false}, rows, "No records yet"));
        add(panel);
    }

    private void expenseForm(final Expense existing) {
        final Ui.Modal m = new Ui.Modal(this, existing == null ? "Add Expense" : "Edit Expense");
        final Ui.DateField date = new Ui.DateField(this, existing == null ? Util.today() : existing.date);
        final Ui.Select cat = Ui.Select.of(this, EXPENSE_CATS);
        if (existing != null) cat.setValue(existing.category);
        final EditText amount = ed(existing == null ? "" : Util.num(existing.amount), Ui.NUM);
        final EditText desc = ed(existing == null ? "" : existing.description, Ui.TXT);
        m.body.addView(Ui.field(this, "Date", date));
        m.body.addView(Ui.field(this, "Category", cat));
        m.body.addView(Ui.field(this, "Amount (₹)", amount));
        m.body.addView(Ui.field(this, "Description", desc));
        m.body.addView(Ui.bar(this,
                nb("Cancel", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m.dismiss();
                    }
                }),
                nb("Save Expense", 1, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        double a = Util.parse(txt(amount));
                        if (a == 0 || date.iso.isEmpty()) {
                            toast("Enter date and amount");
                            return;
                        }
                        Expense e = existing;
                        if (e == null) {
                            e = new Expense();
                            e.id = Util.uid();
                            st.expenses.add(e);
                        }
                        e.date = date.iso;
                        e.category = cat.value();
                        e.amount = a;
                        e.description = txt(desc);
                        st.save();
                        m.dismiss();
                        render();
                        toast("Expense saved");
                    }
                })));
        m.show();
    }

    // ============================================================ REPORTS

    private static class Rep {
        List<Invoice> s = new ArrayList<>();
        List<Purchase> p = new ArrayList<>();
        List<Expense> e = new ArrayList<>();
        double sales, purchases, expenses, gst, nonGst, nil, outstanding, profit;
        double[] collected = new double[4];
    }

    private Rep compute(String from, String to) {
        Rep r = new Rep();
        for (Invoice x : st.invoices)
            if (x.date.compareTo(from) >= 0 && x.date.compareTo(to) <= 0) r.s.add(x);
        for (Purchase x : st.purchases)
            if (x.date.compareTo(from) >= 0 && x.date.compareTo(to) <= 0) r.p.add(x);
        for (Expense x : st.expenses)
            if (x.date.compareTo(from) >= 0 && x.date.compareTo(to) <= 0) r.e.add(x);
        for (Invoice x : r.s) {
            r.sales += x.total;
            r.outstanding += x.balance;
            if (x.type.equals("GST")) r.gst += x.total;
            else if (x.type.equals("Non-GST")) r.nonGst += x.total;
            else if (x.type.equals("NIL")) r.nil += x.total;
            double pr = 0;
            for (Line l : x.items) pr += (l.rate - l.cost) * l.qty;
            r.profit += pr - x.discount;
            for (int i = 0; i < MODES.length; i++) if (x.mode.equals(MODES[i])) r.collected[i] += x.paid;
        }
        for (Purchase x : r.p) r.purchases += x.total;
        for (Expense x : r.e) r.expenses += x.amount;
        return r;
    }

    private void reports() {
        add(Ui.pageHead(this, "Reports & Analytics",
                "Sales, purchases, GST, collections and profitability",
                nb("⇩ Export CSV", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        exportCsv();
                    }
                }),
                nb("Print / PDF", 0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        printDoc(reportDoc(), "Report");
                    }
                })));
        LinearLayout panel = Ui.panel(this);
        final Ui.DateField from = new Ui.DateField(this, reportFrom);
        final Ui.DateField to = new Ui.DateField(this, reportTo);
        final LinearLayout host = Ui.vbox(this);
        LinearLayout row = Ui.hbox(this);
        LinearLayout.LayoutParams p1 = Ui.lpm(0, Ui.WRAP, 0, 0, 5, 0);
        p1.weight = 1;
        LinearLayout.LayoutParams p2 = Ui.lpm(0, Ui.WRAP, 5, 0, 0, 0);
        p2.weight = 1;
        row.addView(Ui.field(this, "From date", from), p1);
        row.addView(Ui.field(this, "To date", to), p2);
        panel.addView(row);
        panel.addView(Ui.bar(this, nb("Generate Report", 1, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportFrom = from.iso;
                reportTo = to.iso;
                showReport(host);
            }
        })));
        panel.addView(host);
        add(panel);
        showReport(host);
    }

    private void showReport(LinearLayout host) {
        host.removeAllViews();
        Rep r = compute(reportFrom, reportTo);
        List<String[]> stats = new ArrayList<>();
        stats.add(new String[]{"Sales", Util.money(r.sales), r.s.size() + " invoices"});
        stats.add(new String[]{"Purchases", Util.money(r.purchases), r.p.size() + " entries"});
        stats.add(new String[]{"Expenses", Util.money(r.expenses), r.e.size() + " entries"});
        stats.add(new String[]{"GST Sales", Util.money(r.gst), ""});
        stats.add(new String[]{"Non-GST Sales", Util.money(r.nonGst), ""});
        stats.add(new String[]{"NIL Sales", Util.money(r.nil), ""});
        LinearLayout g = Ui.statGrid(this, stats, 2);
        g.setPadding(0, dp(12), 0, 0);
        host.addView(g);

        host.addView(h3("Payment Collection"));
        List<View[]> rows = new ArrayList<>();
        for (int i = 0; i < MODES.length; i++) {
            rows.add(new View[]{Ui.cell(this, MODES[i], false, false),
                    Ui.cell(this, Util.money(r.collected[i]), false, false)});
        }
        host.addView(Ui.table(this, new String[]{"Mode", "Collected"}, null, rows, ""));

        host.addView(h3("Outstanding & Profit Estimate"));
        host.addView(labelValue("Customer outstanding:", Util.money(r.outstanding)));
        host.addView(labelValue("Estimated gross profit:", Util.money(r.profit)));
        TextView note = Ui.tv(this,
                "Profit estimate uses recorded purchase cost for each sold item and excludes overheads, taxes and other adjustments.",
                13, Ui.MUTED, false);
        note.setPadding(0, dp(6), 0, 0);
        host.addView(note);

        host.addView(h3("Period Transactions"));
        List<View[]> trows = new ArrayList<>();
        for (Invoice x : r.s) {
            trows.add(new View[]{Ui.cell(this, Util.fmtDate(x.date), false, false),
                    Ui.cell(this, "Sale · " + x.type, false, false),
                    Ui.cell(this, x.number, false, false),
                    Ui.cell(this, x.customer.isEmpty() ? "Walk-in" : x.customer, false, false),
                    Ui.cell(this, Util.money(x.total), false, false),
                    Ui.cell(this, Util.money(x.balance), false, false)});
        }
        for (Purchase x : r.p) {
            trows.add(new View[]{Ui.cell(this, Util.fmtDate(x.date), false, false),
                    Ui.cell(this, "Purchase", false, false),
                    Ui.cell(this, x.number, false, false),
                    Ui.cell(this, x.supplier, false, false),
                    Ui.cell(this, Util.money(x.total), false, false),
                    Ui.cell(this, Util.money(x.balance), false, false)});
        }
        for (Expense x : r.e) {
            trows.add(new View[]{Ui.cell(this, Util.fmtDate(x.date), false, false),
                    Ui.cell(this, "Expense", false, false),
                    Ui.cell(this, "—", false, false),
                    Ui.cell(this, x.category, false, false),
                    Ui.cell(this, Util.money(x.amount), false, false),
                    Ui.cell(this, "—", false, false)});
        }
        host.addView(Ui.table(this,
                new String[]{"Date", "Type", "Reference", "Party", "Amount", "Balance"}, null, trows,
                "No records yet"));
    }

    private void exportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type,Date,Reference,Party,Total,Paid,Balance\r\n");
        for (Invoice x : st.invoices) {
            sb.append(Util.csv("Sale " + x.type)).append(',').append(Util.csv(x.date)).append(',')
                    .append(Util.csv(x.number)).append(',').append(Util.csv(x.customer)).append(',')
                    .append(Util.csv(Util.num(x.total))).append(',').append(Util.csv(Util.num(x.paid))).append(',')
                    .append(Util.csv(Util.num(x.balance))).append("\r\n");
        }
        for (Purchase x : st.purchases) {
            sb.append(Util.csv("Purchase")).append(',').append(Util.csv(x.date)).append(',')
                    .append(Util.csv(x.number)).append(',').append(Util.csv(x.supplier)).append(',')
                    .append(Util.csv(Util.num(x.total))).append(',').append(Util.csv(Util.num(x.paid))).append(',')
                    .append(Util.csv(Util.num(x.balance))).append("\r\n");
        }
        for (Expense x : st.expenses) {
            sb.append(Util.csv("Expense")).append(',').append(Util.csv(x.date)).append(',')
                    .append(Util.csv(x.category)).append(',').append(Util.csv(x.description)).append(',')
                    .append(Util.csv(Util.num(x.amount))).append(',').append(Util.csv("")).append(',')
                    .append(Util.csv("")).append("\r\n");
        }
        try {
            String where = Util.saveDownload(this, "SK-Traders-Report-" + Util.today() + ".csv",
                    "text/csv", ("﻿" + sb).getBytes(StandardCharsets.UTF_8));
            toast("Saved CSV to " + where);
        } catch (Exception e) {
            toast("Save failed: " + e.getMessage());
        }
    }

    // ============================================================ SETTINGS

    private void settings() {
        add(Ui.pageHead(this, "Settings", "Business profile, invoice preferences and data safety"));

        LinearLayout biz = Ui.panel(this);
        biz.addView(h3("Business Details"));
        final EditText name = ed(st.name, Ui.TXT);
        final EditText prefix = ed(st.prefix, Ui.TXT);
        final EditText addr = ed(st.address, Ui.MULTI);
        final EditText phone = ed(st.phone, Ui.PHONE);
        final EditText wa = ed(st.whatsapp, Ui.PHONE);
        final EditText low = ed(Util.num(st.lowStock), Ui.NUM);
        biz.addView(Ui.field(this, "Business name", name));
        biz.addView(Ui.field(this, "Invoice prefix", prefix));
        biz.addView(Ui.field(this, "Address", addr));
        biz.addView(Ui.field(this, "Call / Phone", phone));
        biz.addView(Ui.field(this, "WhatsApp number", wa));
        biz.addView(Ui.field(this, "Low-stock alert quantity", low));
        biz.addView(Ui.bar(this, nb("Save Settings", 1, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                st.name = txt(name);
                st.prefix = txt(prefix).isEmpty() ? "SKT" : txt(prefix);
                st.address = txt(addr);
                st.phone = txt(phone);
                st.whatsapp = txt(wa);
                st.lowStock = Util.parse(txt(low));
                st.save();
                render();
                toast("Settings saved");
            }
        })));
        add(biz);

        LinearLayout bk = Ui.panel(this);
        bk.addView(h3("Backup & Restore"));
        TextView notice = Ui.tv(this,
                "Your records are saved inside this app on this phone only. Uninstalling the app or clearing its data will remove them. Download a backup regularly and keep a copy somewhere safe (Google Drive, WhatsApp, email).",
                12, 0xFF755A22, false);
        notice.setBackground(Ui.rect(0xFFFFF8E8, 0xFFF1DFB7, 10));
        notice.setPadding(dp(12), dp(12), dp(12), dp(12));
        bk.addView(notice, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 0, 0, 14));
        LinearLayout actions = Ui.vbox(this);
        actions.addView(nb("⇩ Download Complete JSON Backup", 1, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backup();
            }
        }), Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
        actions.addView(nb("⇧ Restore JSON Backup", 0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(i, REQ_RESTORE);
                } catch (ActivityNotFoundException e) {
                    toast("No file picker available");
                }
            }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));
        bk.addView(actions);
        TextView foot = Ui.tv(this, "Restore will never silently overwrite your current data. It will ask before replacing it.",
                11, Ui.MUTED, false);
        foot.setPadding(0, dp(14), 0, 0);
        bk.addView(foot);
        add(bk);

        LinearLayout ov = Ui.panel(this);
        ov.addView(h3("Data Overview"));
        ov.addView(Ui.tv(this, "Products: " + st.products.size() + " · Customers: " + st.customers.size()
                + " · Invoices: " + st.invoices.size() + " · Purchases: " + st.purchases.size()
                + " · Expenses: " + st.expenses.size(), 14, Ui.INK, false), Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));
        ov.addView(nb("Delete all business data", 3, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm("Delete ALL products, customers, invoices, purchases and expenses? This cannot be undone. Download a backup first.",
                        new Runnable() {
                            @Override
                            public void run() {
                                confirm("Final confirmation: permanently clear all business records on this phone?",
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                st.clearAll();
                                                st.save();
                                                render();
                                                toast("Business data cleared");
                                            }
                                        });
                            }
                        });
            }
        }));
        add(ov);
    }

    private void backup() {
        try {
            String json = st.toJson(true);
            String where = Util.saveDownload(this, "SK-Traders-Backup-" + Util.today() + ".json",
                    "application/json", json.getBytes(StandardCharsets.UTF_8));
            toast("Backup saved to " + where);
        } catch (Exception e) {
            toast("Backup failed: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_RESTORE || resultCode != RESULT_OK || data == null || data.getData() == null)
            return;
        try {
            final String text = new String(Util.readAll(getContentResolver().openInputStream(data.getData())),
                    StandardCharsets.UTF_8);
            new Store(this).fromJson(text); // validate first
            confirm("This will replace all current business data with the backup. Download a backup of current data first if needed. Continue?",
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                st.fromJson(text);
                                st.save();
                                render();
                                toast("Backup restored");
                            } catch (JSONException e) {
                                toast("Could not restore: " + e.getMessage());
                            }
                        }
                    });
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Could not restore: " + e.getMessage())
                    .setPositiveButton("OK", null).show();
        }
    }

    // ============================================================ PRINTING (native PDF)

    private void printDoc(PdfDoc d, String jobName) {
        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
        if (pm == null) {
            toast("Printing is not available");
            return;
        }
        pm.print(jobName, new PdfDoc.Adapter(this, d, jobName),
                new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
    }

    private PdfDoc header(PdfDoc d) {
        return d.text(st.name, 18, true, PdfDoc.NAVY, 0)
                .text(st.address, 10, false)
                .text("Phone: " + st.phone + " · WhatsApp: " + st.whatsapp, 10, false)
                .rule(PdfDoc.GOLD).gap(8);
    }

    private PdfDoc invoiceDoc(Invoice x) {
        PdfDoc d = header(new PdfDoc());
        d.kv(x.type + " TAX INVOICE", "Bill To", true, 10, 0)
                .kv("Invoice: " + x.number, x.customer, false, 10, 0)
                .kv("Date: " + Util.fmtDate(x.date), x.phone, false, 10, 0)
                .kv("", x.address, false, 10, 0).gap(10);
        float[] w = {4, 1, 1.6f, 1, 1.8f};
        boolean[] r = {false, true, true, true, true};
        d.head(new String[]{"Product", "Qty", "Rate", "GST", "Amount"}, w, r);
        for (Line l : x.items) {
            d.row(new String[]{l.name, Util.num(l.qty), Util.money(l.rate), Util.num(l.tax) + "%",
                    Util.money(l.qty * l.rate)}, w, r);
        }
        d.gap(10)
                .kv("Subtotal", Util.money(x.subtotal), false, 10, 0.5f)
                .kv("GST", Util.money(x.tax), false, 10, 0.5f)
                .kv("Discount", Util.money(x.discount), false, 10, 0.5f)
                .kv("Grand Total", Util.money(x.total), true, 12, 0.5f)
                .kv("Amount Paid", Util.money(x.paid), false, 10, 0.5f)
                .kv("Balance Due", Util.money(x.balance), false, 10, 0.5f)
                .gap(8).text("Payment mode: " + x.mode, 10, false).gap(24)
                .text("Thank you for your business.", 10, false, PdfDoc.MUTED, 1);
        return d;
    }

    private PdfDoc purchaseDoc(Purchase x) {
        PdfDoc d = header(new PdfDoc());
        d.text("PURCHASE " + x.number, 11, true).text("Supplier: " + x.supplier
                + (x.phone.isEmpty() ? "" : " · " + x.phone), 10, false)
                .text("Date: " + Util.fmtDate(x.date), 10, false).gap(8);
        float[] w = {4, 1, 1.6f, 1.8f};
        boolean[] r = {false, true, true, true};
        d.head(new String[]{"Product", "Qty", "Rate", "Amount"}, w, r);
        for (Line l : x.items) {
            d.row(new String[]{l.name, Util.num(l.qty), Util.money(l.rate), Util.money(l.qty * l.rate)}, w, r);
        }
        d.gap(10)
                .kv("Total", Util.money(x.total), true, 12, 0.5f)
                .kv("Paid", Util.money(x.paid), false, 10, 0.5f)
                .kv("Payable", Util.money(x.balance), false, 10, 0.5f);
        return d;
    }

    private PdfDoc ledgerDoc(Customer c) {
        PdfDoc d = header(new PdfDoc());
        d.text("CUSTOMER STATEMENT", 11, true).text(c.name + " · " + c.phone, 10, false)
                .text(c.address, 10, false).gap(8);
        float[] w = {1.4f, 2, 1.6f, 1.6f, 1.6f};
        boolean[] r = {false, false, true, true, true};
        d.head(new String[]{"Date", "Invoice", "Total", "Paid", "Due"}, w, r);
        double sales = 0, paid = 0, due = 0;
        for (Invoice x : st.invoices) {
            if (!x.customerId.equals(c.id)) continue;
            sales += x.total;
            paid += x.paid;
            due += x.balance;
            d.row(new String[]{Util.fmtDate(x.date), x.number, Util.money(x.total), Util.money(x.paid),
                    Util.money(x.balance)}, w, r);
        }
        d.gap(8).kv("Sales", Util.money(sales), false, 10, 0.5f)
                .kv("Invoice Paid", Util.money(paid), false, 10, 0.5f)
                .kv("Outstanding", Util.money(due), true, 12, 0.5f).gap(12);
        d.text("Payments recorded", 11, true);
        float[] w2 = {1.4f, 1.8f, 1.6f, 3};
        boolean[] r2 = {false, true, false, false};
        d.head(new String[]{"Date", "Amount", "Mode", "Note"}, w2, r2);
        for (Payment p : st.payments) {
            if (!p.customerId.equals(c.id)) continue;
            d.row(new String[]{Util.fmtDate(p.date), Util.money(p.amount), p.mode, p.note}, w2, r2);
        }
        return d;
    }

    private PdfDoc reportDoc() {
        Rep r = compute(reportFrom, reportTo);
        PdfDoc d = new PdfDoc();
        d.text(st.name + " — Report", 16, true, PdfDoc.NAVY, 0).text(st.address, 10, false)
                .text("Period: " + Util.fmtDate(reportFrom) + " to " + Util.fmtDate(reportTo), 10, false)
                .rule(PdfDoc.GOLD).gap(8)
                .kv("Sales (" + r.s.size() + " invoices)", Util.money(r.sales), false, 10, 0)
                .kv("Purchases (" + r.p.size() + " entries)", Util.money(r.purchases), false, 10, 0)
                .kv("Expenses (" + r.e.size() + " entries)", Util.money(r.expenses), false, 10, 0)
                .kv("GST Sales", Util.money(r.gst), false, 10, 0)
                .kv("Non-GST Sales", Util.money(r.nonGst), false, 10, 0)
                .kv("NIL Sales", Util.money(r.nil), false, 10, 0).gap(8)
                .text("Payment Collection", 11, true);
        for (int i = 0; i < MODES.length; i++)
            d.kv(MODES[i], Util.money(r.collected[i]), false, 10, 0);
        d.gap(8).kv("Customer outstanding", Util.money(r.outstanding), true, 10, 0)
                .kv("Estimated gross profit", Util.money(r.profit), true, 10, 0).gap(10)
                .text("Period Transactions", 11, true);
        float[] w = {1.4f, 1.8f, 2, 2.4f, 1.8f, 1.8f};
        boolean[] rt = {false, false, false, false, true, true};
        d.head(new String[]{"Date", "Type", "Reference", "Party", "Amount", "Balance"}, w, rt);
        for (Invoice x : r.s)
            d.row(new String[]{Util.fmtDate(x.date), "Sale · " + x.type, x.number,
                    x.customer.isEmpty() ? "Walk-in" : x.customer, Util.money(x.total), Util.money(x.balance)}, w, rt);
        for (Purchase x : r.p)
            d.row(new String[]{Util.fmtDate(x.date), "Purchase", x.number, x.supplier, Util.money(x.total),
                    Util.money(x.balance)}, w, rt);
        for (Expense x : r.e)
            d.row(new String[]{Util.fmtDate(x.date), "Expense", "—", x.category, Util.money(x.amount), "—"}, w, rt);
        return d;
    }
}
