package com.example.taybudget.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Expense extends Data {
    private List<String> tags;
    private String category;
    private int smsId;
    private final List<String> trackers = new ArrayList<>();
    private int trackerCount = 0;

    public Expense() {
        super();
        this.tags = new ArrayList<>();
    }

    public Expense(String user, String category, String name, Date date, double amount, boolean recurring, Recurring recur) {
        super(user, name, date, recurring, recur, amount);
        this.tags = new ArrayList<>();
        this.category = category;
    }

    public List<String> getTags() {
        if (tags == null)
            tags = new ArrayList<>();
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getSmsId() {
        return smsId;
    }

    public void setSmsId(int smsId) {
        this.smsId = smsId;
    }

    public void addTracker(String tracker) {
        this.trackers.add(tracker);
        this.trackerCount = this.trackers.size();
    }

    public void addTrackers(List<String> trackers) {
        this.trackers.addAll(trackers);
        this.trackerCount = this.trackers.size();
    }

    public void setTrackers(List<String> trackers) {
        this.trackers.clear();
        this.trackers.addAll(trackers);
        this.trackerCount = this.trackers.size();
    }

    public List<String> getTrackers() {
        return this.trackers;
    }

    public int getTrackerCount() {
        return trackerCount;
    }
}
