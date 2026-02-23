package com.example.taybudget.ui.model;

import com.example.taybudget.data.model.LimitedAmount;
import com.example.taybudget.enums.CategoryEnum;

public class UiGoalCategory {
    private final CategoryEnum category;
    private double weight;
    private double previousLimit;
    private double nowLimit;

    public UiGoalCategory(CategoryEnum category, double weight) {
        this.category = category;
        this.weight = weight;
        this.previousLimit = 0;
        this.nowLimit = 0;
    }

    public UiGoalCategory(CategoryEnum category, double weight, double previousLimit, double nowLimit) {
        this.category = category;
        this.weight = weight;
        this.previousLimit = previousLimit;
        this.nowLimit = nowLimit;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getPreviousLimit() {
        return previousLimit;
    }

    public void setPreviousLimit(double previousLimit) {
        this.previousLimit = previousLimit;
    }

    public double getNowLimit() {
        return nowLimit;
    }

    public void setNowLimit(double nowLimit) {
        this.nowLimit = nowLimit;
    }
}
