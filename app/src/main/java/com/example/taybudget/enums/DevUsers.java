package com.example.taybudget.enums;

public enum DevUsers {
    TAY("tay4");

    private String id;

    DevUsers(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
