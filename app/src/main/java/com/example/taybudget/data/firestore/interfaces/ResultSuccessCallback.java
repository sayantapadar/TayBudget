package com.example.taybudget.data.firestore.interfaces;

public interface ResultSuccessCallback<E> {
    void onResult(E result, String documentId);
}
