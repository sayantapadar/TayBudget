package com.example.taybudget.ui.model;

import android.util.Log;

import com.example.taybudget.data.handler.builders.html.HtmlListBuilder;
import com.example.taybudget.data.handler.builders.html.HtmlTableBuilder;
import com.example.taybudget.data.model.LimitedAmount;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.enums.CategoryEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UiGoal {
    private final String name;
    private final String html;

    public UiGoal(MonthlyAggregate monthlyAggregate, MonthlyAggregate updatedMonthlyAggregate) {
        this.name = monthlyAggregate.getName();
        this.html = populateMonthlyAggregate(monthlyAggregate, updatedMonthlyAggregate);
    }

    public UiGoal(String name, List<String> comments, MasterAggregate masterAggregate, MasterAggregate updatedMasterAggregate) {
        this.name = name;
        if (comments == null || comments.size() == 0)
            this.html = populateMasterAggregate(masterAggregate, updatedMasterAggregate);
        else
            this.html = populateComments(comments) + "<br>" + populateMasterAggregate(masterAggregate, updatedMasterAggregate);
    }

    public UiGoal(String name, Map<String, Double> categoricalSavings) {
        this.name = name;
        this.html = populateTableFromCategoricalSavings(categoricalSavings);
    }

    private String populateTableFromCategoricalSavings(Map<String, Double> categoricalSavings) {
        if (categoricalSavings == null || categoricalSavings.isEmpty())
            return "";
        HtmlTableBuilder.Builder builder = new HtmlTableBuilder.Builder(2)
                .setHeading("Category", "Amount");
        categoricalSavings.forEach((category, amount) -> {
            builder.addRow(category, String.format(Locale.getDefault(), "%.2f", amount));
        });
        builder.addRow("Total", String.format(Locale.getDefault(), "%.2f", categoricalSavings.values().stream().reduce(Double::sum).get()));
        return builder.format();
    }

    private String populateMonthlyAggregate(MonthlyAggregate monthlyAggregate, MonthlyAggregate updatedMonthlyAggregate) {
        HtmlTableBuilder.Builder builder = new HtmlTableBuilder.Builder(4)
                .setHeading("Category", "Previous Limit", "New Limit", "Reduction");
        for (CategoryEnum category : CategoryEnum.values()) {
            builder.addRow(category.getName(),
                    String.format(Locale.getDefault(), "%.2f", monthlyAggregate.getCategoricalUsage().get(category.getName()).getLimit()),
                    String.format(Locale.getDefault(), "%.2f", updatedMonthlyAggregate.getCategoricalUsage().get(category.getName()).getLimit()),
                    String.format(Locale.getDefault(), "%.2f", monthlyAggregate.getCategoricalUsage().get(category.getName()).getLimit()
                            - updatedMonthlyAggregate.getCategoricalUsage().get(category.getName()).getLimit()));
        }
        return builder.format();
    }

    private String populateMasterAggregate(MasterAggregate masterAggregate, MasterAggregate updatedMasterAggregate) {
        HtmlTableBuilder.Builder builder = new HtmlTableBuilder.Builder(4)
                .setHeading("Category", "Previous", "New", "Reduction");
        builder.addRow("Balance",
                String.format(Locale.getDefault(), "%.2f", masterAggregate.getBalance()),
                String.format(Locale.getDefault(), "%.2f", updatedMasterAggregate.getBalance()),
                String.format(Locale.getDefault(), "%.2f", masterAggregate.getBalance() - updatedMasterAggregate.getBalance()));
        return builder.format();
    }

    private String populateComments(List<String> comments) {
        HtmlListBuilder.Builder builder = new HtmlListBuilder.Builder();
        comments.forEach(builder::addItem);
        return builder.format();
    }

    public String getName() {
        return name;
    }

    public String getHtml() {
        return html;
    }
}
