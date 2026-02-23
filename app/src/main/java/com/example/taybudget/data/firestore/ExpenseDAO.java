package com.example.taybudget.data.firestore;

import android.os.Looper;
import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.QueryCallback;
import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.example.taybudget.enums.DevUsers;
import com.example.taybudget.tools.CommonUtils;
import com.example.taybudget.tools.ThreadUtil;
import com.example.taybudget.tools.exceptions.NotInThreadException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ExpenseDAO {
    public static final String TAG = "ExpenseDAO";

    public void addExpenseAndUpdateAggregates(Expense expense, Map<String, MonthlyAggregate> monthlyAggregateMap,
                                              MasterAggregate masterAggregate, String masterAggId, QueryCallback callback) {
        // Add expense
        // Update Monthly Aggregate
        // Update Master Aggregate

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference expenseRef = db.collection(DatabaseSchemaEnum.EXPENSE.getName());
        CollectionReference monthlyAggRef = db.collection(DatabaseSchemaEnum.MONTHLY_AGGREGATE.getName());
        CollectionReference masterAggRef = db.collection(DatabaseSchemaEnum.MASTER_AGGREGATE.getName());

        db.runTransaction(transaction -> {
                    DocumentReference expenseNewDoc = expenseRef.document();
                    transaction.set(expenseNewDoc, expense);
                    if (monthlyAggregateMap != null)
                        monthlyAggregateMap.forEach((monthlyAggId, monthlyAggregate)
                                -> {
                            if (monthlyAggId != null)
                                monthlyAggRef.document(monthlyAggId).set(monthlyAggregate);
                        });
                    if (masterAggregate != null)
                        masterAggRef.document(masterAggId).set(masterAggregate);
                    return null;
                }).addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transaction complete");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    callback.onFailure(e);
                });
    }

    public void removeExpenseAndUpdateAggregates(Expense expense, Map<String, MonthlyAggregate> monthlyAggregateMap, MasterAggregate masterAggregate, String masterAggId, QueryCallback callback) {

        // Delete expense
        // Update Monthly Aggregate
        // Update Master Aggregate

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference expenseRef = db.collection(DatabaseSchemaEnum.EXPENSE.getName());
        CollectionReference monthlyAggRef = db.collection(DatabaseSchemaEnum.MONTHLY_AGGREGATE.getName());
        CollectionReference masterAggRef = db.collection(DatabaseSchemaEnum.MASTER_AGGREGATE.getName());

        db.runTransaction(transaction -> {
                    if (monthlyAggregateMap != null)
                        monthlyAggregateMap.forEach((monthlyAggId, monthlyAggregate)
                                -> {
                            if (monthlyAggId != null)
                                monthlyAggRef.document(monthlyAggId).set(monthlyAggregate);
                        });
                    if (masterAggregate != null)
                        masterAggRef.document(masterAggId).set(masterAggregate);
                    expenseRef.document(expense.getDocumentId()).delete();
                    return null;
                }).addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transaction complete");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    callback.onFailure(e);
                });
    }

    public void getRecurring(ResultCallback<List<Expense>> callback, Date date, boolean validate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(DatabaseSchemaEnum.EXPENSE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("recurring", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Expense> expenses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            //Log.d(TAG, document.getId() + " => " + document.getData());
                            Expense expense = document.toObject(Expense.class);
                            expense.setDocumentId(document.getId());
                            if (!validate || CommonUtils.validateRecurringData(expense.getRecur(), date))
                                expenses.add(expense);
                        }
                        callback.onResult(expenses, null);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        callback.onError("Error in fetching recurring expenses", task.getException());
                    }
                });
    }

    public void getNonRecurringData(Date date, ResultSuccessCallback<List<Expense>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDatOfMonth = calendar.getTime();
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DATE, -1);
        Date lastDayOfMonth = calendar.getTime();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.EXPENSE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("recurring", false)
                .whereGreaterThanOrEqualTo("date", firstDatOfMonth)
                .whereLessThanOrEqualTo("date", lastDayOfMonth)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Expense> expenses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            //Log.d(TAG, document.getId() + " => " + document.getData());
                            Expense expense = document.toObject(Expense.class);
                            expense.setDocumentId(document.getId());
                            expenses.add(expense);
                        }
                        resultSuccessCallback.onResult(expenses, null);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        resultFailureCallback.onError("Error in fetching non recurring expenses", task.getException());
                    }
                });
    }

    public List<Expense> getDailyBudgetExpensesBetween(Date fromDate, Date toDate, boolean fromInclusive, boolean toInclusive, ResultFailureCallback resultFailureCallback) {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new NotInThreadException();
        List<Expense> expenses = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // TODO Recurring - Leaving this for later

        // Non recurring
        Query query = db.collection(DatabaseSchemaEnum.EXPENSE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("recurring", false)
                .whereEqualTo("hiddenFromDailyBudget", false);
        if (fromInclusive)
            query = query.whereGreaterThanOrEqualTo("date", fromDate);
        else
            query = query.whereGreaterThan("date", fromDate);
        if (toInclusive)
            query = query.whereLessThanOrEqualTo("date", toDate);
        else
            query = query.whereLessThan("date", toDate);
        Task<QuerySnapshot> task = query.get();
        try {
            QuerySnapshot result = Tasks.await(task);
            for (QueryDocumentSnapshot document : result) {
                //Log.d(TAG, document.getId() + " => " + document.getData());
                Expense expense = document.toObject(Expense.class);
                expense.setDocumentId(document.getId());
                expenses.add(expense);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.w(TAG, "Error getting documents.", task.getException());
            resultFailureCallback.onError("Error in fetching non recurring expenses", task.getException());
        }
        return expenses;
    }

    public List<Expense> getTrackerExpenses(Date fromDate, Date toDate, boolean fromInclusive, boolean toInclusive, ResultFailureCallback resultFailureCallback) {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new NotInThreadException();
        List<Expense> expenses = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Non recurring
        Query query = db.collection(DatabaseSchemaEnum.EXPENSE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("recurring", false)
                .whereGreaterThan("trackerCount", 0);
        if (fromInclusive)
            query = query.whereGreaterThanOrEqualTo("date", fromDate);
        else
            query = query.whereGreaterThan("date", fromDate);
        if (toInclusive)
            query = query.whereLessThanOrEqualTo("date", toDate);
        else
            query = query.whereLessThan("date", toDate);
        Task<QuerySnapshot> task = query.get();
        try {
            QuerySnapshot result = Tasks.await(task);
            for (QueryDocumentSnapshot document : result) {
                //Log.d(TAG, document.getId() + " => " + document.getData());
                Expense expense = document.toObject(Expense.class);
                expense.setDocumentId(document.getId());
                expenses.add(expense);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.w(TAG, "Error getting documents.", task.getException());
            resultFailureCallback.onError("Error in fetching non recurring expenses", task.getException());
        }
        return expenses;
    }

    /*
    Remember validation checks end of month
     */
    public List<Expense> getRecurringTrackerExpenses(Date fromDate, Date toDate, boolean fromInclusive, ResultFailureCallback resultFailureCallback) {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new NotInThreadException();
        List<Expense> expenses = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (!fromInclusive)
            fromDate = CommonUtils.getNextMonth(fromDate);

        // Non recurring
        Query query = db.collection(DatabaseSchemaEnum.EXPENSE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("recurring", true)
                .whereGreaterThan("trackerCount", 0);
        Task<QuerySnapshot> task = query.get();
        try {
            QuerySnapshot result = Tasks.await(task);
            for (QueryDocumentSnapshot document : result) {
                //Log.d(TAG, document.getId() + " => " + document.getData());
                Expense expense = document.toObject(Expense.class);
                expense.setDocumentId(document.getId());
                expenses.add(expense);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.w(TAG, "Error getting documents.", task.getException());
            resultFailureCallback.onError("Error in fetching non recurring expenses", task.getException());
        }

        return expenses;
    }
}
