package com.example.taybudget.data.firestore.interfaces;

public interface ResultCallback<E> extends ResultSuccessCallback<E>, ResultFailureCallback{
    void onNoResultFound();
}
