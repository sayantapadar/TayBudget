package com.example.taybudget.data.model;

import java.util.Date;

public class Income extends Data {
    private boolean taxable;

    public Income() {
        super();
    }

    public Income(String user, String name, Date date, boolean recurring, Recurring recur, boolean taxable, double amount) {
        super(user, name, date, recurring, recur, amount);
        this.taxable = taxable;
    }

    public boolean isTaxable() {
        return taxable;
    }

    public void setTaxable(boolean taxable) {
        this.taxable = taxable;
    }
}
