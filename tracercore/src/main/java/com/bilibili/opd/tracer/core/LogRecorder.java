package com.bilibili.opd.tracer.core;

import android.content.Context;
import android.support.annotation.Keep;
import android.util.Log;

import com.bilibili.opd.tracer.core.store.DBLogStorage;
import com.bilibili.opd.tracer.core.store.LogStorage;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by wq on 2018/4/8.
 */

@Keep
public class LogRecorder {

    private final Context context;
    private static LogRecorder INSTANCE;
    private final LogStorage logStorage;

    private ConcurrentLinkedQueue<TraceObj> queue = new ConcurrentLinkedQueue<>();
    private AtomicInteger wip = new AtomicInteger(0);

    public static void init(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LogRecorder(context);
        }
    }

    public static LogRecorder getInstance() {
        return INSTANCE;
    }

    private LogRecorder(Context context) {
        this.context = context.getApplicationContext();
        logStorage = new DBLogStorage(context);
        Observable.interval(10, 10, TimeUnit.SECONDS, Schedulers.io())
                .onBackpressureDrop()
                .subscribe((along) -> {
                    long startTime = System.currentTimeMillis();
                    TraceObj obj;
                    while ((obj = queue.poll()) != null) {
                        logStorage.collectStore(obj);
                    }
                    logStorage.flush();
                    Log.e("AAA", "db duration:" + (System.currentTimeMillis() - startTime));
                }, throwable -> {

                });
    }

    public boolean isEnable() {
        return true;
    }

    public void enqueue(boolean isStatic, String methodSignature, String singleParam) {
        long time = System.nanoTime();
        String name = Thread.currentThread().getName();
        queue.offer(TraceObj.obtain(isStatic, time, name, methodSignature, singleParam));
//        enqueue(TraceObj.obtain(isStatic, time, name, methodSignature, singleParam));
    }

//    private void enqueue(MethodTraceObj traceObj) {
//        queue.offer(traceObj);
//        if (wip.getAndIncrement() == 0) {
//            do {
//                MethodTraceObj poll = queue.poll();
//                executorService.execute(() -> write(poll));
//            } while (wip.decrementAndGet() != 0);
//        }
//    }

//    private void enqueue(TraceObj traceObj) {
//        if (wip.compareAndSet(0, 1)) {
//            collect(traceObj);
//            if (wip.decrementAndGet() == 0) {
//                return;
//            }
//        } else {
//            queue.offer(traceObj);
//            if (wip.getAndIncrement() != 0) {
//                return;
//            }
//        }
//        do {
//            TraceObj poll = queue.poll();
//            collect(poll);
//        } while (wip.decrementAndGet() != 0);
//    }

//    private void enqueue(MethodTraceObj traceObj) {
//        queue.offer(traceObj);
//        if (wip.getAndIncrement() == 0) {
//            do {
//                 wip.set(1);
//                MethodTraceObj obj;
//                while ((obj = queue.poll()) != null) {
//                    collect(obj);
//                }
//            } while (wip.decrementAndGet() != 0);
//        }
//    }
}
