package com.example.taybudget.data.model;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreditCycle {
    private Date start;
    private Date end;

    public CreditCycle() {
    }

    public CreditCycle(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public static CreditCycle currentCreditCycle(Date today, String creditCycle) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(today);
        int today_day = calendar.get(Calendar.DAY_OF_MONTH);
        CreditCycle cycle = new CreditCycle();
        int creditDay = Integer.parseInt(creditCycle);
        if (creditDay > today_day) {
            calendar.set(Calendar.DAY_OF_MONTH, creditDay);
            cycle.setEnd(calendar.getTime());
            calendar.add(Calendar.MONTH, -1);
            calendar.add(Calendar.DATE, 1);
            cycle.setStart(calendar.getTime());
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, creditDay);
            calendar.add(Calendar.DATE, 1);
            cycle.setStart(calendar.getTime());
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DATE, -1);
            cycle.setEnd(calendar.getTime());
        }
        return cycle;
    }

    public static CreditCycle previousCreditCycle(Date today, String creditCycle) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(today);
        calendar.set(Calendar.HOUR, 24);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        int today_day = calendar.get(Calendar.DAY_OF_MONTH);
        CreditCycle cycle = new CreditCycle();
        int creditDay = Integer.parseInt(creditCycle);
        if (creditDay > today_day) {
            calendar.set(Calendar.DAY_OF_MONTH, creditDay);
            calendar.add(Calendar.MONTH, -1);
            cycle.setEnd(calendar.getTime());
            calendar.add(Calendar.MONTH, -1);
            calendar.add(Calendar.DATE, 1);
            cycle.setStart(calendar.getTime());
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, creditDay);
            calendar.add(Calendar.DATE, 1);
            calendar.add(Calendar.MONTH, -1);
            cycle.setStart(calendar.getTime());
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DATE, -1);
            cycle.setEnd(calendar.getTime());
        }
        return cycle;
    }
}
