/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.froad.ukey.utils.np;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Global executor pools for the whole application.
 * <p>
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
public class AppExecutors {

    enum executorType{
        MainThread,
        DiskIO,
        Single,
        Scheduled
    }

    private final MainThreadExecutor mMainThread;
    private final Executor mDiskIO;
    private final ScheduledExecutorService mScheduledExecutorService;

    private AppExecutors(MainThreadExecutor mainThread, Executor diskIO, ScheduledExecutorService scheduledExecutorService) {
        mMainThread = mainThread;
        mDiskIO = diskIO;
        mScheduledExecutorService = scheduledExecutorService;
    }

    private AppExecutors() {
        this(new MainThreadExecutor(), Executors.newSingleThreadExecutor(), Executors.newScheduledThreadPool(3));
    }

    private static AppExecutors appExecutors;

    public static AppExecutors getAppExecutors() {
        if (appExecutors== null){
            synchronized (AppExecutors.class){
                if (appExecutors == null){
                    appExecutors = new AppExecutors();
                }
            }
        }
        return appExecutors;
    }

    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }

        public void executeDelayed(@NonNull Runnable command, long delayMillis) {
            mainThreadHandler.postDelayed(command,delayMillis);
        }
    }

    public void postDiskIOThread(Runnable runnable){
        mDiskIO.execute(runnable);
    }


    public void postMainThread(Runnable runnable){
        mMainThread.execute(runnable);
    }
    public void postMainThreadDelayed(Runnable runnable, long delayMillis){
        mMainThread.executeDelayed(runnable,delayMillis);
    }
    public void postScheduledExecutorThread(Runnable runnable){
        mScheduledExecutorService.schedule(runnable,0, TimeUnit.MILLISECONDS);
    }
    public void postScheduledAtFixedRate(Runnable runnable, long initDelay , long period){
        mScheduledExecutorService.scheduleAtFixedRate(runnable,initDelay,period, TimeUnit.MILLISECONDS);
    }
    public void postScheduledThreadDelayed(Runnable runnable, long delayMillis){
        mScheduledExecutorService.schedule(runnable,delayMillis, TimeUnit.MILLISECONDS);
    }

}
