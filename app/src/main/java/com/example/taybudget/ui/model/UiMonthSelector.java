package com.example.taybudget.ui.model;

import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.tools.CommonUtils;

public class UiMonthSelector {
    private int healthColor;
    private MonthlyAggregate monthlyAggregate;

    public UiMonthSelector(MonthlyAggregate monthlyAggregate) {
        this.monthlyAggregate = monthlyAggregate;
        this.healthColor = CommonUtils.getHealthColor(monthlyAggregate.getCategoricalUsage(), monthlyAggregate.getBalance());
    }

    public int getHealthColor() {
        return healthColor;
    }

    public void setHealthColor(int healthColor) {
        this.healthColor = healthColor;
    }

    public MonthlyAggregate getMonthlyAggregate() {
        return monthlyAggregate;
    }

    public void setMonthlyAggregate(MonthlyAggregate monthlyAggregate) {
        this.monthlyAggregate = monthlyAggregate;
    }
}
