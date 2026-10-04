package com.example.taybudget.tools;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.example.taybudget.R;
import com.example.taybudget.data.handler.model.Tracker;
import com.example.taybudget.data.model.Aggregate;
import com.example.taybudget.data.model.Data;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Income;
import com.example.taybudget.data.model.LimitedAmount;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.data.model.Recurring;
import com.example.taybudget.data.model.Sms;
import com.example.taybudget.enums.AggregateType;
import com.example.taybudget.enums.CategoryEnum;
import com.example.taybudget.enums.DataType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {
    public static double getCategoryRatio(CategoryEnum category, Map<String, LimitedAmount> categoricalUsage) {
        if (categoricalUsage.get(category.getName()) == null || getCategoryLimit(category, categoricalUsage) == 0)
            return 0;
        return getCategoryCurrent(category, categoricalUsage) / getCategoryLimit(category, categoricalUsage);
    }

    public static double getCategoryOffsetRatio(CategoryEnum category, Map<String, LimitedAmount> categoricalUsage, double offset) {
        if (categoricalUsage.get(category.getName()) == null || getCategoryLimit(category, categoricalUsage) == 0)
            return 0;
        return (getCategoryCurrent(category, categoricalUsage) + offset) / getCategoryLimit(category, categoricalUsage);
    }

    public static double getCategoryCurrent(CategoryEnum category, Map<String, LimitedAmount> categoricalUsage) {
        if (categoricalUsage.get(category.getName()) == null)
            return 0;
        return categoricalUsage.get(category.getName()).getCurrent();
    }

    public static double getCategoryLimit(CategoryEnum category, Map<String, LimitedAmount> categoricalUsage) {
        if (categoricalUsage.get(category.getName()) == null)
            return 0;
        return categoricalUsage.get(category.getName()).getLimit();
    }

    public static int getHealthColor(Map<String, LimitedAmount> categoricalUsage, double balance) {
        int maxGrade = 0;
        for (CategoryEnum category : CategoryEnum.values()) {
            int grade = category.getGradeFromRatio(getCategoryRatio(category, categoricalUsage));
            if (grade > maxGrade)
                maxGrade = grade;
        }

        if (maxGrade == 0)
            return android.R.color.holo_green_dark;
        else if (maxGrade == 1)
            return R.color.yellow;
        else
            return android.R.color.holo_red_dark;
    }

    public static boolean validateRecurringData(Recurring recur, Date date) {
        date = getEndOfMonth(date);
        return date.compareTo(getEndOfMonth(recur.getTo())) <= 0 // if "date end of month" <= recur.To - expiry condition
                && date.compareTo(recur.getFrom()) >= 0 // if "date" >= recur.From - start condition
                && getMonthsBetween(recur.getFrom(), date) % recur.getPeriod() == 0;  // if "date" is impacted by recur
    }

    public static int getValidRecurringMonthsBetween(Recurring recur, Date fromDate, Date toDate) {
        int months = getMonthsBetween(fromDate, toDate);
        int validMonths = 0;
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(fromDate);
        for (int i = 0; i <= months; i++) {
            if (validateRecurringData(recur, calendar.getTime()))
                validMonths++;
            calendar.add(Calendar.MONTH, 1);
        }
        return validMonths;
    }

    public static boolean validateExpenseInTracker(Tracker tracker, Expense expense) {
        if (expense.isRecurring()) {
            return getValidRecurringMonthsBetween(expense.getRecur(), tracker.getDate(), expense.getRecur().getTo()) > 0;
        } else {
            return expense.getDate().compareTo(tracker.getDate()) >= 0 && expense.getDate().compareTo(tracker.getStopDate()) <= 0;
        }
    }

    public static Date getEndOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static int getMonthsBetween(Date start, Date end) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(start);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Calendar calendar1 = Calendar.getInstance(Locale.getDefault());
        calendar1.setTime(end);
        calendar1.set(Calendar.DAY_OF_MONTH, 1);
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);

        return (calendar1.get(Calendar.YEAR) - calendar.get(Calendar.YEAR)) * 12
                + (calendar1.get(Calendar.MONTH) - calendar.get(Calendar.MONTH));
    }

    public static int getDaysBetween(Date start, Date end) {
        Calendar s = Calendar.getInstance(Locale.getDefault());
        s.setTime(start);
        s.set(Calendar.HOUR_OF_DAY, 0);
        s.set(Calendar.MINUTE, 0);
        s.set(Calendar.SECOND, 0);
        s.set(Calendar.MILLISECOND, 0);

        Calendar e = Calendar.getInstance(Locale.getDefault());
        e.setTime(end);
        e.set(Calendar.HOUR_OF_DAY, 0);
        e.set(Calendar.MINUTE, 0);
        e.set(Calendar.SECOND, 0);
        e.set(Calendar.MILLISECOND, 0);

        return (int) ((e.getTimeInMillis() - s.getTimeInMillis()) / 86400000);
    }

    public static int getYearsBetween(Date start, Date end) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(start);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Calendar calendar1 = Calendar.getInstance(Locale.getDefault());
        calendar1.setTime(end);
        calendar1.set(Calendar.DAY_OF_MONTH, 1);
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);

        return calendar1.get(Calendar.YEAR) - calendar.get(Calendar.YEAR);
    }

    public static void updateAggregate(Aggregate aggregate, Data data, AggregateType aggregateType, DataType dataType, boolean remove) {
        double amount = data.getAmount();
        if (remove)
            amount = -amount;
        if (aggregateType == AggregateType.MASTER && dataType == DataType.INCOME) {
            MasterAggregate masterAggregate = (MasterAggregate) aggregate;
            Income income = (Income) data;
            masterAggregate.updateBalanceIncome(amount);
            for (CategoryEnum category : CategoryEnum.values()) {
                masterAggregate.updateCategoricalUsageLimit(category.getName(), amount * category.getPercentage() / 100);
            }
            if (income.isTaxable())
                masterAggregate.setTaxLiableIncome(masterAggregate.getTaxLiableIncome() + amount);
            masterAggregate.setIncome(masterAggregate.getIncome() + amount);
        } else if (aggregateType == AggregateType.MASTER && dataType == DataType.EXPENSE) {
            MasterAggregate masterAggregate = (MasterAggregate) aggregate;
            Expense expense = (Expense) data;
            masterAggregate.updateCategoricalUsageCurrent(expense.getCategory(), amount);
            masterAggregate.updateBalanceExpense(amount);
        } else if (aggregateType == AggregateType.MONTH && dataType == DataType.INCOME) {
            MonthlyAggregate monthlyAggregate = (MonthlyAggregate) aggregate;
            Income income = (Income) data;
            monthlyAggregate.updateBalanceIncome(amount);
            monthlyAggregate.setIncome(monthlyAggregate.getIncome() + amount);
            for (CategoryEnum category : CategoryEnum.values()) {
                monthlyAggregate.updateCategoricalUsageLimit(category.getName(), amount * category.getPercentage() / 100);
            }
        } else if (aggregateType == AggregateType.MONTH && dataType == DataType.EXPENSE) {
            MonthlyAggregate monthlyAggregate = (MonthlyAggregate) aggregate;
            Expense expense = (Expense) data;
            monthlyAggregate.updateCategoricalUsageCurrent(expense.getCategory(), amount);
            monthlyAggregate.updateBalanceExpense(amount);
        }
    }

    public static void scaleWeights(Map<CategoryEnum, Double> weights, int scaleTo) {
        double totalWeight = weights.values().stream().reduce(Double::sum).get();
        double multiplier = scaleTo / totalWeight;
        weights.replaceAll((category, weight) -> weight * multiplier);
    }

    public static double roundUp(double number, int roundUpTo) {
        // 32215.251 -> 32300 | 32215.251 -> 32215 -> 322 (/rut) -> 323 (+1) -> 32300 (*rut)
        if (number == 0)
            return 0;
        return (double) ((((int) number / roundUpTo) + 1) * roundUpTo);
    }

    public static <E> E deepCopy(E object, Class<E> eClass) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(object), eClass);
    }

    public static void extractValueFromSms(Sms sms) {
        Matcher m = CommonConstants.SMS_VALUE_PATTERN.matcher(sms.getText());
        if (m.find()) {
            String match = m.group();
            StringBuilder val = new StringBuilder();
            for (char ch : match.toCharArray()) {
                if ((ch >= '0' && ch <= '9') || ch == '.') {
                    if (!(val.toString().equals("") && ch == '.'))
                        val.append(ch);
                }
            }
            try {
                sms.setExtractedAmount(Double.parseDouble(val.toString()));
            } catch (Exception ignored) {
            }
        }
    }

    public static void extractNameFromSms(Sms sms) {
        Pattern[] patterns;
        if (sms.getTag() != null && sms.getTag().equalsIgnoreCase("HDFC UPI")) {
            patterns = new Pattern[]{CommonConstants.SMS_NAME_UPI_HDFC_PATTERN};
        } else if (sms.getTag() != null && sms.getTag().equalsIgnoreCase("ICICI UPI")) {
            patterns = new Pattern[]{CommonConstants.SMS_NAME_UPI_ICICI_PATTERN};
        } else {
            patterns = new Pattern[]{CommonConstants.SMS_NAME_CRD_PATTERN, CommonConstants.SMS_NAME_CRD_ICICI_PATTERN,
                    CommonConstants.SMS_NAME_UPI_ICICI_PATTERN, CommonConstants.SMS_NAME_UPI_HDFC_PATTERN};
        }
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(sms.getText());
            if (m.find()) {
                String match = m.group(1);
                if (match != null && !match.equals("")) {
                    match = match.replace('.', ' ');
                    match = match.trim();
                    String finalMatch = match;
                    if (Arrays.stream(CommonConstants.CURRENCY_INDICATORS).anyMatch(finalMatch::contains) && (match.contains("for") || match.contains("For") || match.contains("FOR")))
                        match = match.split("[f|F][O|o][R|r]")[0];
                    sms.setExtractedName(match);
                    break;
                }
            }
        }
    }

    public static Date parseDate(String date, SimpleDateFormat simpleDateFormat) {
        try {
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Date getNextMilliSecond(Date date) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        calendar.add(Calendar.MILLISECOND, 1);
        return calendar.getTime();
    }

    public static Date getPreviousMilliSecond(Date date) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTime();
    }

    public static Date getNextMonth(Date date) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTime();
    }

    public static Date getInfiniteDate() {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(Calendar.YEAR, 9998);
        calendar.set(Calendar.MONTH, 12);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        return calendar.getTime();

    }

    public static Date getPreviousMonth(Date date) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, -1);
        return calendar.getTime();
    }

    public static void setDatePicker(Context context, EditText editText, Date defaultTime) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(defaultTime);
        editText.setKeyListener(null);
        editText.setInputType(InputType.TYPE_NULL);
        editText.setFocusable(false);
        editText.setText(CommonConstants.DEFAULT_DATE_DISPLAY.format(defaultTime));
        editText.setOnClickListener(v1 -> {
            Log.d("CommonUtils", "Clicked date field");
            DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                    (view1, year, monthOfYear, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        editText.setText(CommonConstants.DEFAULT_DATE_DISPLAY.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    public static void setDropdownItems(Context context, AutoCompleteTextView textView, List<String> items) {
        textView.setKeyListener(null);
        textView.setInputType(InputType.TYPE_NULL);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, items);
        textView.setAdapter(adapter);
        textView.setText(adapter.getItem(0), false);
    }

    public static void showDocumentationAlert(Context context, String title, List<String> items) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(String.join("\n", items))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create().show();
    }

    public static Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
                    // Define all possible formats here
                    String[] formats = new String[] {
                            // 1. Month-first formats (Common in US/India)
                            "MMM d, yyyy HH:mm:ss",    // Sep 5, 2026 18:48:59
                            "MMM dd, yyyy HH:mm:ss",   // Sep 05, 2026 18:48:59
                            "MMM d, yyyy h:mm:ss a",   // Sep 5, 2026 6:48:59 PM
                            "MMM d, yyyy",             // Sep 5, 2026

                            // 2. Day-first formats (Common in UK/Europe)
                            "d MMM, yyyy HH:mm:ss",    // 5 Sep, 2026 18:48:59
                            "dd MMM, yyyy HH:mm:ss",   // 05 Sep, 2026 18:48:59
                            "d MMM, yyyy",             // 5 Sep, 2026
                            "dd-MM-yyyy HH:mm:ss",     // 05-09-2026 18:48:59
                            "dd/MM/yyyy",              // 05/09/2026

                            // 3. ISO 8601 & Technical formats
                            "yyyy-MM-dd'T'HH:mm:ss.SSSZ", // 2026-09-05T18:48:59.000+0000
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",   // 2026-09-05T18:48:59Z
                            "yyyy-MM-dd HH:mm:ss",        // 2026-09-05 18:48:59
                            "yyyy-MM-dd",                 // 2026-09-05

                            // 4. Gson Default variations
                            "MMM d, yyyy",                // Oct 4, 2026 (Gson default)
                    };

                    for (String format : formats) {
                        try {
                            return new SimpleDateFormat(format, Locale.US).parse(json.getAsString());
                        } catch (ParseException ignored) {
                            // Try the next format
                        }
                    }

                    // Fallback: Try long timestamp or throw error
                    try {
                        return new Date(json.getAsLong());
                    } catch (Exception e) {
                        throw new JsonParseException("Unparseable date: \"" + json.getAsString() + "\". Supported formats: " + Arrays.toString(formats));
                    }
                })
                .create();
    }
}
