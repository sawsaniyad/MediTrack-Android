package com.meditrack.app.data;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private final Executor mainThread = new MainThreadExecutor();

    private AppExecutors() {
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public void diskIO(Runnable runnable) {
        diskIO.execute(runnable);
    }

    public void mainThread(Runnable runnable) {
        mainThread.execute(runnable);
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
