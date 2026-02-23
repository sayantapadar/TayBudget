package com.example.taybudget.data.firestore;

import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.model.Tracker;
import com.example.taybudget.enums.DatabaseSchemaEnum;
import com.example.taybudget.enums.DevUsers;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TrackerDAO {

    public void addTracker(Tracker tracker, ResultSuccessCallback<Tracker> successCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tracker").add(tracker).addOnSuccessListener(documentReference -> {
            tracker.setId(documentReference.getId());
            successCallback.onResult(tracker, documentReference.getId());
        }).addOnFailureListener(e -> {
            Log.w("TrackerDAO", "Error adding tracker", e);
            failureCallback.onError("Error in adding tracker", e);
        });

        // TODO revisit savepoints, this should change themselves
    }

    public Task<QuerySnapshot> getTrackersTask() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("tracker")
                .whereEqualTo("user", DevUsers.TAY.getId())
                .whereGreaterThan("stopDate", Calendar.getInstance().getTime())
                .get();
    }

    public void getTrackers(ResultSuccessCallback<List<Tracker>> successCallback, ResultFailureCallback failureCallback) {
        getTrackersTask().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Tracker> trackers = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Log.d("TrackerDAO", document.getId() + " => " + document.getData());
                    Tracker tracker = document.toObject(Tracker.class);
                    tracker.setId(document.getId());
                    trackers.add(tracker);
                }
                successCallback.onResult(trackers, null);
            } else {
                Log.w("TrackerDAO", "Error getting trackers", task.getException());
                failureCallback.onError("Error in getting trackers", task.getException());
            }
        }).addOnFailureListener(e -> {
            Log.w("TrackerDAO", "Error getting trackers", e);
            failureCallback.onError("Error in getting trackers", e);
        });
    }

    public void stopTracker(Tracker tracker, ResultSuccessCallback<Tracker> successCallback, ResultFailureCallback failureCallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        tracker.setStopDate(Calendar.getInstance(Locale.getDefault()).getTime());
        db.collection(DatabaseSchemaEnum.TRACKER.getName()).document(tracker.getId()).set(tracker)
                .addOnSuccessListener(aVoid -> successCallback.onResult(tracker, null))
                .addOnFailureListener(e -> failureCallback.onError("Error in stopping tracker", e));
    }
}
