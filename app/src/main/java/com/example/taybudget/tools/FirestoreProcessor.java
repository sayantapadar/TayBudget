package com.example.taybudget.tools;

import android.util.Log;

import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.model.BiParameterizedObject;
import com.example.taybudget.data.handler.model.Tracker;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import kotlin.Function;

public class FirestoreProcessor<E> {

    public List<E> getList(Task<QuerySnapshot> querySnapshotTask, BiConsumer<E, QueryDocumentSnapshot> consumer, ResultFailureCallback failureCallback, Class<E> eClass) {
        try {
            QuerySnapshot result = Tasks.await(querySnapshotTask);
            return getList(result, consumer, eClass);
        } catch (Exception e) {
            Log.w("FirestoreProcessor", "Error getting objects", e);
            failureCallback.onError("Error in getting objects", e);
            return null;
        }
    }

    private List<E> getList(QuerySnapshot result, BiConsumer<E, QueryDocumentSnapshot> consumer, Class<E> eClass) {
        List<E> objects = new ArrayList<>();
        for (QueryDocumentSnapshot document : result) {
            E object = document.toObject(eClass);
            if (consumer != null)
                consumer.accept(object, document);
            objects.add(object);
        }
        return objects;
    }

    public void getListAsync(Task<QuerySnapshot> querySnapshotTask, BiConsumer<E, QueryDocumentSnapshot> consumer, ResultSuccessCallback<List<E>> successCallback, ResultFailureCallback failureCallback, Class<E> eClass) {
        querySnapshotTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                successCallback.onResult(getList(task.getResult(), consumer, eClass), null);
            } else {
                Log.w("FirestoreProcessor", "Error getting objects", task.getException());
                failureCallback.onError("Error in getting objects", task.getException());
            }
        }).addOnFailureListener(e -> {
            Log.w("FirestoreProcessor", "Error getting objects", e);
            failureCallback.onError("Error in getting objects", e);
        });
    }


    public BiParameterizedObject<E, String> getObject(Task<QuerySnapshot> querySnapshotTask, BiConsumer<E, QueryDocumentSnapshot> consumer, ResultFailureCallback failureCallback, Class<E> eClass) {
        try {
            QuerySnapshot result = Tasks.await(querySnapshotTask);
            return getObject(result, consumer, eClass);
        } catch (Exception e) {
            Log.w("FirestoreProcessor", "Error getting object", e);
            failureCallback.onError("Error in getting object", e);
            return null;
        }
    }

    private BiParameterizedObject<E, String> getObject(QuerySnapshot result, BiConsumer<E, QueryDocumentSnapshot> consumer, Class<E> eClass) {
        if (result.isEmpty())
            return null;

        return new BiParameterizedObject<>(result.getDocuments().get(0).toObject(eClass), result.getDocuments().get(0).getId());
    }

    public void getObjectAsync(Task<QuerySnapshot> querySnapshotTask, BiConsumer<E, QueryDocumentSnapshot> consumer, ResultSuccessCallback<BiParameterizedObject<E, String>> successCallback, ResultFailureCallback failureCallback, Class<E> eClass) {
        querySnapshotTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                successCallback.onResult(getObject(task.getResult(), consumer, eClass), null);
            } else {
                Log.w("FirestoreProcessor", "Error getting object", task.getException());
                failureCallback.onError("Error in getting object", task.getException());
            }
        }).addOnFailureListener(e -> {
            Log.w("FirestoreProcessor", "Error getting object", e);
            failureCallback.onError("Error in getting object", e);
        });
    }
}

    