package com.example.taybudget.data.handler.interfaces;

import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Income;

import java.util.List;

public interface RecurringDataCallback {
    void onResult(List<Income> incomes, List<Expense> expenses);
}
