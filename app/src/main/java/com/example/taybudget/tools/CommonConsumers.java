package com.example.taybudget.tools;

import com.example.taybudget.data.handler.model.Tracker;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.function.BiConsumer;

public class CommonConsumers {

    public static BiConsumer<Tracker, QueryDocumentSnapshot> TRACKER_ID_SETTER = (tracker, document) -> tracker.setId(document.getId());
}
