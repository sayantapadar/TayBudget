package com.example.taybudget.data.handler.model;

public class BiParameterizedObject<A,B>{
    private A first;
    private B second;

    public BiParameterizedObject(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }
}
