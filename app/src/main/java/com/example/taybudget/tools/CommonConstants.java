package com.example.taybudget.tools;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public class CommonConstants {
    public static String[] CURRENCY_INDICATORS = new String[]{"INR", "Rs", "rs", "inr", "Inr"};
    public static final Pattern SMS_VALUE_PATTERN = Pattern.compile("(" + String.join("|", CURRENCY_INDICATORS) + ").? *[0-9,]+(.?)[0-9]*");
    public static final Pattern SMS_NAME_CRD_PATTERN = Pattern.compile("\\b[a|A][t|T] (.*)([o|O][n|N] +|[A|a][v|V][l|L])");
    public static final Pattern SMS_NAME_CRD_ICICI_PATTERN = Pattern.compile("\\b\\d+ [o|O][n|N] (.*) [a|A][v|V][l|L]");
    public static final Pattern SMS_NAME_UPI_ICICI_PATTERN = Pattern.compile(";(.*)[c|C][r|R][e|E][d|D][i|I][t|T][e|E][d|D]");
    public static final Pattern SMS_NAME_UPI_HDFC_PATTERN = Pattern.compile("\\b[T|t][o|O] (.*)([o|O][n|N] +|[A|a][v|V][l|L])");
    public static SimpleDateFormat DEFAULT_DATE_DISPLAY;
    public static SimpleDateFormat DEFAULT_DAY_ONLY_DISPLAY;

    public static String SHARED_PREFERENCES = "AppSharedPreferences";
    public static String SHARED_PREFERENCES_SMS = "StoredSms";
    public static String SHARED_PREFERENCES_SMS_LAST_READ = "SmsLastRead";
    public static String SHARED_PREFERENCES_PATTERNS_LAST_UPDATED = "PatternsLastUpdated";
    public static final String TEXT_OLDER = "Older Messages...";
    public static final String DAILY_BUDGET_TRACKER = "Daily Budget";

    public static void init() {
        DEFAULT_DATE_DISPLAY = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        DEFAULT_DAY_ONLY_DISPLAY = new SimpleDateFormat("dd", Locale.getDefault());
    }
}
