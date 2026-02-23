package com.example.taybudget.tools.behaviour.recycler.filterable;

import com.example.taybudget.tools.GenericRecyclerAdapter;
import com.example.taybudget.tools.behaviour.recycler.InitializationFunction;

import java.util.List;

public interface FilterableInitializationFunction<E> extends InitializationFunction {
    void initialize(List<E> list, GenericRecyclerAdapter<E> adapter);
}
