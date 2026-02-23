package com.example.taybudget.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SmsPattern implements Serializable {
    private String tag;
    private List<String> patterns;
    private String creditCycle;

    public SmsPattern() {
        patterns = new ArrayList<>();
    }

    public SmsPattern(String tag, List<String> patterns, String creditCycle) {
        this.tag = tag;
        this.patterns = patterns;
        this.creditCycle = creditCycle;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public String getCreditCycle() {
        if (creditCycle == null)
            return "";
        return creditCycle;
    }

    public void setCreditCycle(String creditCycle) {
        this.creditCycle = creditCycle;
    }
}
