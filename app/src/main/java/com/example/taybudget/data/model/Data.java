package com.example.taybudget.data.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class Data implements Serializable {
    private String documentId;
    private String user;
    private String name;
    private Date date;
    private boolean recurring;
    private Recurring recur;
    private double amount;
    private Date insertDate;

    public Data() {
    }

    public Data(String user, String name, Date date, boolean recurring, Recurring recur, double amount) {
        this.user = user;
        this.name = name;
        this.date = date;
        this.recurring = recurring;
        this.recur = recur;
        this.amount = amount;
        this.insertDate = Calendar.getInstance().getTime();
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public Recurring getRecur() {
        return recur;
    }

    public void setRecur(Recurring recur) {
        this.recur = recur;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(Date insertDate) {
        this.insertDate = insertDate;
    }
}
