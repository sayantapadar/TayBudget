package com.example.taybudget.data.model;

import java.io.Serializable;

public class LimitedAmount implements Serializable {
    private double current;
    private double limit;

    public LimitedAmount(double current, double limit) {
        this.current = current;
        this.limit = limit;
    }

    public LimitedAmount() {
    }

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }
}
