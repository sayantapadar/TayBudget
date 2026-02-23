package com.example.taybudget.data.handler.builders.html;

import com.example.taybudget.data.handler.builders.exception.HtmlTableBuilderException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HtmlTableBuilder {
    private final int columns;
    private final List<String> heading;
    private final List<List<String>> rows;

    private HtmlTableBuilder(int columns, List<String> heading, List<List<String>> rows) {
        this.columns = columns;
        this.heading = heading;
        this.rows = rows;
    }

    private String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"border: 1px solid black; font-size: 13px\">");
        if (heading != null && heading.size() != 0) {
            sb.append("<tr style=\"border: 1px solid black; font-size: 13px\">");
            heading.forEach(head -> {
                sb.append("<th style=\"border: 1px solid black; font-size: 13px; background-color: #96D4D4;\">");
                sb.append(head);
                sb.append("</th>");
            });
            sb.append("</tr>");
        }
        if (rows != null && rows.size() != 0) {
            rows.forEach(row -> {
                sb.append("<tr style=\"border: 1px solid black; font-size: 13px\">");
                row.forEach(val -> {
                    sb.append("<th style=\"border: 1px solid black; font-size: 13px\">");
                    sb.append(val);
                    sb.append("</th>");
                });
                sb.append("</tr>");
            });
        }
        sb.append("</table>");
        return sb.toString();
    }

    public static class Builder {
        private final int columns;
        private final List<String> heading;
        private final List<List<String>> rows;

        public Builder(int columns) {
            this.columns = columns;
            heading = new ArrayList<>();
            rows = new ArrayList<>();
        }

        public Builder setHeading(String... headingValues) {
            if (headingValues.length != columns)
                throw new HtmlTableBuilderException("Number of columns specified is " + columns + ", but parameters contain "
                        + headingValues.length + " columns");
            Collections.addAll(heading, headingValues);
            return this;
        }

        public Builder addRow(String... rowValues) {
            if (rowValues.length != columns)
                throw new HtmlTableBuilderException("Number of columns specified is " + columns + ", but parameters contain "
                        + rowValues.length + " columns");
            rows.add(Arrays.stream(rowValues).collect(Collectors.toList()));
            return this;
        }

        public String format() {
            return new HtmlTableBuilder(columns, heading, rows).format();
        }
    }
}
