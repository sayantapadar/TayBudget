package com.example.taybudget.data.handler;

import android.util.Log;

import com.example.taybudget.data.firestore.MasterAggregatesDAO;
import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.interfaces.TaskFailedCallback;
import com.example.taybudget.data.handler.interfaces.TaskSuccessCallback;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MasterAggregateHandler {
    private static final String TAG = "MasterAggregateHandler";
    private static volatile MasterAggregateHandler INSTANCE = null;

    private MasterAggregateHandler() {
    }

    public static MasterAggregateHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (MasterAggregateHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MasterAggregateHandler();
                }
            }
        }
        return INSTANCE;
    }

    MasterAggregatesDAO masterAggregatesDAO = new MasterAggregatesDAO();

    public void editBankBalance(double balance, MasterAggregate masterAggregate, String documentId, ResultSuccessCallback<MasterAggregate> successCallback, ResultFailureCallback failedCallback) {
        if (masterAggregate == null || documentId == null)
            failedCallback.onError("Master Aggregate has not been fetched yet", null);
        else {
            masterAggregate.setBalance(balance);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference masterAggRef = db.collection(DatabaseSchemaEnum.MASTER_AGGREGATE.getName());
            db.runTransaction(transaction -> {
                        masterAggRef.document(documentId).set(masterAggregate);
                        return null;
                    })
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Transaction complete");
                        successCallback.onResult(masterAggregate, documentId);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error adding document", e);
                        failedCallback.onError("Error updating master aggregate", e);
                    });
        }
    }
}

