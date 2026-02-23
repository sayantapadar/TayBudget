package com.example.taybudget.data.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

public class Sms implements Serializable {
    private String sender;
    private String text;
    private Date date;
    private double extractedAmount;
    private String extractedName;
    private String tag;
    private int id;
    private boolean highlight;
    private boolean flag;

    public Sms() {
    }

    public Sms(String sender, String text, Date date, int id) {
        this.sender = sender;
        this.text = text;
        this.date = date;
        this.id = id;
        this.highlight = false;
        this.flag = false;
    }

    public Sms(String sender, String text, Date date, int id, boolean highlight) {
        this.sender = sender;
        this.text = text;
        this.date = date;
        this.id = id;
        this.highlight = highlight;
        this.flag = false;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getText() {
        if (text.contains("\n"))
            text = text.replace("\n", " ");
        if (text.contains("\r"))
            text = text.replace("\r", " ");
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @NonNull
    @Override
    public String toString() {
        return date + " | " + extractedAmount + " | Sender: " + sender + " | text: " + text;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getExtractedAmount() {
        return extractedAmount;
    }

    public void setExtractedAmount(double extractedAmount) {
        this.extractedAmount = extractedAmount;
    }

    public String getExtractedName() {
        return extractedName;
    }

    public void setExtractedName(String extractedName) {
        this.extractedName = extractedName;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisplayTitle() {
        return (highlight? "*" : "") + String.format(Locale.getDefault(), "%.2f", extractedAmount) + " (" + tag + ")";
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
