package com.sktraders.manager;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** All business data + persistence (JSON file in the app's private storage). */
class Store {
    static final String FILE = "sktraders_data.json";

    final Context ctx;

    String name = "S.K TRADERS";
    String address = "Plant Site, Dhaga Gali, Rourkela, Odisha";
    String phone = "9437129612";
    String whatsapp = "9937608901";
    String prefix = "SKT";
    double lowStock = 5;
    int nextInvoice = 1;

    List<Product> products = new ArrayList<>();
    List<Customer> customers = new ArrayList<>();
    List<Invoice> invoices = new ArrayList<>();
    List<Purchase> purchases = new ArrayList<>();
    List<Expense> expenses = new ArrayList<>();
    List<Payment> payments = new ArrayList<>();
    List<Adjustment> adjustments = new ArrayList<>();

    Store(Context c) {
        ctx = c.getApplicationContext();
    }

    // ---------------------------------------------------------------- persistence

    void load() {
        File f = new File(ctx.getFilesDir(), FILE);
        if (!f.exists()) return;
        try {
            FileInputStream in = new FileInputStream(f);
            String text = new String(Util.readAll(in), StandardCharsets.UTF_8);
            fromJson(text);
        } catch (Exception e) {
            // keep the unreadable file so nothing is lost, start clean
            File bad = new File(ctx.getFilesDir(), FILE + ".corrupt");
            //noinspection ResultOfMethodCallIgnored
            f.renameTo(bad);
        }
    }

    void save() {
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            File t = new File(ctx.getFilesDir(), FILE + ".tmp");
            FileOutputStream o = new FileOutputStream(t);
            try {
                o.write(toJson(false).getBytes(StandardCharsets.UTF_8));
                o.flush();
                o.getFD().sync();
            } finally {
                o.close();
            }
            if (!t.renameTo(f)) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
                //noinspection ResultOfMethodCallIgnored
                t.renameTo(f);
            }
        } catch (Exception ignored) {
        }
    }

    void clearAll() {
        name = "S.K TRADERS";
        address = "Plant Site, Dhaga Gali, Rourkela, Odisha";
        phone = "9437129612";
        whatsapp = "9937608901";
        prefix = "SKT";
        lowStock = 5;
        nextInvoice = 1;
        products = new ArrayList<>();
        customers = new ArrayList<>();
        invoices = new ArrayList<>();
        purchases = new ArrayList<>();
        expenses = new ArrayList<>();
        payments = new ArrayList<>();
        adjustments = new ArrayList<>();
    }

    // ---------------------------------------------------------------- JSON

    private static double safe(double d) {
        return (Double.isNaN(d) || Double.isInfinite(d)) ? 0 : d;
    }

    private static String str(JSONObject o, String k, String def) {
        if (o == null || o.isNull(k)) return def;
        return o.optString(k, def);
    }

    private static JSONArray linesToJson(List<Line> lines) throws JSONException {
        JSONArray a = new JSONArray();
        for (Line l : lines) {
            JSONObject j = new JSONObject();
            j.put("productId", l.productId);
            j.put("name", l.name);
            j.put("qty", safe(l.qty));
            j.put("rate", safe(l.rate));
            j.put("tax", safe(l.tax));
            j.put("cost", safe(l.cost));
            a.put(j);
        }
        return a;
    }

    private static List<Line> linesFromJson(JSONArray a) throws JSONException {
        List<Line> out = new ArrayList<>();
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONObject j = a.getJSONObject(i);
            Line l = new Line();
            l.productId = str(j, "productId", "");
            l.name = str(j, "name", "");
            l.qty = j.optDouble("qty", 0);
            l.rate = j.optDouble("rate", 0);
            l.tax = j.optDouble("tax", 0);
            l.cost = j.optDouble("cost", 0);
            out.add(l);
        }
        return out;
    }

    String toJson(boolean forBackup) throws JSONException {
        JSONObject o = new JSONObject();
        JSONObject b = new JSONObject();
        b.put("name", name);
        b.put("address", address);
        b.put("phone", phone);
        b.put("whatsapp", whatsapp);
        b.put("prefix", prefix);
        o.put("business", b);

        JSONArray pa = new JSONArray();
        for (Product p : products) {
            JSONObject j = new JSONObject();
            j.put("id", p.id);
            j.put("name", p.name);
            j.put("category", p.category);
            j.put("cost", safe(p.cost));
            j.put("price", safe(p.price));
            j.put("stock", safe(p.stock));
            pa.put(j);
        }
        o.put("products", pa);

        JSONArray ca = new JSONArray();
        for (Customer c : customers) {
            JSONObject j = new JSONObject();
            j.put("id", c.id);
            j.put("name", c.name);
            j.put("phone", c.phone);
            j.put("address", c.address);
            ca.put(j);
        }
        o.put("customers", ca);
        o.put("suppliers", new JSONArray());

        JSONArray ia = new JSONArray();
        for (Invoice x : invoices) {
            JSONObject j = new JSONObject();
            j.put("id", x.id);
            j.put("number", x.number);
            j.put("date", x.date);
            j.put("type", x.type);
            j.put("customerId", x.customerId);
            j.put("customer", x.customer);
            j.put("phone", x.phone);
            j.put("address", x.address);
            j.put("items", linesToJson(x.items));
            j.put("subtotal", safe(x.subtotal));
            j.put("tax", safe(x.tax));
            j.put("discount", safe(x.discount));
            j.put("total", safe(x.total));
            j.put("paid", safe(x.paid));
            j.put("balance", safe(x.balance));
            j.put("mode", x.mode);
            j.put("created", x.created);
            ia.put(j);
        }
        o.put("invoices", ia);

        JSONArray pu = new JSONArray();
        for (Purchase x : purchases) {
            JSONObject j = new JSONObject();
            j.put("id", x.id);
            j.put("number", x.number);
            j.put("date", x.date);
            j.put("supplier", x.supplier);
            j.put("phone", x.phone);
            j.put("items", linesToJson(x.items));
            j.put("subtotal", safe(x.subtotal));
            j.put("charges", safe(x.charges));
            j.put("total", safe(x.total));
            j.put("paid", safe(x.paid));
            j.put("balance", safe(x.balance));
            j.put("created", x.created);
            pu.put(j);
        }
        o.put("purchases", pu);

        JSONArray ea = new JSONArray();
        for (Expense x : expenses) {
            JSONObject j = new JSONObject();
            j.put("id", x.id);
            j.put("date", x.date);
            j.put("category", x.category);
            j.put("amount", safe(x.amount));
            j.put("description", x.description);
            ea.put(j);
        }
        o.put("expenses", ea);

        JSONArray pay = new JSONArray();
        for (Payment x : payments) {
            JSONObject j = new JSONObject();
            j.put("id", x.id);
            j.put("customerId", x.customerId);
            j.put("date", x.date);
            j.put("amount", safe(x.amount));
            j.put("mode", x.mode);
            j.put("note", x.note);
            pay.put(j);
        }
        o.put("payments", pay);

        JSONArray adj = new JSONArray();
        for (Adjustment x : adjustments) {
            JSONObject j = new JSONObject();
            j.put("id", x.id);
            j.put("productId", x.productId);
            j.put("qty", safe(x.qty));
            j.put("reason", x.reason);
            j.put("date", x.date);
            adj.put(j);
        }
        o.put("adjustments", adj);

        JSONObject s = new JSONObject();
        s.put("lowStock", safe(lowStock));
        s.put("nextInvoice", nextInvoice);
        o.put("settings", s);

        if (forBackup) o.put("backupCreated", Util.nowIso());
        return forBackup ? o.toString(2) : o.toString();
    }

    /** Replaces ALL data with the JSON content. Throws if the file is not a valid backup. */
    void fromJson(String text) throws JSONException {
        JSONObject o = new JSONObject(text);
        JSONObject b = o.optJSONObject("business");
        JSONArray pa = o.optJSONArray("products");
        JSONArray ia = o.optJSONArray("invoices");
        if (b == null || pa == null || ia == null) throw new JSONException("Invalid backup");

        List<Product> np = new ArrayList<>();
        for (int i = 0; i < pa.length(); i++) {
            JSONObject j = pa.getJSONObject(i);
            Product p = new Product();
            p.id = str(j, "id", Util.uid());
            p.name = str(j, "name", "");
            p.category = str(j, "category", "");
            p.cost = j.optDouble("cost", 0);
            p.price = j.optDouble("price", 0);
            p.stock = j.optDouble("stock", 0);
            np.add(p);
        }

        List<Customer> nc = new ArrayList<>();
        JSONArray ca = o.optJSONArray("customers");
        if (ca != null) for (int i = 0; i < ca.length(); i++) {
            JSONObject j = ca.getJSONObject(i);
            Customer c = new Customer();
            c.id = str(j, "id", Util.uid());
            c.name = str(j, "name", "");
            c.phone = str(j, "phone", "");
            c.address = str(j, "address", "");
            nc.add(c);
        }

        List<Invoice> ni = new ArrayList<>();
        for (int i = 0; i < ia.length(); i++) {
            JSONObject j = ia.getJSONObject(i);
            Invoice x = new Invoice();
            x.id = str(j, "id", Util.uid());
            x.number = str(j, "number", "");
            x.date = str(j, "date", "");
            x.type = str(j, "type", "GST");
            x.customerId = str(j, "customerId", "");
            x.customer = str(j, "customer", "Walk-in");
            x.phone = str(j, "phone", "");
            x.address = str(j, "address", "");
            x.items = linesFromJson(j.optJSONArray("items"));
            x.subtotal = j.optDouble("subtotal", 0);
            x.tax = j.optDouble("tax", 0);
            x.discount = j.optDouble("discount", 0);
            x.total = j.optDouble("total", 0);
            x.paid = j.optDouble("paid", 0);
            x.balance = j.optDouble("balance", 0);
            x.mode = str(j, "mode", "Cash");
            x.created = str(j, "created", "");
            ni.add(x);
        }

        List<Purchase> npu = new ArrayList<>();
        JSONArray pu = o.optJSONArray("purchases");
        if (pu != null) for (int i = 0; i < pu.length(); i++) {
            JSONObject j = pu.getJSONObject(i);
            Purchase x = new Purchase();
            x.id = str(j, "id", Util.uid());
            x.number = str(j, "number", "");
            x.date = str(j, "date", "");
            x.supplier = str(j, "supplier", "");
            x.phone = str(j, "phone", "");
            x.items = linesFromJson(j.optJSONArray("items"));
            x.subtotal = j.optDouble("subtotal", 0);
            x.charges = j.optDouble("charges", 0);
            x.total = j.optDouble("total", 0);
            x.paid = j.optDouble("paid", 0);
            x.balance = j.optDouble("balance", 0);
            x.created = str(j, "created", "");
            npu.add(x);
        }

        List<Expense> ne = new ArrayList<>();
        JSONArray ea = o.optJSONArray("expenses");
        if (ea != null) for (int i = 0; i < ea.length(); i++) {
            JSONObject j = ea.getJSONObject(i);
            Expense x = new Expense();
            x.id = str(j, "id", Util.uid());
            x.date = str(j, "date", "");
            x.category = str(j, "category", "Other");
            x.amount = j.optDouble("amount", 0);
            x.description = str(j, "description", "");
            ne.add(x);
        }

        List<Payment> npay = new ArrayList<>();
        JSONArray pay = o.optJSONArray("payments");
        if (pay != null) for (int i = 0; i < pay.length(); i++) {
            JSONObject j = pay.getJSONObject(i);
            Payment x = new Payment();
            x.id = str(j, "id", Util.uid());
            x.customerId = str(j, "customerId", "");
            x.date = str(j, "date", "");
            x.amount = j.optDouble("amount", 0);
            x.mode = str(j, "mode", "");
            x.note = str(j, "note", "");
            npay.add(x);
        }

        List<Adjustment> nadj = new ArrayList<>();
        JSONArray adj = o.optJSONArray("adjustments");
        if (adj != null) for (int i = 0; i < adj.length(); i++) {
            JSONObject j = adj.getJSONObject(i);
            Adjustment x = new Adjustment();
            x.id = str(j, "id", Util.uid());
            x.productId = str(j, "productId", "");
            x.qty = j.optDouble("qty", 0);
            x.reason = str(j, "reason", "");
            x.date = str(j, "date", "");
            nadj.add(x);
        }

        // everything parsed – now assign
        name = str(b, "name", "S.K TRADERS");
        address = str(b, "address", "");
        phone = str(b, "phone", "");
        whatsapp = str(b, "whatsapp", "");
        prefix = str(b, "prefix", "SKT");
        JSONObject s = o.optJSONObject("settings");
        lowStock = s != null ? s.optDouble("lowStock", 5) : 5;
        nextInvoice = s != null ? s.optInt("nextInvoice", 1) : 1;
        if (nextInvoice < 1) nextInvoice = 1;
        products = np;
        customers = nc;
        invoices = ni;
        purchases = npu;
        expenses = ne;
        payments = npay;
        adjustments = nadj;
    }

    // ---------------------------------------------------------------- lookups

    Product product(String id) {
        for (Product p : products) if (p.id.equals(id)) return p;
        return null;
    }

    Customer customer(String id) {
        for (Customer c : customers) if (c.id.equals(id)) return c;
        return null;
    }

    Invoice invoice(String id) {
        for (Invoice x : invoices) if (x.id.equals(id)) return x;
        return null;
    }

    Purchase purchase(String id) {
        for (Purchase x : purchases) if (x.id.equals(id)) return x;
        return null;
    }

    boolean invoiceNumberExists(String n) {
        for (Invoice x : invoices) if (x.number.equals(n)) return true;
        return false;
    }

    boolean purchaseNumberExists(String n) {
        for (Purchase x : purchases) if (x.number.equals(n)) return true;
        return false;
    }

    String nextInvoiceNumber() {
        return (prefix.isEmpty() ? "SKT" : prefix) + "-" + Util.pad(nextInvoice, 5);
    }

    String nextPurchaseNumber() {
        int n = purchases.size() + 1;
        String p = (prefix.isEmpty() ? "SKT" : prefix) + "-PUR-";
        while (purchaseNumberExists(p + Util.pad(n, 4))) n++;
        return p + Util.pad(n, 4);
    }

    double outstanding() {
        double t = 0;
        for (Invoice x : invoices) t += x.balance;
        return t;
    }

    // ---------------------------------------------------------------- mutations

    void deleteInvoice(String id) {
        Invoice x = invoice(id);
        if (x == null) return;
        for (Line l : x.items) {
            Product p = product(l.productId);
            if (p != null) p.stock += l.qty;
        }
        invoices.remove(x);
        save();
    }

    void deletePurchase(String id) {
        Purchase x = purchase(id);
        if (x == null) return;
        for (Line l : x.items) {
            Product p = product(l.productId);
            if (p != null) p.stock -= l.qty;
        }
        purchases.remove(x);
        save();
    }

    void deleteProduct(String id) {
        Product p = product(id);
        if (p != null) products.remove(p);
        save();
    }

    void deleteCustomer(String id) {
        Customer c = customer(id);
        if (c != null) customers.remove(c);
        save();
    }

    void deleteExpense(String id) {
        for (Expense e : new ArrayList<>(expenses)) if (e.id.equals(id)) expenses.remove(e);
        save();
    }

    /** Allocates a customer payment to the oldest unpaid invoices. Returns the unallocated rest. */
    double applyCustomerPayment(Payment pay) {
        double left = pay.amount;
        List<Invoice> due = new ArrayList<>();
        for (Invoice x : invoices) if (x.customerId.equals(pay.customerId) && x.balance > 0) due.add(x);
        Collections.sort(due, new Comparator<Invoice>() {
            @Override
            public int compare(Invoice a, Invoice b) {
                return a.date.compareTo(b.date);
            }
        });
        for (Invoice x : due) {
            double used = Math.min(left, x.balance);
            x.paid += used;
            x.balance -= used;
            left -= used;
            if (left <= 0) break;
        }
        payments.add(pay);
        save();
        return left;
    }
}
