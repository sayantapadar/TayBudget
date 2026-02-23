package com.example.taybudget.data.model;

import com.example.taybudget.data.handler.model.GoalCalculation;
import com.example.taybudget.enums.CategoryEnum;
import com.example.taybudget.ui.model.UiGoal;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Goal implements Serializable {
    private String id;
    private String user;
    private String name;
    private double amount;
    private Date fromDate;
    private Date toDate;
    private Map<String, Double> weights;
    private Map<String, Map<String, Double>> savings;
    private double masterBalanceUsed;
    private GoalCalculation calculation;

    public Goal() {
    }

    public Goal(String user, String name, GoalCalculation calculation) {
        this.user = user;
        this.name = name;
        this.amount = calculation.getAmount();

        this.weights = calculation.getWeights();
        this.calculation = calculation;
        this.savings = new HashMap<>();

        if (calculation.getMonthlyAggregates() != null && calculation.getUpdatedMonthlyAggregates().size() > 0) {
            this.fromDate = calculation.getMonthlyAggregates().get(0).getDate();
            this.toDate = calculation.getMonthlyAggregates().get(calculation.getMonthlyAggregates().size() - 1).getDate();
            for (int i = 0; i < calculation.getMonthlyAggregates().size(); i++) {
                Map<String, Double> sav = new HashMap<>();
                MonthlyAggregate aggregate = calculation.getMonthlyAggregates().get(i);
                MonthlyAggregate updatedAggregate = calculation.getUpdatedMonthlyAggregates().get(i);
                for (CategoryEnum category : CategoryEnum.values()) {
                    sav.put(category.getName(), aggregate.getAmountFromCategoricalUsage(category).getLimit() - updatedAggregate.getAmountFromCategoricalUsage(category).getLimit());
                }
                this.savings.put(aggregate.getName(), sav);
            }
            this.masterBalanceUsed = calculation.getMasterAggregate().getBalance();
        } else {
            this.fromDate = calculation.getDate();
            this.toDate = calculation.getDate();
            this.masterBalanceUsed = calculation.getAmount();
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
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

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Map<String, Double> getWeights() {
        return weights;
    }

    public void setWeights(Map<String, Double> weights) {
        this.weights = weights;
    }

    public Map<String, Map<String, Double>> getSavings() {
        return savings;
    }

    public void setSavings(Map<String, Map<String, Double>> savings) {
        this.savings = savings;
    }

    public GoalCalculation getCalculation() {
        return calculation;
    }

    public void setCalculation(GoalCalculation calculation) {
        this.calculation = calculation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getMasterBalanceUsed() {
        return masterBalanceUsed;
    }
}
