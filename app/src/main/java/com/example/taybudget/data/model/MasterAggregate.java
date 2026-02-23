package com.example.taybudget.data.model;

import java.io.Serializable;
import java.util.Map;

public class MasterAggregate extends Aggregate {
    private double taxLiableIncome;

    public MasterAggregate() {
    }

    public MasterAggregate(String user, double balance, double income, double taxLiableIncome) {
        super(user, balance, income);
        this.taxLiableIncome = taxLiableIncome;
    }

    public double getTaxLiableIncome() {
        return taxLiableIncome;
    }

    public void setTaxLiableIncome(double taxLiableIncome) {
        this.taxLiableIncome = taxLiableIncome;
    }

}
