package com.example.taybudget.data.firestore;

import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Goal;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GoalDAO {
    private static final String TAG = "GoalDAO";

    public void insertGoal(Goal goal, ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.GOAL.getName())
                .add(goal)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        successCallback.onResult("Goal inserted successfully", task.getResult().getId());
                    else
                        failureCallback.onError("An error occurred in inserting goal", null);
                }).addOnFailureListener(e -> failureCallback.onError("An error occurred in inserting goal", e));
    }

    public void removeGoal(Goal goal, ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.GOAL.getName()).document(goal.getId())
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        successCallback.onResult("Goal deleted successfully", null);
                    else
                        failureCallback.onError("An error occurred in deleting goal", null);
                }).addOnFailureListener(e -> failureCallback.onError("An error occurred in deleting goal", e));
    }

    public void getAllGoals(ResultSuccessCallback<List<Goal>> successCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.GOAL.getName())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Goal> goals = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            Goal goal = document.toObject(Goal.class);
                            goal.setId(document.getId());
                            goals.add(goal);
                        }
                        successCallback.onResult(goals, null);
                    } else {
                        failureCallback.onError("An error occurred in retrieving goals", null);
                    }
                }).addOnFailureListener(e -> failureCallback.onError("An error occurred in retrieving goals", e));
    }
}
