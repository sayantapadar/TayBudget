package com.example.taybudget.enums;

import com.example.taybudget.tools.CommonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public enum TrackerTypeEnum {
    ONE_TIME("One Time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    ANNUALLY("Annually");

    private String name;

    TrackerTypeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static TrackerTypeEnum getByName(String name) {
        for (TrackerTypeEnum type : TrackerTypeEnum.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    public static TrackerTypeEnum getDefault() {
        return DAILY;
    }

    public static List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (TrackerTypeEnum type : TrackerTypeEnum.values()) {
            names.add(type.getName());
        }
        return names;
    }

    public int getPeriodCount(Date fromDate, Date toDate) {
        switch (this) {
            case ONE_TIME:
                return 1;
            case DAILY:
                return CommonUtils.getDaysBetween(fromDate, toDate) + 1;
            case WEEKLY:
                return (CommonUtils.getDaysBetween(fromDate, toDate) + 1) / 7;
            case MONTHLY:
                return CommonUtils.getMonthsBetween(fromDate, toDate) + 1;
            case QUARTERLY:
                return (CommonUtils.getMonthsBetween(fromDate, toDate) + 1) / 3;
            case ANNUALLY:
                return CommonUtils.getYearsBetween(fromDate, toDate) + 1;
            default:
                return 0;
        }
    }
}
