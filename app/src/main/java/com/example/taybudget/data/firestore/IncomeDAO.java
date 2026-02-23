package com.example.taybudget.data.firestore;

import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.QueryCallback;
import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.model.Income;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.example.taybudget.enums.DevUsers;
import com.example.taybudget.tools.CommonUtils;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IncomeDAO {
    public static final String TAG = "IncomeDAO";

    public void getById(String name, Date date, ResultCallback<Income> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(DatabaseSchemaEnum.INCOME.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("name", name)
                .whereEqualTo("date", date)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().size() == 0)
                            callback.onNoResultFound();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            callback.onResult(document.toObject(Income.class), document.getId());
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        callback.onError("Error in fetching income for validation", task.getException());
                    }
                });
    }

    public void addIncomeAndUpdateAggregates(Income income, Map<String, MonthlyAggregate> monthlyAggregateMap, MasterAggregate masterAggregate,
                                             String masterAggId, QueryCallback callback) {
        // Add income
        // Update Monthly Aggregate
        // Update Master Aggregate

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference incomeRef = db.collection(DatabaseSchemaEnum.INCOME.getName());
        CollectionReference monthlyAggRef = db.collection(DatabaseSchemaEnum.MONTHLY_AGGREGATE.getName());
        CollectionReference masterAggRef = db.collection(DatabaseSchemaEnum.MASTER_AGGREGATE.getName());

        db.runTransaction(transaction -> {
                    DocumentReference incomeNewDoc = incomeRef.document();
                    transaction.set(incomeNewDoc, income);
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

    public void removeIncomeAndUpdateAggregates(Income income, Map<String, MonthlyAggregate> monthlyAggregateMap, MasterAggregate masterAggregate, String masterAggId, QueryCallback callback) {

        // Add income
        // Update Monthly Aggregate
        // Update Master Aggregate

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference incomeRef = db.collection(DatabaseSchemaEnum.INCOME.getName());
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
                    incomeRef.document(income.getDocumentId()).delete();
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

    public void getRecurring(ResultCallback<List<Income>> callback, Date date, boolean validate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(DatabaseSchemaEnum.INCOME.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("recurring", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Income> incomes = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            Income income = document.toObject(Income.class);
                            income.setDocumentId(document.getId());
                            if (!validate || CommonUtils.validateRecurringData(income.getRecur(), date))
                                incomes.add(income);
                        }
                        callback.onResult(incomes, null);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        callback.onError("Error in fetching recurring incomes", task.getException());
                    }
                });
    }

    public void getNonRecurringData(Date date, ResultSuccessCallback<List<Income>> resultSuccessCallback, ResultFailureCallback resultFailureCallback) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDatOfMonth = calendar.getTime();
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DATE, -1);
        Date lastDayOfMonth = calendar.getTime();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.INCOME.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("recurring", false)
                .whereGreaterThanOrEqualTo("date", firstDatOfMonth)
                .whereLessThanOrEqualTo("date", lastDayOfMonth)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Income> incomes = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            Income income = document.toObject(Income.class);
                            income.setDocumentId(document.getId());
                            incomes.add(income);
                        }
                        resultSuccessCallback.onResult(incomes, null);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        resultFailureCallback.onError("Error in fetching non recurring incomes", task.getException());
                    }
                });
    }
}
