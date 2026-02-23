package com.example.taybudget.data.handler;

import com.example.taybudget.data.firestore.IncomeDAO;
import com.example.taybudget.data.firestore.MasterAggregatesDAO;
import com.example.taybudget.data.firestore.MonthlyAggregatesDAO;
import com.example.taybudget.data.firestore.interfaces.QueryCallback;
import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.interfaces.AggregatesResultCallback;
import com.example.taybudget.data.handler.interfaces.TaskCompletionCallback;
import com.example.taybudget.data.handler.interfaces.TaskFailedCallback;
import com.example.taybudget.data.handler.interfaces.ValidationErrorCallback;
import com.example.taybudget.data.handler.model.BiParameterizedObject;
import com.example.taybudget.data.model.Data;
import com.example.taybudget.data.model.Income;
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

public class IncomeHandler {
    private static IncomeHandler instance;
    private final IncomeDAO incomeDAO;
    private final MonthlyAggregatesDAO monthlyAggregatesDAO;
    private final MasterAggregatesDAO masterAggregatesDAO;

    private IncomeHandler() {
        incomeDAO = new IncomeDAO();
        monthlyAggregatesDAO = new MonthlyAggregatesDAO();
        masterAggregatesDAO = new MasterAggregatesDAO();
    }

    public static IncomeHandler getInstance() {
        if (instance == null)
            instance = new IncomeHandler();
        return instance;
    }

    public void addIncome(String name, double amount, Date date, boolean recurring, int recurringPeriod,
                          Date recurringDateEnd, boolean taxable, ValidationErrorCallback callback, TaskCompletionCallback taskCompletionCallback) {

        if (!validate(name, amount, date, recurring, recurringPeriod, recurringDateEnd, taxable, callback)) {
            taskCompletionCallback.error(null);
            return;
        }

        incomeDAO.getById(name, date, new ResultCallback<Income>() {
            @Override
            public void onResult(Income result, String documentId) {
                callback.onValidationError("Income with same name and date already exists");
                taskCompletionCallback.error(null);
            }

            @Override
            public void onError(String message, Exception exception) {
                exception.printStackTrace();
                taskCompletionCallback.error(message);
            }

            @Override
            public void onNoResultFound() {
                Income income = new Income(DevUsers.TAY.getId(), name, date, recurring,
                        new Recurring(date, recurringDateEnd, recurringPeriod), taxable, amount);
                getAggregates(date, taskCompletionCallback, (masterDocId, masterAggregate, monthDocId, monthlyAggregate) ->
                        addIncomeAndUpdateAggregates(income, monthlyAggregate,
                                masterAggregate, monthDocId, masterDocId,
                                (message, documentId) -> taskCompletionCallback.taskComplete(message),
                                (message, exception) -> taskCompletionCallback.error(message)));
            }
        });
    }

