package com.example.taybudget.data.firestore;

import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.ResultCallback;

import com.example.taybudget.data.model.LimitedAmount;
import com.example.taybudget.data.model.MasterAggregate;

import com.example.taybudget.enums.CategoryEnum;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.example.taybudget.enums.DevUsers;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;

public class MasterAggregatesDAO {

    private static final String TAG = "MasterAggregatesDAO";

    public void getMasterAggregate(boolean insertIfNotExists, ResultCallback<MasterAggregate> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.MASTER_AGGREGATE.getName())
                .whereEqualTo("user", DevUsers.TAY.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().size() == 0) {
                            if (insertIfNotExists)
                                insertMasterAggregate(insertIfNotExists, callback);
                            else
                                callback.onResult(null, null);
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            callback.onResult(document.toObject(MasterAggregate.class), document.getId());
                        }
                    } else {
                        Log.w(TAG, "Error getting master aggregates.", task.getException());
                        callback.onError("Exception in getting master aggregates", task.getException());
                    }
                });
    }

    private void insertMasterAggregate(boolean insertIfNotExists, ResultCallback<MasterAggregate> callback) {
        MasterAggregate masterAggregate = new MasterAggregate(DevUsers.TAY.getId(), 0, 0, 0);
        for (CategoryEnum category : CategoryEnum.values()) {
            masterAggregate.updateCategoricalUsage(category.getName(), new LimitedAmount(0, 0));
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.MASTER_AGGREGATE.getName())
                .add(masterAggregate)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    getMasterAggregate(insertIfNotExists, callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    callback.onError("Error in inserting master aggregate", e);
                });
    }
}
