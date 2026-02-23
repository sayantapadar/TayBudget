package com.example.taybudget.ui.model;

import com.example.taybudget.data.model.Data;

public class UiData {
    private String heading;
    private String message;
    private String footer;
    private Data data;


    public UiData(String heading, String message, String footer, Data data) {
        this.heading = heading;
        this.message = message;
        this.footer = footer;
        this.data = data;
    }

    public UiData() {
    }


    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }
}
