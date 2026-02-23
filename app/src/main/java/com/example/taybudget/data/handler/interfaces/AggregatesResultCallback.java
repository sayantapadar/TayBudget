package com.example.taybudget.data.handler.interfaces;

import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;

public interface AggregatesResultCallback {
    void getResult(String masterDocId, MasterAggregate masterAggregate, String monthDocId, MonthlyAggregate monthlyAggregate);
}
