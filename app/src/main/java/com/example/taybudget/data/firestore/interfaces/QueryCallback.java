package com.example.taybudget.data.firestore.interfaces;

public interface QueryCallback {
    public void onSuccess();
    public void onFailure(Exception e);
}
