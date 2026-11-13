package com.sktraders.manager;

import java.util.ArrayList;
import java.util.List;

class Product {
    String id = "", name = "", category = "";
    double cost, price, stock;
}

class Customer {
    String id = "", name = "", phone = "", address = "";
}

/** One item line on an invoice (rate = selling rate) or a purchase (rate = purchase cost). */
class Line {
    String productId = "", name = "";
    double qty, rate, tax, cost;
}

class Invoice {
    String id = "", number = "", date = "", type = "GST", customerId = "", customer = "",
            phone = "", address = "", mode = "Cash", created = "";
    List<Line> items = new ArrayList<>();
    double subtotal, tax, discount, total, paid, balance;
}

class Purchase {
    String id = "", number = "", date = "", supplier = "", phone = "", created = "";
    List<Line> items = new ArrayList<>();
    double subtotal, charges, total, paid, balance;
}

class Expense {
    String id = "", date = "", category = "", description = "";
    double amount;
}

class Payment {
    String id = "", customerId = "", date = "", mode = "", note = "";
    double amount;
}

class Adjustment {
    String id = "", productId = "", reason = "", date = "";
    double qty;
}
