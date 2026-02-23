package com.example.taybudget.data.handler;

import com.example.taybudget.data.firestore.ExpenseDAO;
import com.example.taybudget.data.firestore.IncomeDAO;
import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.interfaces.RecurringDataCallback;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Income;

import java.util.Date;
import java.util.List;

public class RecurringHandler {
    private static RecurringHandler instance;
    private final IncomeDAO incomeDAO;
    private final ExpenseDAO expenseDAO;

    private RecurringHandler() {
        incomeDAO = new IncomeDAO();
        expenseDAO = new ExpenseDAO();
    }

    public static RecurringHandler getInstance() {
        if (instance == null)
            instance = new RecurringHandler();
        return instance;
    }

    public void getRecurringData(Date date, RecurringDataCallback recurringDataCallback, ResultFailureCallback resultFailureCallback) {
        getRecurringIncomeData(date, ((incomes, documentId) -> {
            getRecurringExpenseData(date, ((expenses, documentId1) -> {
                recurringDataCallback.onResult(incomes, expenses);
            }), resultFailureCallback);
        }), resultFailureCallback);
    }

    public void getNewMonthData(Date date, RecurringDataCallback recurringDataCallback, ResultFailureCallback resultFailureCallback) {
        getRecurringData(date, (incomes, expenses) -> {
            incomeDAO.getNonRecurringData(date, (result, documentId) -> {
                incomes.addAll(result);
                expenseDAO.getNonRecurringData(date, (result1, documentId1) -> {
                    expenses.addAll(result1);
                    recurringDataCallback.onResult(incomes, expenses);
                }, resultFailureCallback);
            }, resultFailureCallback);
        }, resultFailureCallback);
    }

    public void getRecurringDataRaw(RecurringDataCallback recurringDataCallback, ResultFailureCallback resultFailureCallback) {
        getRecurringIncomeDataRaw(((incomes, documentId) -> {
            getRecurringExpenseDataRaw(((expenses, documentId1) -> {
                recurringDataCallback.onResult(incomes, expenses);
            }), resultFailureCallback);
        }), resultFailureCallback);
    }

    public void getRecurringIncomeData(Date date, ResultSuccessCallback<List<Income>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        incomeDAO.getRecurring(new ResultCallback<List<Income>>() {
            @Override
            public void onResult(List<Income> incomes, String documentId) {
                resultSuccessCallback.onResult(incomes, documentId);
            }

            @Override
            public void onError(String message, Exception exception) {
                if (resultFailureCallback != null)
                    resultFailureCallback.onError(message, exception);
            }

            @Override
            public void onNoResultFound() {
                // Isn't called
            }
        }, date, true);
    }

    public void getRecurringIncomeDataRaw(ResultSuccessCallback<List<Income>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        incomeDAO.getRecurring(new ResultCallback<List<Income>>() {
            @Override
            public void onResult(List<Income> incomes, String documentId) {
                resultSuccessCallback.onResult(incomes, documentId);
            }

            @Override
            public void onError(String message, Exception exception) {
                if (resultFailureCallback != null)
                    resultFailureCallback.onError(message, exception);
            }

            @Override
            public void onNoResultFound() {
                // Isn't called
            }
        }, null, false);
    }

    public void getRecurringExpenseData(Date date, ResultSuccessCallback<List<Expense>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        expenseDAO.getRecurring(new ResultCallback<List<Expense>>() {
            @Override
            public void onResult(List<Expense> expenses, String documentId) {
                resultSuccessCallback.onResult(expenses, documentId);
            }

            @Override
            public void onError(String message, Exception exception) {
                if (resultFailureCallback != null)
                    resultFailureCallback.onError(message, exception);
            }

            @Override
            public void onNoResultFound() {
                // Isn't called
            }
        }, date, true);
    }

    public void getRecurringExpenseDataRaw(ResultSuccessCallback<List<Expense>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        expenseDAO.getRecurring(new ResultCallback<List<Expense>>() {
            @Override
            public void onResult(List<Expense> expenses, String documentId) {
                resultSuccessCallback.onResult(expenses, documentId);
            }

            @Override
            public void onError(String message, Exception exception) {
                if (resultFailureCallback != null)
                    resultFailureCallback.onError(message, exception);
            }

            @Override
            public void onNoResultFound() {
                // Isn't called
            }
        }, null, false);
    }
}
