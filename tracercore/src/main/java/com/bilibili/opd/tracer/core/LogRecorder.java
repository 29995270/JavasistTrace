package com.bilibili.opd.tracer.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Keep;
import android.util.Log;

import com.bilibili.opd.tracer.core.store.LogDBOpenHelper;

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
    private static LogDBOpenHelper openHelper;
    private SQLiteDatabase database;
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
            long startTime = System.currentTimeMillis();
            if (openHelper == null) {
                openHelper = new LogDBOpenHelper(context, LogDBOpenHelper.DB_NAME, null, LogDBOpenHelper.VERSION);
                database = openHelper.getWritableDatabase();
            }

            database.beginTransaction();
            try {
                for (TraceObj traceObj : buffer) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(LogDBOpenHelper.COLUMN_T_NAME, traceObj.threadName);
                    contentValues.put(LogDBOpenHelper.COLUMN_TIME, String.valueOf(traceObj.timeStamp));
                    contentValues.put(LogDBOpenHelper.COLUMN_METHOD, traceObj.methodSignature);
                    contentValues.put(LogDBOpenHelper.COLUMN_PARAMS, traceObj.paramStatement);
                    database.insert(LogDBOpenHelper.TABLE_NAME, null, contentValues);
                    traceObj.recycle();
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            buffer.clear();
            Log.e("AAA", "db duration:" + (System.currentTimeMillis() - startTime));
        }
    }
}
