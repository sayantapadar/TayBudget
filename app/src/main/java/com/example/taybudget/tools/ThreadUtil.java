package com.example.taybudget.tools;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class ThreadUtil {
    private static final Map<Long, Thread> activeThreads = new HashMap<>();

    public static void startNewThread(String tag, Runnable runnable) {
        Thread thread = new Thread(() -> {
            Log.d(tag, "Thread start");
            runnable.run();
            Log.d(tag, "Thread stop");
            activeThreads.remove(Thread.currentThread().getId());
        }, tag);
        activeThreads.put(thread.getId(), thread);
        thread.start();
    }
}
