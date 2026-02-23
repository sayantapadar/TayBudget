package com.example.taybudget.data.handler;

import com.example.taybudget.data.firestore.ExpenseDAO;
import com.example.taybudget.data.firestore.MasterAggregatesDAO;
import com.example.taybudget.data.firestore.MonthlyAggregatesDAO;
import com.example.taybudget.data.firestore.interfaces.QueryCallback;
import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.interfaces.AggregatesResultCallback;
import com.example.taybudget.data.handler.interfaces.TaskCompletionCallback;
import com.example.taybudget.data.handler.interfaces.ValidationErrorCallback;
import com.example.taybudget.data.handler.model.BiParameterizedObject;
import com.example.taybudget.data.model.Data;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.data.model.Recurring;
import com.example.taybudget.enums.AggregateType;
import com.example.taybudget.enums.DataType;
import com.example.taybudget.enums.DevUsers;
import com.example.taybudget.tools.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseHandler {

    private static ExpenseHandler instance;
    private final ExpenseDAO expenseDAO;
    private final MonthlyAggregatesDAO monthlyAggregatesDAO;
    private final MasterAggregatesDAO masterAggregatesDAO;

    private ExpenseHandler() {
        super();
        expenseDAO = new ExpenseDAO();
        monthlyAggregatesDAO = new MonthlyAggregatesDAO();
        masterAggregatesDAO = new MasterAggregatesDAO();
    }

    public static ExpenseHandler getInstance() {
        if (instance == null)
            instance = new ExpenseHandler();
        return instance;
    }

    /**
     * Underlying assumption - If it is a new month, opening the app should create the new monthly aggregate.
     * Here new aggregate isn't being created, to avoid historical aggregates from being created.
     * Historical aggregates not needed, unless it was maintained from before
     * Eg. We start maintaining from September, adding an expense from past should not create new aggregate month
     */
    public void addExpense(String category, String name, double amount, Date date, boolean recurring,
                           int recurringPeriod, Date recurringDateEnd, List<String> tags, List<String> trackers,
                           ValidationErrorCallback callback, TaskCompletionCallback taskCompletionCallback) {

        if (!validate(category, name, amount, date, recurring, recurringPeriod, recurringDateEnd, callback)) {
            taskCompletionCallback.error(null);
            return;
        }

        Expense expense = new Expense(DevUsers.TAY.getId(), category, name, date, amount, recurring,
                new Recurring(date, recurringDateEnd, recurringPeriod));
        expense.getTags().addAll(tags);
        expense.setTrackers(trackers);
        if (expense.getTags() != null && expense.getTags().size() > 1) // SMSActivity is setting Card Name | SMSId as the 2 tags
            expense.setSmsId(Integer.parseInt(expense.getTags().get(1)));
        getAggregates(date, taskCompletionCallback, ((masterDocId, masterAggregate, monthDocId, monthlyAggregate) ->
                addExpenseAndUpdateAggregates(expense, monthlyAggregate,
                        masterAggregate, monthDocId, masterDocId,
                        (message, documentId) -> taskCompletionCallback.taskComplete(message),
                        (message, exception) -> taskCompletionCallback.error(message))));
    }

    private boolean validate(String category, String name, double amount, Date date, boolean recurring,
                             int recurringPeriod, Date recurringDateEnd, ValidationErrorCallback callback) {

        if (recurringPeriod > 12 || recurringPeriod < 1) {
            callback.onValidationError("Recurring Period is between 1-12");
            return false;
        }
        if (recurring && recurringDateEnd.compareTo(date) < 0) {
            callback.onValidationError("End date is before start date");
            return false;
        }
        if (recurring && recurringDateEnd.compareTo(Calendar.getInstance(Locale.getDefault()).getTime()) < 0) {
            callback.onValidationError("End date is in the past");
            return false;
        }
        if (amount == 0) {
            callback.onValidationError("Amount cannot be 0");
            return false;
        }
        if (name.equals("")) {
            callback.onValidationError("Name cannot be blank");
            return false;
        }
        return true;
    }

    private void getAggregates(Date date, TaskCompletionCallback taskCompletionCallback, AggregatesResultCallback aggregatesResultCallback) {
        // Get master aggregates
        masterAggregatesDAO.getMasterAggregate(true, new ResultCallback<MasterAggregate>() {
            @Override
            public void onResult(MasterAggregate masterAggregate, String masterAggId) {

                // Get monthly aggregates

                Calendar monthYear = Calendar.getInstance(Locale.getDefault());
                monthYear.setTime(date);
                monthYear.set(Calendar.DAY_OF_MONTH, 1);

                monthlyAggregatesDAO.getMonthlyAggregate(monthYear.getTime(), new ResultCallback<MonthlyAggregate>() {
                    @Override
                    public void onResult(MonthlyAggregate monthlyAggregate, String monthlyAggId) {
                        aggregatesResultCallback.getResult(masterAggId, masterAggregate, monthlyAggId, monthlyAggregate);
                    }

                    @Override
                    public void onError(String message, Exception exception) {
                        taskCompletionCallback.error(message);
                    }

                    @Override
                    public void onNoResultFound() {
                        taskCompletionCallback.error("Should not happen. WTF");
                    }
                }, false);
            }

            @Override
            public void onError(String message, Exception exception) {
                taskCompletionCallback.error(message);
            }

            @Override
            public void onNoResultFound() {
                taskCompletionCallback.error("Should not happen. WTF");
            }
        });
    }

    private void addExpenseAndUpdateAggregates(Expense expense, MonthlyAggregate monthlyAggregate, MasterAggregate masterAggregate,
                                               String monthlyAggId, String masterAggId,
                                               ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {
        // TODO get all savepoints that need to be updated & update
        updateAggregates(expense, monthlyAggregate, masterAggregate, monthlyAggId, masterAggId,
                (result, documentId) -> expenseDAO.addExpenseAndUpdateAggregates(expense, result.getSecond(), result.getFirst(), masterAggId, new QueryCallback() {
                    @Override
                    public void onSuccess() {
                        successCallback.onResult("Expense added successfully", null);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        failureCallback.onError("An error has occurred while inserting expense", e);
                    }
                }),
                failureCallback, false);
    }


    private void removeExpenseAndUpdateAggregates(Expense expense, MonthlyAggregate monthlyAggregate, MasterAggregate masterAggregate,
                                                  String monthlyAggId, String masterAggId,
                                                  ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {

        // TODO get all savepoints that need to be updated & update
        updateAggregates(expense, monthlyAggregate, masterAggregate, monthlyAggId, masterAggId,
                ((result, documentId) -> expenseDAO.removeExpenseAndUpdateAggregates(expense, result.getSecond(), result.getFirst(), masterAggId, new QueryCallback() {
                    @Override
                    public void onSuccess() {
                        successCallback.onResult("Expense deleted successfully", null);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        failureCallback.onError("An error has occurred while inserting expense", e);
                    }
                })), failureCallback, true);
    }

    private void updateAggregates(Data data, MonthlyAggregate monthlyAggregate, MasterAggregate masterAggregate,
                                  String monthlyAggId, String masterAggId,
                                  ResultSuccessCallback<BiParameterizedObject<MasterAggregate, Map<String, MonthlyAggregate>>> successCallback,
                                  ResultFailureCallback failureCallback, boolean remove) {

        Map<String, MonthlyAggregate> monthlyAggregateUpdates = new HashMap<>();

        if (monthlyAggregate != null || data.isRecurring()) {
            Date today = Calendar.getInstance(Locale.getDefault()).getTime();
            if (!data.isRecurring()) {
                CommonUtils.updateAggregate(monthlyAggregate, data, AggregateType.MONTH, DataType.EXPENSE, remove);
                CommonUtils.updateAggregate(masterAggregate, data, AggregateType.MASTER, DataType.EXPENSE, remove);
                monthlyAggregateUpdates.put(monthlyAggId, monthlyAggregate);
                successCallback.onResult(new BiParameterizedObject<>(masterAggregate, monthlyAggregateUpdates), null);
            } else if (data.getRecur().getFrom().compareTo(today) <= 0) {  // recurring and does not start in future
                Calendar calendar = Calendar.getInstance(Locale.getDefault());
                calendar.setTime(data.getRecur().getFrom());
                int months = CommonUtils.getMonthsBetween(calendar.getTime(), today);
                List<String> impactedMonths = new ArrayList<>();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                for (int i = 0; i < months; i++) {
                    calendar.add(Calendar.MONTH, i);
                    if (CommonUtils.validateRecurringData(data.getRecur(), calendar.getTime()))
                        impactedMonths.add(simpleDateFormat.format(calendar.getTime()));
                }
                if (impactedMonths.size() > 0)
                    monthlyAggregatesDAO.getMonthlyAggregates(impactedMonths,
                            (monthlyAggregateMap, documentId) -> {
                                if (CommonUtils.validateRecurringData(data.getRecur(), today))  // current month
                                    monthlyAggregateMap.put(monthlyAggId, monthlyAggregate);
                                monthlyAggregateMap.forEach((docId, aggregate) -> {
                                    if (aggregate != null)
                                        CommonUtils.updateAggregate(aggregate, data, AggregateType.MONTH, DataType.EXPENSE, remove);
                                    CommonUtils.updateAggregate(masterAggregate, data, AggregateType.MASTER, DataType.EXPENSE, remove);
                                });
                                successCallback.onResult(new BiParameterizedObject<>(masterAggregate, monthlyAggregateMap), null);
                            }, failureCallback);
                else {
                    if (CommonUtils.validateRecurringData(data.getRecur(), today))  // current month
                        monthlyAggregateUpdates.put(monthlyAggId, monthlyAggregate);
                    monthlyAggregateUpdates.forEach((docId, aggregate) -> {
                        if (aggregate != null)
                            CommonUtils.updateAggregate(aggregate, data, AggregateType.MONTH, DataType.EXPENSE, remove);
                        CommonUtils.updateAggregate(masterAggregate, data, AggregateType.MASTER, DataType.EXPENSE, remove);
                    });
                    successCallback.onResult(new BiParameterizedObject<>(masterAggregate, monthlyAggregateUpdates), null);
                }
            } else  // recurring income in the future
                successCallback.onResult(new BiParameterizedObject<>(null, null), null);
        } else
            successCallback.onResult(new BiParameterizedObject<>(null, null), null);
    }

    public void getMonthExpense(Date date, ResultSuccessCallback<List<Expense>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        RecurringHandler.getInstance().getRecurringExpenseData(date, (recurringExpense, documentId) -> {
            expenseDAO.getNonRecurringData(date, (nonRecurringExpense, documentId1) -> {
                recurringExpense.addAll(nonRecurringExpense);
                resultSuccessCallback.onResult(recurringExpense, null);
            }, resultFailureCallback);
        }, resultFailureCallback);
    }

    public void editExpense(Expense expense, String category, String name, double amount, Date date, boolean recurring,
                            int recurringPeriod, Date recurringDateEnd, List<String> tags, List<String> trackers,
                            ValidationErrorCallback callback, TaskCompletionCallback taskCompletionCallback) {
        if (!validate(category, name, amount, date, recurring, recurringPeriod, recurringDateEnd, callback)) {
            taskCompletionCallback.error(null);
            return;
        }

        Expense newExpense = new Expense(DevUsers.TAY.getId(), category, name, date, amount, recurring,
                new Recurring(date, recurringDateEnd, recurringPeriod));
        newExpense.getTags().addAll(tags);
        newExpense.setTrackers(trackers);
        if (newExpense.getTags() != null && newExpense.getTags().size() > 1)
            newExpense.setSmsId(Integer.parseInt(newExpense.getTags().get(1)));

        getAggregates(expense.getDate(), taskCompletionCallback, (masterDocId, masterAggregate, monthDocId, monthlyAggregate) ->
                removeExpenseAndUpdateAggregates(expense, monthlyAggregate, masterAggregate, monthDocId, masterDocId,
                        (success, did) -> addExpenseAndUpdateAggregates(newExpense, monthlyAggregate, masterAggregate, monthDocId, masterDocId,
                                (message, dId) -> taskCompletionCallback.taskComplete(message),
                                (message, exception) -> taskCompletionCallback.error(message)),
                        (failure, exception) -> taskCompletionCallback.error(failure)));
    }

    public void deleteExpense(Expense expense, TaskCompletionCallback taskCompletionCallback) {
        getAggregates(expense.getDate(), taskCompletionCallback, (masterDocId, masterAggregate, monthDocId, monthlyAggregate) ->
                removeExpenseAndUpdateAggregates(expense, monthlyAggregate, masterAggregate, monthDocId, masterDocId,
                        (message, did) -> taskCompletionCallback.taskComplete(message),
                        (message, exception) -> taskCompletionCallback.error(message)));
    }
}
