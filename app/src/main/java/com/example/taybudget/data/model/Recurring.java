package com.example.taybudget.data.model;

import java.io.Serializable;
import java.util.Date;

public class Recurring implements Serializable {
    private Date from;
    private Date to;
    private int period;

    public Recurring() {
    }

    public Recurring(Date from, Date to, int period) {
        this.from = from;
        this.to = to;
        this.period = period;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
