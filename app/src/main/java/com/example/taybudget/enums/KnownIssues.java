package com.example.taybudget.enums;

public enum KnownIssues {
    A("Cannot edit the date in income / expense, because that might break the logic"),
    B("You can edit the end date of a recurring income / expense, but not delete it. It might break the logic"),
    C("Take extra caution in adding historical recurring data. You cannot see it, and you cannot edit it");
    private final String description;

    KnownIssues(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
