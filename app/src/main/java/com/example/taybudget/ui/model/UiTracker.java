package com.example.taybudget.ui.model;

import com.example.taybudget.data.handler.model.Tracker;
import com.example.taybudget.data.model.Expense;

import java.util.Date;
import java.util.List;

public class UiTracker {
    private final Tracker tracker;
    private boolean selected;
    private List<Expense> expenses;
    private double amount;

    public UiTracker() {
        this.tracker = new Tracker();
    }

    public UiTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    public UiTracker(Tracker tracker, List<Expense> expenses, double amount) {
        this.tracker = tracker;
        this.expenses = expenses;
        this.amount = amount;
    }

    public UiTracker(String name) {
        this.tracker = new Tracker(name);
    }

    public String getName() {
        return tracker.getName();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return tracker.getDate();
    }

    public double getRenewAmount() {
        return tracker.getAmount();
    }

    public Tracker getTracker() {
        return tracker;
    }
}
