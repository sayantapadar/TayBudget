package com.example.taybudget.data.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class MonthlyAggregate extends Aggregate {
    private String name;
    private Date date;

    public MonthlyAggregate() {
    }

    public MonthlyAggregate(String user, String name, Date date, double balance, double income) {
        super(user, balance, income);
        this.name = name;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
