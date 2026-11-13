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
            phone = ed
