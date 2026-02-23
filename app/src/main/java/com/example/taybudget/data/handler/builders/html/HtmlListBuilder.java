package com.example.taybudget.data.handler.builders.html;

import com.example.taybudget.data.handler.builders.exception.HtmlTableBuilderException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HtmlListBuilder {
    private final List<String> items;

    private HtmlListBuilder(List<String> items) {
        this.items = items;
    }

    private String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul style=\"font-size: 13px\">");
        if (items != null && items.size() != 0) {
            items.forEach(item -> {
                sb.append("<li style=\"font-size: 13px;\">");
                sb.append(item);
                sb.append("</li>");
            });
        }
        sb.append("</ul>");
        return sb.toString();
    }

    public static class Builder {
        private List<String> items;

        public Builder() {
            items = new ArrayList<>();
        }

        public Builder addItem(String item) {
            items.add(item);
            return this;
        }

        public String format() {
            return new HtmlListBuilder(items).format();
        }
    }
}
