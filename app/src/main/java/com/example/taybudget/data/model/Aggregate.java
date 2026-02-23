package com.example.taybudget.data.model;

import com.example.taybudget.enums.CategoryEnum;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Aggregate implements Serializable {

    private String user;
    private double balance;
    private double income;
    private Map<String, LimitedAmount> categoricalUsage;

    public Aggregate() {
    }

    public Aggregate(String user, double balance, double income) {
        this.user = user;
        this.balance = balance;
        this.income = income;
        this.categoricalUsage = new HashMap<>();
        for (CategoryEnum category : CategoryEnum.values()) {
            this.updateCategoricalUsage(category.getName(), new LimitedAmount(0, 0));
        }
    }

    public Map<String, LimitedAmount> getCategoricalUsage() {
        return categoricalUsage;
    }

    public void setCategoricalUsage(Map<String, LimitedAmount> categoricalUsage) {
        this.categoricalUsage = categoricalUsage;
    }

    public void updateCategoricalUsage(String name, LimitedAmount amount) {
        this.categoricalUsage.put(name, amount);
    }

    public void updateCategoricalUsageCurrent(String name, double amount) {
        LimitedAmount limitedAmount = this.categoricalUsage.getOrDefault(name, new LimitedAmount(0, 0));
        limitedAmount.setCurrent(limitedAmount.getCurrent() + amount);
        this.categoricalUsage.put(name, limitedAmount);
    }

    public void updateCategoricalUsageLimit(String name, double amount) {
        LimitedAmount limitedAmount = this.categoricalUsage.getOrDefault(name, new LimitedAmount(0, 0));
        limitedAmount.setLimit(limitedAmount.getLimit() + amount);
        this.categoricalUsage.put(name, limitedAmount);
    }

    public LimitedAmount getAmountFromCategoricalUsage(CategoryEnum category) {
        return this.categoricalUsage.get(category.getName());
    }

    public double getBalanceFromCategoricalUsage(CategoryEnum category) {
        return this.categoricalUsage.get(category.getName()).getLimit() - this.categoricalUsage.get(category.getName()).getCurrent();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void updateBalanceExpense(double amount) {
        this.balance = this.balance - amount;
    }

    public void updateBalanceIncome(double amount) {
        this.balance = this.balance + amount;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }
}
