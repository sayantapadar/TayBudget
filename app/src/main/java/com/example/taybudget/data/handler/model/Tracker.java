package com.example.taybudget.data.handler.model;

import com.example.taybudget.enums.DevUsers;
import com.example.taybudget.tools.CommonUtils;

import java.util.Date;

public class Tracker {
    private String id;
    private String user;
    private String name;
    private Date date;
    private Date stopDate;
    private String renewPeriod;
    private double amount;

    public Tracker() {

    }

    public Tracker(String name) {
        this.name = name;
    }

    public Tracker(String name, Date date, String renewPeriod, double amount) {
        this.name = name;
        this.date = date;
        this.renewPeriod = renewPeriod;
        this.amount = amount;
        this.stopDate = CommonUtils.getInfiniteDate();
        this.user = DevUsers.TAY.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public void setStopDate(Date stopDate) {
        this.stopDate = stopDate;
    }

    public String getRenewPeriod() {
        return renewPeriod;
    }

    public void setRenewPeriod(String renewPeriod) {
        this.renewPeriod = renewPeriod;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
