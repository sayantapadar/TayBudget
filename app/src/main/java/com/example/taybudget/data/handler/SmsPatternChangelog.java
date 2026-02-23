package com.example.taybudget.data.handler;

import com.example.taybudget.data.model.SmsPattern;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SmsPatternChangelog {
    private long timestamp;
    private List<SmsPattern> newPatterns;
    private Map<String, SmsPattern> removePatterns;
    private Map<String, SmsPattern> updatePatterns;

    public SmsPatternChangelog(List<SmsPattern> newPatterns, Map<String, SmsPattern> removePatterns, Map<String, SmsPattern> updatePatterns) {
        this.timestamp = Calendar.getInstance(Locale.getDefault()).getTimeInMillis();
        this.newPatterns = newPatterns;
        this.removePatterns = removePatterns;
        this.updatePatterns = updatePatterns;
    }

    public SmsPatternChangelog() {
    }

    public List<SmsPattern> getNewPatterns() {
        return newPatterns;
    }

    public void setNewPatterns(List<SmsPattern> newPatterns) {
        this.newPatterns = newPatterns;
    }

    public Map<String, SmsPattern> getRemovePatterns() {
        return removePatterns;
    }

    public void setRemovePatterns(Map<String, SmsPattern> removePatterns) {
        this.removePatterns = removePatterns;
    }

    public Map<String, SmsPattern> getUpdatePatterns() {
        return updatePatterns;
    }

    public void setUpdatePatterns(Map<String, SmsPattern> updatePatterns) {
        this.updatePatterns = updatePatterns;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
