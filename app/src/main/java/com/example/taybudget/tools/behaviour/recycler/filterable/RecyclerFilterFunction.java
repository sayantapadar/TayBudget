package com.example.taybudget.tools.behaviour.recycler.filterable;

public interface RecyclerFilterFunction<E> {
    boolean filter(E object, String filterString);
}
