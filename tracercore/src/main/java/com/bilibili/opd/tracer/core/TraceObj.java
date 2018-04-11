package com.bilibili.opd.tracer.core;

import android.support.annotation.NonNull;
import android.support.v4.util.Pools;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TraceObj {

    private static final Pool<TraceObj> sPool = new Pool<>(1000);

    public boolean isStatic;
    public long timeStamp;
    public String threadName;
    public String methodSignature;
    public String paramStatement;

    public TraceObj(boolean isStatic, long timeStamp, String threadName, String methodSignature, String paramStatement) {
        this.isStatic = isStatic;
        this.timeStamp = timeStamp;
        this.threadName = threadName;
        this.methodSignature = methodSignature;
        this.paramStatement = paramStatement;
    }

    @Override
    public String toString() {
        return isStatic +
                "}{" + timeStamp +
                "}{" + threadName +
                "}{" + methodSignature +
                "}{" + paramStatement;
    }

    public void recycle() {
        sPool.release(this);
    }

    public static TraceObj obtain(boolean isStatic, long timeStamp, String threadName, String methodSignature, String paramStatement) {
        TraceObj instance = sPool.acquire();
        if (instance != null) {
            instance.isStatic = isStatic;
            instance.timeStamp = timeStamp;
            instance.threadName = threadName;
            instance.methodSignature = methodSignature;
            instance.paramStatement = paramStatement;
            return instance;
        } else {
            return new TraceObj(isStatic, timeStamp, threadName, methodSignature, paramStatement);
        }
    }

    static class Pool<T> implements Pools.Pool<T> {

        private ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
        private int maxPoolSize;

        Pool(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        @Override
        public T acquire() {
            return queue.poll();
        }

        @Override
        public boolean release(@NonNull T instance) {
            return queue.size() < maxPoolSize && queue.offer(instance);
        }
    }
}