    private boolean validate(String name, double amount, Date date, boolean recurring, int recurringPeriod,
                             Date recurringDateEnd, boolean taxable, ValidationErrorCallback callback) {

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

    private void addIncomeAndUpdateAggregates(Income income, MonthlyAggregate monthlyAggregate, MasterAggregate masterAggregate,
                                              String monthlyAggId, String masterAggId,
                                              ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {

        updateAggregates(income, monthlyAggregate, masterAggregate, monthlyAggId, masterAggId,
                (result, documentId) -> incomeDAO.addIncomeAndUpdateAggregates(income, result.getSecond(), result.getFirst(), masterAggId, new QueryCallback() {
                    @Override
                    public void onSuccess() {
                        successCallback.onResult("Income added successfully", null);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        failureCallback.onError("An error has occurred while inserting income", e);
                    }
                }),
                failureCallback, false);
    }

    private void removeIncomeAndUpdateAggregates(Income income, MonthlyAggregate monthlyAggregate, MasterAggregate masterAggregate,
                                                 String monthlyAggId, String masterAggId,
                                                 ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {

        updateAggregates(income, monthlyAggregate, masterAggregate, monthlyAggId, masterAggId,
                ((result, documentId) -> incomeDAO.removeIncomeAndUpdateAggregates(income, result.getSecond(), result.getFirst(), masterAggId, new QueryCallback() {
                    @Override
                    public void onSuccess() {
                        successCallback.onResult("Income deleted successfully", null);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        failureCallback.onError("An error has occurred while inserting income", e);
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
                CommonUtils.updateAggregate(monthlyAggregate, data, AggregateType.MONTH, DataType.INCOME, remove);
                CommonUtils.updateAggregate(masterAggregate, data, AggregateType.MASTER, DataType.INCOME, remove);
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
                                    CommonUtils.updateAggregate(aggregate, data, AggregateType.MONTH, DataType.INCOME, remove);
                                    CommonUtils.updateAggregate(masterAggregate, data, AggregateType.MASTER, DataType.INCOME, remove);
                                });
                                successCallback.onResult(new BiParameterizedObject<>(masterAggregate, monthlyAggregateMap), null);
                            }, failureCallback);
                else {
                    if (CommonUtils.validateRecurringData(data.getRecur(), today))  // current month
                        monthlyAggregateUpdates.put(monthlyAggId, monthlyAggregate);
                    monthlyAggregateUpdates.forEach((docId, aggregate) -> {
                        CommonUtils.updateAggregate(aggregate, data, AggregateType.MONTH, DataType.INCOME, remove);
                        CommonUtils.updateAggregate(masterAggregate, data, AggregateType.MASTER, DataType.INCOME, remove);
                    });
                    successCallback.onResult(new BiParameterizedObject<>(masterAggregate, monthlyAggregateUpdates), null);
                }
            } else  // recurring income in the future
                successCallback.onResult(new BiParameterizedObject<>(null, null), null);
        } else
            successCallback.onResult(new BiParameterizedObject<>(null, null), null);
    }

    public void getMonthIncome(Date date, ResultSuccessCallback<List<Income>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        RecurringHandler.getInstance().getRecurringIncomeData(date, (recurringIncome, documentId) -> {
            incomeDAO.getNonRecurringData(date, (nonRecurringIncome, documentId1) -> {
                recurringIncome.addAll(nonRecurringIncome);
                resultSuccessCallback.onResult(recurringIncome, null);
            }, resultFailureCallback);
        }, resultFailureCallback);
    }

    public void editIncome(Income income, String name, double amount, Date date, boolean recurring, int recurringPeriod,
                           Date recurringDateEnd, boolean taxable, ValidationErrorCallback callback,
                           TaskCompletionCallback taskCompletionCallback) {
        if (!validate(name, amount, date, recurring, recurringPeriod, recurringDateEnd, taxable, callback)) {
            taskCompletionCallback.error(null);
            return;
        }

        Income newIncome = new Income(DevUsers.TAY.getId(), name, date, recurring,
                new Recurring(date, recurringDateEnd, recurringPeriod), taxable, amount);

        setIncomeDocumentID(income, (result, documentId) ->
                getAggregates(income.getDate(), taskCompletionCallback, (masterDocId, masterAggregate, monthDocId, monthlyAggregate) ->
                        removeIncomeAndUpdateAggregates(income, monthlyAggregate, masterAggregate, monthDocId, masterDocId,
                                (success, did) -> addIncomeAndUpdateAggregates(newIncome, monthlyAggregate, masterAggregate, monthDocId, masterDocId,
                                        (message, dId) -> taskCompletionCallback.taskComplete(message),
                                        (message, exception) -> taskCompletionCallback.error(message)),
                                (failure, exception) -> taskCompletionCallback.error(failure))), taskCompletionCallback);
    }

    public void deleteIncome(Income income, TaskCompletionCallback taskCompletionCallback) {
        setIncomeDocumentID(income, (result, documentId) ->
                getAggregates(income.getDate(), taskCompletionCallback, (masterDocId, masterAggregate, monthDocId, monthlyAggregate) ->
                        removeIncomeAndUpdateAggregates(income, monthlyAggregate, masterAggregate, monthDocId, masterDocId,
                                (message, did) -> taskCompletionCallback.taskComplete(message),
                                (message, exception) -> taskCompletionCallback.error(message))), taskCompletionCallback);
    }

    private void setIncomeDocumentID(Income income, ResultSuccessCallback<Income> resultSuccessCallback, TaskFailedCallback taskFailedCallback) {
        if (income.getDocumentId() != null)
            resultSuccessCallback.onResult(income, income.getDocumentId());
        else
            incomeDAO.getById(income.getName(), income.getDate(), new ResultCallback<Income>() {
                @Override
                public void onNoResultFound() {
                    taskFailedCallback.error("Could not find income to delete");
                }

                @Override
                public void onError(String message, Exception exception) {
                    taskFailedCallback.error(message);
                }

                @Override
                public void onResult(Income income, String documentId) {
                    income.setDocumentId(documentId);
                    resultSuccessCallback.onResult(income, income.getDocumentId());
                }
            });
    }
}
