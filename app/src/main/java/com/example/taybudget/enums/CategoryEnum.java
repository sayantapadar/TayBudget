package com.example.taybudget.enums;

import java.util.function.Function;

public enum CategoryEnum {
    NECESSITY("Necessity", 50, 0.90, 0.75),  // crossing .90 is red, crossing 0.75 is yellow
    LIFESTYLE("Lifestyle", 30, 0.95, 0.80),
    INVESTMENT("Investment", 10, false, 0.2, 0.5), // since higherIsBetter, below 0.2 is red, below 0.5 is yellow
    ;

    private final String name;
    private final int percentage;
    private final boolean lowerIsBetter;
    private final double[] ratioForGrades;

    CategoryEnum(String name, int percentage, double... ratioForGrade) {
        this.name = name;
        this.percentage = percentage;
        this.lowerIsBetter = true;
        this.ratioForGrades = ratioForGrade;
    }

    CategoryEnum(String name, int percentage, boolean lowerIsBetter, double... ratioForGrade) {
        this.name = name;
        this.percentage = percentage;
        this.lowerIsBetter = lowerIsBetter;
        this.ratioForGrades = ratioForGrade;
    }

    public String getName() {
        return name;
    }

    public int getPercentage() {
        return percentage;
    }

    public boolean isLowerIsBetter() {
        return lowerIsBetter;
    }

    public int getGradeFromRatio(double ratio) {
        if (!isLowerIsBetter()) {
            for (int i = 0; i < ratioForGrades.length; i++) {
                if (ratio > ratioForGrades[i])
                    return i;
            }
        } else {
            for (int i = 0; i < ratioForGrades.length; i++) {
                if (ratio < ratioForGrades[i])
                    return i;
            }
        }
        return ratioForGrades.length;
    }
}
