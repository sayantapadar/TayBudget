package com.example.taybudget.ui.model;

public class UiAggregate {
    private String name;
    private double amount;

    public UiAggregate(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }

    public UiAggregate() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
