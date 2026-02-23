package com.example.taybudget.data.handler.builders.query;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class QueryBuilder<T> {
    // addQuery(): CallBack<T>, T.class
    // execute(): Future

    private final int poolSize;
    private final List<String> tags;
    private final List<Runnable> callables;
    private final ExecutorService executorService;

    public QueryBuilder(int poolSize, List<String> tags, List<Runnable> callables) {
        this.poolSize = poolSize;
        this.tags = tags;
        this.callables = callables;
        executorService = Executors.newFixedThreadPool(poolSize);
    }

    public List<String> getTags() {
        return tags;
    }

    public List<Runnable> getCallables() {
        return callables;
    }

    private void execute() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            Runnable task = callables.get(i);
            Log.d("QueryBuilder", "Executing " + tag);
            futures.add(CompletableFuture.runAsync(task));
        }

        // TODO incomplete
    }

    public int getPoolSize() {
        return poolSize;
    }

    static class QueryTask<T> {
        private final List<String> tags;
        private final List<Runnable> callables;
        private final int poolSize;

        public QueryTask(int poolSize) {
            this.tags = new ArrayList<>();
            this.callables = new ArrayList<>();
            this.poolSize = poolSize;
        }

        public void addQuery(String tag, Runnable query) {
            this.tags.add(tag);
            this.callables.add(query);
        }

        public void execute() {
            new QueryBuilder<>(poolSize, tags, callables).execute();
        }
    }
}
