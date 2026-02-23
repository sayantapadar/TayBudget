package com.example.taybudget.data.handler;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.taybudget.R;
import com.example.taybudget.activity.MainActivity;
import com.example.taybudget.data.firestore.ExpenseDAO;
import com.example.taybudget.data.firestore.TrackerDAO;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.model.BiParameterizedObject;
import com.example.taybudget.data.handler.model.Tracker;
import com.example.taybudget.data.model.Data;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Recurring;
import com.example.taybudget.enums.TrackerTypeEnum;
import com.example.taybudget.tools.CommonConsumers;
import com.example.taybudget.tools.CommonUtils;
import com.example.taybudget.tools.FirestoreProcessor;
import com.example.taybudget.tools.ThreadUtil;
import com.example.taybudget.ui.model.UiTracker;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TrackerHandler {
    private static volatile TrackerHandler INSTANCE = null;
    private final TrackerDAO trackerDAO;
    private final ExpenseDAO expenseDAO;
    private final HashMap<Tracker, BiParameterizedObject<List<Expense>, Double>> trackerMap;

    private TrackerHandler() {
        trackerDAO = new TrackerDAO();
        trackerMap = new HashMap<>();
        expenseDAO = new ExpenseDAO();
    }

    public static TrackerHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (TrackerHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TrackerHandler();
                }
            }
        }
        return INSTANCE;
    }

    public void addTracker(Tracker tracker, ResultSuccessCallback<Tracker> successCallback, ResultFailureCallback failureCallback) {
        ThreadUtil.startNewThread("getTrackers", () -> {
            List<Tracker> trackers = new FirestoreProcessor<Tracker>().getList(trackerDAO.getTrackersTask(), CommonConsumers.TRACKER_ID_SETTER,
                    failureCallback, Tracker.class);
            if (trackers.stream().anyMatch(tracker1 -> tracker1.getName().equals(tracker.getName())))
                failureCallback.onError("Tracker already exists", null);
            else
                trackerDAO.addTracker(tracker, successCallback, failureCallback);
        });
    }

    public void stopTracker(Tracker tracker, ResultSuccessCallback<Tracker> successCallback, ResultFailureCallback failureCallback) {
        ThreadUtil.startNewThread("getTrackers", () -> {
            List<Tracker> trackers = new FirestoreProcessor<Tracker>().getList(trackerDAO.getTrackersTask(), CommonConsumers.TRACKER_ID_SETTER,
                    failureCallback, Tracker.class);
            if (trackers.stream().noneMatch(tracker1 -> tracker1.getName().equals(tracker.getName())))
                failureCallback.onError("Tracker does not exist", null);
            else
                trackerDAO.stopTracker(tracker, successCallback, failureCallback);
        });
    }

    public void getTrackers(Date date, ResultSuccessCallback<Map<Tracker, BiParameterizedObject<List<Expense>, Double>>> successCallback, ResultFailureCallback failureCallback) {
        ThreadUtil.startNewThread("getTrackers", () -> {
            List<Tracker> trackers = new FirestoreProcessor<Tracker>().getList(trackerDAO.getTrackersTask(), CommonConsumers.TRACKER_ID_SETTER,
                    failureCallback, Tracker.class);
            if (trackers != null) {
                Log.d("TrackerHandler", "getTrackers: " + trackers.stream().map(Tracker::getName).collect(Collectors.joining(", ")));
                Date minStartDate = trackers.stream().map(Tracker::getDate).min(Date::compareTo).orElse(null);
                if (minStartDate != null && date != null) {
                    Map<String, Tracker> trackerByNames = new HashMap<>();
                    trackers.forEach(tracker -> {
                        trackerByNames.put(tracker.getName(), tracker);
                        trackerMap.putIfAbsent(tracker, new BiParameterizedObject<>(new ArrayList<>(), 0.0));
                    });
                    List<Expense> expenses = expenseDAO.getTrackerExpenses(minStartDate, date, true, true, failureCallback);
                    List<Expense> recurringExpenses = expenseDAO.getRecurringTrackerExpenses(minStartDate, date, true, failureCallback);
                    Log.d("TrackerHandler", "expenses: " + expenses.size() + " recurring: " + recurringExpenses.size());
                    expenses.forEach(expense -> {
                        expense.getTrackers().forEach(trackerName -> {
                            if (trackerByNames.containsKey(trackerName) && CommonUtils.validateExpenseInTracker(trackerByNames.get(trackerName), expense)) {
                                trackerMap.get(trackerByNames.get(trackerName)).getFirst().add(expense);
                                trackerMap.get(trackerByNames.get(trackerName)).setSecond(trackerMap.get(trackerByNames.get(trackerName)).getSecond() + expense.getAmount());
                            }
                        });
                    });
                    Log.d("TrackerHandler", "Expenses sum: " + expenses.stream().map(Expense::getAmount).reduce(0.0, Double::sum));
                    recurringExpenses.forEach(expense -> {
                        expense.getTrackers().forEach(trackerName -> {
                            int count = CommonUtils.getValidRecurringMonthsBetween(expense.getRecur(), trackerByNames.get(trackerName).getDate(), date);
                            Log.d("TrackerHandler", "Recurring expense: " + expense.getName() + " amount: " + expense.getAmount() + " count: " + count);
                            expense.setAmount(expense.getAmount() * count);
                            trackerMap.putIfAbsent(trackerByNames.get(trackerName), new BiParameterizedObject<>(new ArrayList<>(), 0.0));
                            trackerMap.get(trackerByNames.get(trackerName)).getFirst().add(expense);
                            trackerMap.get(trackerByNames.get(trackerName)).setSecond(trackerMap.get(trackerByNames.get(trackerName)).getSecond() + expense.getAmount());
                        });
                    });
                    Log.d("TrackerHandler", "Recurring expenses sum: " + recurringExpenses.stream().mapToDouble(Data::getAmount).sum());
                    trackerMap.forEach((tracker, biParameterizedObject) -> {
                        int totalPeriods = TrackerTypeEnum.getByName(tracker.getRenewPeriod()).getPeriodCount(tracker.getDate(), date);
                        biParameterizedObject.setSecond(totalPeriods * tracker.getAmount() - biParameterizedObject.getSecond());
                    });
                }
            }
            successCallback.onResult(trackerMap, null);
        });
    }

    public List<String> getTrackerNames() {
        return trackerMap.keySet().stream()
                .map(tracker -> new UiTracker(tracker))
                .sorted(getComparator())
                .map(UiTracker::getName)
                .collect(Collectors.toList());
    }

    public void showBudgetChart(MainActivity activity, List<Expense> expenses, Date fromDate, Date toDate, double renewAmount) {
        View view = LayoutInflater.from(activity).inflate(R.layout.alert_chart, null, false);
        LineChart lineChart = ((LineChart) view.findViewById(R.id.alert_chart));
        List<Entry> dayWiseEntries = new ArrayList<>();  // Day wise Expense
        List<Entry> cumulativeEntries = new ArrayList<>();  // Cumulative
        List<Entry> budget = new ArrayList<>();  // Budget Limit
        List<String> axisLabels = new ArrayList<>();

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(fromDate);
        int days = CommonUtils.getDaysBetween(fromDate, toDate);
        double cumulative = 0.0;
        for (int i = 0; i <= days; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd E", Locale.getDefault());
            double dayExp = expenses.stream().filter(expense -> CommonUtils.getDaysBetween(expense.getDate(), calendar.getTime()) == 0).map(Expense::getAmount).reduce(Double::sum).orElse(0.0);
            cumulative += -dayExp + renewAmount;
            axisLabels.add(sdf.format(calendar.getTime()));
            dayWiseEntries.add(new Entry(i, (float) dayExp));
            cumulativeEntries.add(new Entry(i, (float) cumulative));
            budget.add(new Entry(i, (float) renewAmount));
            calendar.add(Calendar.DATE, 1);
        }

        LineDataSet dayWiseDataSet = new LineDataSet(dayWiseEntries, "Expense");
        dayWiseDataSet.setColor(activity.getColor(R.color.budget_day));
        LineDataSet cumulativeDataSet = new LineDataSet(cumulativeEntries, "Budget");
        cumulativeDataSet.setColor(activity.getColor(R.color.budget_cumulative));
        cumulativeDataSet.setFillColor(activity.getColor(R.color.budget_cumulative));
        cumulativeDataSet.setDrawFilled(true);
        LineDataSet budgetLimit = new LineDataSet(budget, "Expense Limit");
        budgetLimit.setDrawCircles(false);
        budgetLimit.setColor(activity.getColor(R.color.budget_limit));
        budgetLimit.setValueTextSize(0f);
        budgetLimit.enableDashedLine(10f, 10f, 0f);

        LineData lineData = new LineData(dayWiseDataSet, cumulativeDataSet, budgetLimit);
        lineChart.setData(lineData);
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return axisLabels.get((int) value);
            }
        });
        lineChart.setVisibleXRangeMaximum(4);
        lineChart.getXAxis().setGranularity(1.0f);
        Description description = new Description();
        description.setText("Budget Chart");
        lineChart.setDescription(description);
        lineChart.invalidate();

        new AlertDialog.Builder(activity)
                .setTitle("Chart")
                .setView(view)
                .setOnDismissListener(dialog -> ((ViewGroup) view.getParent()).removeView(view))
                .setPositiveButton("Okay", (dialog, which) -> dialog.dismiss())
                .create().show();
    }

    public void clear() {
        trackerMap.clear();
    }

    public Comparator<? super UiTracker> getComparator() {
        return Comparator.comparing(uiTracker -> ((UiTracker)uiTracker).getName().equals("Daily Budget"))
                .thenComparing(uiTracker -> ((UiTracker)uiTracker).getTracker().getName().contains("Gather"))
                .thenComparing(uiTracker -> ((UiTracker)uiTracker).getTracker().getRenewPeriod().equals(TrackerTypeEnum.ONE_TIME.getName()))
                .thenComparing(uiTracker -> ((UiTracker)uiTracker).getTracker().getRenewPeriod().equals(TrackerTypeEnum.DAILY.getName()))
                .thenComparing(uiTracker -> ((UiTracker)uiTracker).getTracker().getRenewPeriod().equals(TrackerTypeEnum.WEEKLY.getName()))
                .thenComparing(uiTracker -> ((UiTracker)uiTracker).getTracker().getRenewPeriod().equals(TrackerTypeEnum.MONTHLY.getName()))
                .thenComparing(uiTracker -> ((UiTracker)uiTracker).getTracker().getRenewPeriod().equals(TrackerTypeEnum.QUARTERLY.getName()))
                .thenComparing(uiTracker -> ((UiTracker)uiTracker).getTracker().getRenewPeriod().equals(TrackerTypeEnum.ANNUALLY.getName()))
                .reversed();
    }

//    public Map<String, Tracker> getTrackerMap() {
//
//    }
}

    