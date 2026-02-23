package com.example.taybudget.enums;

public enum AggregateType {
    MASTER("Master"),
    MONTH("Month");

    private final String name;

    AggregateType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
