package com.example.taybudget.data.firestore;

import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.SmsPatternChangelog;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.SmsPattern;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.example.taybudget.enums.DevUsers;
import com.example.taybudget.tools.CommonUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SmsPatternChangelogDAO {

    private static final String TAG = "SmsPatternChangelogDAO";

    public void getChangelogAfter(long millis, ResultSuccessCallback<List<SmsPatternChangelog>> resultSuccessCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(DatabaseSchemaEnum.SMS_PATTERN_CHANGELOG.getName())
                .whereGreaterThan("timestamp", millis)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<SmsPatternChangelog> list = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            SmsPatternChangelog changelog = document.toObject(SmsPatternChangelog.class);
                            list.add(changelog);
                        }
                        resultSuccessCallback.onResult(list, null);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        failureCallback.onError("Error in fetching sms pattern changelog", task.getException());
                    }
                });
    }
}
