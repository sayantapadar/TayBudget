package com.example.taybudget.data.handler.model;

import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.enums.CategoryEnum;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalCalculation {
    private List<MonthlyAggregate> monthlyAggregates;
    private List<MonthlyAggregate> updatedMonthlyAggregates;
    private MasterAggregate masterAggregate;
    private MasterAggregate updatedMasterAggregate;
    private List<String> comments;
    private boolean success;
    private double amount;
    private double expectedSaving;
    private Date date;
    private Map<String, Double> weights;

    public GoalCalculation() {
    }

    public GoalCalculation(List<MonthlyAggregate> monthlyAggregates, MasterAggregate masterAggregate, double amount, Date date) {
        this.monthlyAggregates = monthlyAggregates;
        this.masterAggregate = masterAggregate;
        this.comments = new ArrayList<>();
        this.amount = amount;
        this.date = date;
        this.weights = new HashMap<>();
    }

    public void addComment(String comment) {
        this.comments.add(comment);
    }

    public List<MonthlyAggregate> getMonthlyAggregates() {
        return monthlyAggregates;
    }

    public MasterAggregate getMasterAggregate() {
        return masterAggregate;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setMonthlyAggregates(List<MonthlyAggregate> monthlyAggregates) {
        this.monthlyAggregates = monthlyAggregates;
    }

    public void setMasterAggregate(MasterAggregate masterAggregate) {
        this.masterAggregate = masterAggregate;
    }

    public List<MonthlyAggregate> getUpdatedMonthlyAggregates() {
        return updatedMonthlyAggregates;
    }

    public void copyMonthlyAggregates() {
        if (monthlyAggregates == null)
            updatedMonthlyAggregates = null;
        else if (monthlyAggregates.size() == 0)
            updatedMonthlyAggregates = new ArrayList<>();
        else {
            Gson gson = new Gson();
            updatedMonthlyAggregates = new ArrayList<>();
            monthlyAggregates.forEach(agg -> updatedMonthlyAggregates.add(gson.fromJson(gson.toJson(agg), MonthlyAggregate.class)));
        }
    }

    public MasterAggregate getUpdatedMasterAggregate() {
        return updatedMasterAggregate;
    }

    public void setUpdatedMasterAggregate(MasterAggregate updatedMasterAggregate) {
        this.updatedMasterAggregate = updatedMasterAggregate;
    }

    public void copyMasterAggregate() {
        if (masterAggregate == null)
            updatedMasterAggregate = null;
        else {
            Gson gson = new Gson();
            updatedMasterAggregate = gson.fromJson(gson.toJson(masterAggregate), MasterAggregate.class);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getExpectedSaving() {
        return expectedSaving;
    }

    public void setExpectedSaving(double expectedSaving) {
        this.expectedSaving = expectedSaving;
    }

    public Date getDate() {
        return date;
    }

    public Map<String, Double> getWeights() {
        return weights;
    }

    public void setWeightsType(Map<CategoryEnum, Double> weights) {
        weights.forEach((category, weight) -> this.weights.put(category.getName(), weight));
    }
}
