package com.example.taybudget.data.firestore;

import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.SmsPatternChangelog;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.SmsPattern;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.example.taybudget.enums.DevUsers;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsPatternDAO {
    private static final String TAG = "SmsPatternDAO";

    public void getSmsPatterns(ResultSuccessCallback<HashMap<String, SmsPattern>> successCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseSchemaEnum.SMS_PATTERNS.getName())
                .get()
                .addOnCompleteListener(task -> {
                    HashMap<String, SmsPattern> smsPatterns = new HashMap<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            smsPatterns.put(document.getId(), document.toObject(SmsPattern.class));
                        }
                        successCallback.onResult(smsPatterns, null);
                    } else {
                        Log.w(TAG, "Error getting master aggregates.", task.getException());
                        failureCallback.onError("Exception in getting master aggregates", task.getException());
                    }
                });
    }

    public void updateSmsPatterns(SmsPatternChangelog changelog, ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference ref = db.collection(DatabaseSchemaEnum.SMS_PATTERNS.getName());
        CollectionReference refChangelog = db.collection(DatabaseSchemaEnum.SMS_PATTERN_CHANGELOG.getName());

        db.runTransaction(transaction -> {
                    changelog.getNewPatterns().forEach(smsPattern -> {
                        DocumentReference newDoc = ref.document();
                        transaction.set(newDoc, smsPattern);
                    });
                    changelog.getUpdatePatterns().forEach((docId, smsPattern) -> {
                        ref.document(docId).set(smsPattern);
                    });
                    changelog.getRemovePatterns().forEach((docId, smsPattern) -> {
                        ref.document(docId).delete();
                    });
                    DocumentReference newChangelogDoc = refChangelog.document();
                    transaction.set(newChangelogDoc, changelog);

                    return null;
                }).addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transaction complete");
                    successCallback.onResult("Successful", null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    failureCallback.onError("Could not add update sms patterns", e);
                });
    }
}
