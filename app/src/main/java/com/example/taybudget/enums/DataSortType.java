package com.example.taybudget.enums;

import com.example.taybudget.ui.model.UiData;

import java.util.Comparator;
import java.util.Locale;

public enum DataSortType {
    LAST_UPDATED("Last Updated", Comparator.comparing(uiData -> ((UiData) uiData).getData().getInsertDate() != null
            ? ((UiData) uiData).getData().getInsertDate() : ((UiData) uiData).getData().getDate()).reversed()),
    ALPHABETICAL("Alphabetical", Comparator.comparing(uiData -> uiData.getData().getName().toLowerCase(Locale.ROOT))),
    DATE("Date", Comparator.comparing(uiData -> uiData.getData().getDate())),
    DATE_REVERSED("Date Reversed", Comparator.comparing(uiData -> ((UiData) uiData).getData().getDate()).reversed());


    private final String name;
    private final Comparator<? super UiData> comparator;

    DataSortType(String name, Comparator<? super UiData> comparator) {
        this.name = name;
        this.comparator = comparator;
    }

    public String getName() {
        return name;
    }

    public Comparator<? super UiData> getComparator() {
        return comparator;
    }
}
