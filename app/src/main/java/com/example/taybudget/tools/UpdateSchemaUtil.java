package com.example.taybudget.tools;

import android.util.Log;

import com.example.taybudget.data.model.Expense;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.example.taybudget.enums.DevUsers;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UpdateSchemaUtil {
    private static volatile UpdateSchemaUtil INSTANCE = null;

    private UpdateSchemaUtil() {
    }

    public static UpdateSchemaUtil getInstance() {
        if (INSTANCE == null) {
            synchronized (UpdateSchemaUtil.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UpdateSchemaUtil();
                }
            }
        }
        return INSTANCE;
    }

    public void update() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.EXPENSE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereEqualTo("hiddenFromDailyBudget", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Expense> expenses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("UpdateSchemaUtil", document.getId() + " => " + document.getData());
                            if (!document.contains("trackerCount")) {
                                Expense expense = document.toObject(Expense.class);
                                expense.setDocumentId(document.getId());
                                expense.setTrackers(new ArrayList<>());
                                expense.addTracker("Daily Budget");
                                expenses.add(expense);
                            }
                        }

                        db.runTransaction(transaction -> {
                            for (Expense expense : expenses) {
                                transaction.set(db.collection(DatabaseSchemaEnum.EXPENSE.getName()).document(expense.getDocumentId()), expense);
                            }
                            return null;
                        }).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.d("UpdateSchemaUtil", "Transaction complete");
                            } else {
                                Log.w("UpdateSchemaUtil", "Error adding document", task1.getException());
                            }
                        });
                    } else {
                        Log.w("UpdateSchemaUtil", "Error getting documents.", task.getException());
                    }
                });
    }
}

    