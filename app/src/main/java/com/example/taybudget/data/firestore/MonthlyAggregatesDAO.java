package com.example.taybudget.data.firestore;

import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.RecurringHandler;
import com.example.taybudget.data.model.LimitedAmount;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.enums.AggregateType;
import com.example.taybudget.enums.CategoryEnum;
import com.example.taybudget.enums.DataType;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.example.taybudget.enums.DevUsers;
import com.example.taybudget.tools.CommonUtils;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyAggregatesDAO {
    private static final String TAG = "MonthlyAggregatesDAO";

    public void getMonthlyAggregate(Date date, ResultCallback<MonthlyAggregate> callback, boolean calculateAggregateButNotInsert) {
        String name = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.MONTHLY_AGGREGATE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("name", name)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().size() == 0) {
                            if (!calculateAggregateButNotInsert)
                                callback.onResult(null, null);
                            else {
                                RecurringHandler.getInstance().getNewMonthData(date, (incomes, expenses) -> {
                                    MonthlyAggregate monthlyAggregate = new MonthlyAggregate(DevUsers.TAY.getId(), name, date, 0, 0);
                                    incomes.forEach(income -> CommonUtils.updateAggregate(monthlyAggregate, income, AggregateType.MONTH, DataType.INCOME, false));
                                    expenses.forEach(expense -> CommonUtils.updateAggregate(monthlyAggregate, expense, AggregateType.MONTH, DataType.EXPENSE, false));
                                    callback.onResult(monthlyAggregate, null);
                                }, callback);
                            }
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            callback.onResult(document.toObject(MonthlyAggregate.class), document.getId());
                        }
                    } else {
                        Log.w(TAG, "Error getting monthly aggregates.", task.getException());
                        callback.onError("Exception in getting monthly aggregates", task.getException());
                    }
                });
    }

    public void getMonthlyAggregates(List<String> names, ResultSuccessCallback<Map<String, MonthlyAggregate>> successCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.MONTHLY_AGGREGATE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereIn("name", names)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, MonthlyAggregate> monthlyAggregates = new HashMap<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            monthlyAggregates.put(document.getId(), document.toObject(MonthlyAggregate.class));
                        }
                        successCallback.onResult(monthlyAggregates, null);
                    } else {
                        Log.w(TAG, "Error getting monthly aggregates.", task.getException());
                        failureCallback.onError("Exception in getting monthly aggregates", task.getException());
                    }
                });
    }

    public void getMonthlyAggregate(Date date, boolean insertIfNotExists, ResultCallback<MonthlyAggregate> callback,
                                    ResultSuccessCallback<Boolean> newInsertedCallback,
                                    MasterAggregate masterAggregate, String masterAggregateId, boolean calculateAggregateButNotInsert) {
        String name = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);
        if (!insertIfNotExists)
            getMonthlyAggregate(date, callback, calculateAggregateButNotInsert);
        else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(DatabaseSchemaEnum.MONTHLY_AGGREGATE.getName())
                    .whereEqualTo("user", DevUsers.TAY.getId())
                    .whereEqualTo("name", name)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().size() == 0) {
                                insertMonthlyAggregate(date, callback, newInsertedCallback, masterAggregate, masterAggregateId);
                            }
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                callback.onResult(document.toObject(MonthlyAggregate.class), document.getId());
                            }
                        } else {
                            Log.w(TAG, "Error getting monthly aggregates.", task.getException());
                            callback.onError("Exception in getting monthly aggregates", task.getException());
                        }
                    });
        }
    }

    void insertMonthlyAggregate(Date d, ResultCallback<MonthlyAggregate> callback, ResultSuccessCallback<Boolean> newInsertedCallback, MasterAggregate masterAggregate, String masterAggregateId) {
        // Initializing with recurring data
        RecurringHandler.getInstance().getNewMonthData(Calendar.getInstance(Locale.getDefault()).getTime(), ((incomes, expenses) -> {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setTime(d);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date date = calendar.getTime();

            String name = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);
            MonthlyAggregate monthlyAggregate = new MonthlyAggregate(DevUsers.TAY.getId(), name, date, 0, 0);

            incomes.forEach(income -> {
                CommonUtils.updateAggregate(monthlyAggregate, income, AggregateType.MONTH, DataType.INCOME, false);
                CommonUtils.updateAggregate(masterAggregate, income, AggregateType.MASTER, DataType.INCOME, false);
            });
            expenses.forEach(expense -> {
                CommonUtils.updateAggregate(monthlyAggregate, expense, AggregateType.MONTH, DataType.EXPENSE, false);
                CommonUtils.updateAggregate(masterAggregate, expense, AggregateType.MASTER, DataType.EXPENSE, false);
            });

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference monthlyAggRef = db.collection(DatabaseSchemaEnum.MONTHLY_AGGREGATE.getName());
            CollectionReference masterAggRef = db.collection(DatabaseSchemaEnum.MASTER_AGGREGATE.getName());

            db.runTransaction(transaction -> {
                        DocumentReference newMonthlyDoc = monthlyAggRef.document();
                        transaction.set(newMonthlyDoc, monthlyAggregate);
                        masterAggRef.document(masterAggregateId).set(masterAggregate);
                        return null;
                    }).addOnSuccessListener(documentReference -> {
                        if (newInsertedCallback != null)
                            newInsertedCallback.onResult(true, null);
                        getMonthlyAggregate(date, callback, false);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error adding document", e);
                        callback.onError("Error in inserting monthly aggregate", e);
                    });
        }), callback);
    }

    public void getAvailableMonths(ResultSuccessCallback<List<MonthlyAggregate>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.MONTHLY_AGGREGATE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .limit(12)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<MonthlyAggregate> aggregates = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            aggregates.add(document.toObject(MonthlyAggregate.class));
                        }
                        resultSuccessCallback.onResult(aggregates, null);
                    } else {
                        Log.w(TAG, "Error getting monthly aggregates.", task.getException());
                        resultFailureCallback.onError("Exception in getting monthly aggregates", task.getException());
                    }
                });
    }
}
