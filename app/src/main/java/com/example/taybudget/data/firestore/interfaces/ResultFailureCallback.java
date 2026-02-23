package com.example.taybudget.data.firestore.interfaces;

public interface ResultFailureCallback {
    void onError(String message, Exception exception);
}
