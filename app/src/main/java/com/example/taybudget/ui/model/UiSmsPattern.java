package com.example.taybudget.ui.model;

import com.example.taybudget.R;
import com.example.taybudget.data.model.CreditCycle;
import com.example.taybudget.data.model.SmsPattern;

import java.util.Date;

public class UiSmsPattern extends SmsPattern {
    int backgroundColour;

    public UiSmsPattern() {
        super();
        backgroundColour = android.R.color.background_light;
    }

    public UiSmsPattern(SmsPattern smsPattern) {
        this.setTag(smsPattern.getTag());
        this.setPatterns(smsPattern.getPatterns());
        this.setCreditCycle(smsPattern.getCreditCycle());
        this.setBackgroundColourDefault();
    }

    public int getBackgroundColour() {
        return backgroundColour;
    }

    public void setBackgroundColourDefault() {
        this.backgroundColour = android.R.color.background_light;
    }

    public void setBackgroundColourGreen() {
        this.backgroundColour = android.R.color.holo_green_light;
    }

    public void setBackgroundColourRed() {
        this.backgroundColour = android.R.color.holo_red_light;
    }

    public void setBackgroundColourYellow() {
        this.backgroundColour = R.color.yellow_light;
    }

    public CreditCycle getCurrentCreditCycleFor(Date date) {
        return CreditCycle.currentCreditCycle(date, this.getCreditCycle());
    }

    public CreditCycle getPreviousCreditCycleFor(Date date) {
        return CreditCycle.previousCreditCycle(date, this.getCreditCycle());
    }
}
