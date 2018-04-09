package com.bilibili.opd.tracer.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.annotation.Keep;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wq on 2018/4/8.
 */

@Keep
public class LogRecorder {

    private final Context context;
    private static LogRecorder INSTANCE;

    private ConcurrentLinkedQueue<MethodTraceObj> queue = new ConcurrentLinkedQueue<>();
    private AtomicInteger wip = new AtomicInteger(0);
    private static ExecutorService executorService;

    public static void init(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LogRecorder(context);
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
    }

    public static LogRecorder getInstance() {
        return INSTANCE;
    }

    private LogRecorder(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * params is empty
     */
    public void enqueue(boolean isStatic, String methodSignature) {
        enqueue(isStatic, methodSignature, null, null);
    }

    /**
     * single param statement
     */
    public void enqueue(boolean isStatic, String methodSignature, String singleParam) {
        enqueue(isStatic, methodSignature, singleParam, null);
    }

    /**
     * multi param statements
     */
    public void enqueue(boolean isStatic, String methodSignature, String[] paramExps) {
        enqueue(isStatic, methodSignature, null, paramExps);
    }

    private void enqueue(boolean isStatic, String methodSignature, String singleParam, String[] paramExps) {
        long time = System.nanoTime();
        String name = Thread.currentThread().getName();
        MethodTraceObj traceObj;
        if (singleParam == null && paramExps == null) {
            traceObj = new MethodTraceObj(isStatic, time, name, methodSignature);
        } else if (paramExps == null) {
            traceObj = new MethodTraceObj(isStatic, time, name, methodSignature, singleParam);
        } else {
            traceObj = new MethodTraceObj(isStatic, time, name, methodSignature, paramExps);
        }
        enqueue(traceObj);
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

//    private void enqueue(MethodTraceObj traceObj) {
//        if (wip.compareAndSet(0, 1)) {
//            executorService.execute(() -> write(traceObj));
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
//            MethodTraceObj poll = queue.poll();
//            executorService.execute(() -> write(traceObj));
//        } while (wip.decrementAndGet() != 0);
//    }

    private void enqueue(MethodTraceObj traceObj) {
        queue.offer(traceObj);
        if (wip.getAndIncrement() == 0) {
            do {
                 wip.set(1);
                MethodTraceObj obj;
                while ((obj = queue.poll()) != null) {
                    MethodTraceObj finalObj = obj;
                    executorService.execute(() -> write(finalObj));
                }
            } while (wip.decrementAndGet() != 0);
        }
    }

    private void write(MethodTraceObj traceObj) {
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(Environment.getDataDirectory().getAbsolutePath() + File.separator + "bltrace.db", null);
        database.execSQL("create table IF NOT EXISTS \"method\" (\"ID\" INTEGER PRIMARY KEY , \"STATIC\" INTEGER, \"T_NAME\" TEXT, \"TIME\" TEXT, \"METHOD_SIG\" TEXT, \"ARGS\" TEXT);");
    }

    public static class MethodTraceObj {
        public boolean isStatic;
        public long timeStamp;
        public String threadName;
        public String methodSignature;
        public String singleParamStatement;
        public String[] multiParamStatements;

        public MethodTraceObj(boolean isStatic, long timeStamp, String threadName, String methodSignature) {
            this.isStatic = isStatic;
            this.timeStamp = timeStamp;
            this.threadName = threadName;
            this.methodSignature = methodSignature;
        }

        public MethodTraceObj(boolean isStatic, long timeStamp, String threadName, String methodSignature, String singleParamStatement) {
            this(isStatic, timeStamp, threadName, methodSignature);
            this.singleParamStatement = singleParamStatement;
        }

        public MethodTraceObj(boolean isStatic, long timeStamp, String threadName, String methodSignature, String[] multiParamStatements) {
            this(isStatic, timeStamp, threadName, methodSignature);
            this.multiParamStatements = multiParamStatements;
        }
    }
}