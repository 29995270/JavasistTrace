package com.bilibili.opd.tracer.core;

import android.content.Context;
import android.support.annotation.Keep;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wq on 2018/4/8.
 */

@Keep
public class LogRecorder {

    private final Context context;
    private static LogRecorder INSTANCE;

    private ConcurrentLinkedQueue<TraceObj> queue = new ConcurrentLinkedQueue<>();
    private AtomicInteger wip = new AtomicInteger(0);
    private ArrayList<TraceObj> buffer = new ArrayList<>(50);
//    private SQLiteDatabase database;

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
    }

    public boolean isEnable() {
        return true;
    }

    /**
     * params is empty
     */
    public void enqueue(boolean isStatic, String methodSignature) {
        enqueue(isStatic, methodSignature, null);
    }

    public void enqueue(boolean isStatic, String methodSignature, String singleParam) {
        long time = System.nanoTime();
        String name = Thread.currentThread().getName();
        enqueue(TraceObj.obtain(isStatic, time, name, methodSignature, singleParam));
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

    private void enqueue(TraceObj traceObj) {
        if (wip.compareAndSet(0, 1)) {
            collect(traceObj);
            if (wip.decrementAndGet() == 0) {
                return;
            }
        } else {
            queue.offer(traceObj);
            if (wip.getAndIncrement() != 0) {
                return;
            }
        }
        do {
            TraceObj poll = queue.poll();
            collect(poll);
        } while (wip.decrementAndGet() != 0);
    }

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

    private void collect(TraceObj obj) {
        buffer.add(obj);
        if (buffer.size() >= 50) {
            for (TraceObj traceObj : buffer) {
                traceObj.recycle();
            }
            buffer.clear();
        }
    }
}
