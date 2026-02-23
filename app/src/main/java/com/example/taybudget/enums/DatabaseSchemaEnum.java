package com.example.taybudget.enums;

public enum DatabaseSchemaEnum {
    INCOME("income"),
    EXPENSE("expense"),
    TAG("tag"),
    MONTHLY_AGGREGATE("monthly_aggregate"),
    MASTER_AGGREGATE("master_aggregate"),
    GOAL("goal"),
    SMS_PATTERNS("sms_patterns"),
    SMS_PATTERN_CHANGELOG("sms_patterns_changelog"),
    TRACKER("tracker"),
    TRACKER_SAVES("tracker_saves");

    private final String name;

    DatabaseSchemaEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
