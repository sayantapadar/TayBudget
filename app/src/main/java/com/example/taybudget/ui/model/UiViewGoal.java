package com.example.taybudget.ui.model;

import com.example.taybudget.data.model.Goal;

import java.util.Locale;

public class UiViewGoal {
    private String name;
    private double amount;
    private int months;
    private final Goal goal;

    public UiViewGoal(Goal goal) {
        this.goal = goal;
        this.name = goal.getName();
        this.amount = goal.getAmount();
        if (goal.getCalculation().getMonthlyAggregates() == null || goal.getCalculation().getMonthlyAggregates().size() == 0)
            this.months = 0;
        else
            this.months = goal.getCalculation().getMonthlyAggregates().size();
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

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public String getAmountString() {
        return String.format(Locale.getDefault(), "%.2f", amount);
    }

    public Goal getGoal() {
        return goal;
    }
}
