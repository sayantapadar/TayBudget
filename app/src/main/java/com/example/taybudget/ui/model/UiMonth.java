package com.example.taybudget.ui.model;

import java.util.Locale;

public class UiMonth {
    private String name;
    private double progress;
    private double current;
    private double limit;

    public UiMonth(String name, double current, double limit) {
        this.name = name;
        this.current = current;
        this.limit = limit;
        this.progress = limit == 0 ? 0 : current / limit;
    }

    public UiMonth() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getCurrent() {
        return String.format(Locale.getDefault(), "%.2f", current);
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public String getLimit() {
        return String.format(Locale.getDefault(), "%.2f", limit);
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }
}
