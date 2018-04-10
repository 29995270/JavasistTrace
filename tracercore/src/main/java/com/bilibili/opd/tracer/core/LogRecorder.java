package com.bilibili.opd.tracer.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.annotation.Keep;

import java.io.File;
import java.util.ArrayList;
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
    private ArrayList<MethodTraceObj> buffer = new ArrayList<>(30);
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

    /**
     * params is empty
     */
    public void enqueue(boolean isStatic, String methodSignature) {
        enqueue(isStatic, methodSignature, null);
    }

    public void enqueue(boolean isStatic, String methodSignature, String singleParam) {
        long time = System.nanoTime();
        String name = Thread.currentThread().getName();
        MethodTraceObj traceObj;
        if (singleParam == null) {
            traceObj = new MethodTraceObj(isStatic, time, name, methodSignature);
        } else {
            traceObj = new MethodTraceObj(isStatic, time, name, methodSignature, singleParam);
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

    private void enqueue(MethodTraceObj traceObj) {
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
            MethodTraceObj poll = queue.poll();
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

    private void collect(MethodTraceObj obj) {
        buffer.add(obj);
        if (buffer.size() >= 30) {
            //flash
//            if (database == null) {
//                File directory = Environment.getDataDirectory();
//                if (!directory.exists()) {
//                    directory.mkdirs();
//                }
//                database = SQLiteDatabase.openOrCreateDatabase(Environment.getDataDirectory().getAbsolutePath() + File.separator + "bltrace.db", null);
//                database.execSQL("CREATE TABLE IF NOT EXISTS method (_ID INTEGER PRIMARY KEY , STATIC INTEGER, T_NAME TEXT, TIME TEXT, METHOD_SIG TEXT, ARGS TEXT);");
//            }
//            database.beginTransaction();
            for (MethodTraceObj traceObj : buffer) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("STATIC", traceObj.isStatic ? 1 : 0);
                contentValues.put("T_NAME", traceObj.threadName);
                contentValues.put("TIME", String.valueOf(traceObj.timeStamp));
                contentValues.put("METHOD_SIG", traceObj.methodSignature);
                if (traceObj.singleParamStatement != null) {
                    contentValues.put("ARGS", traceObj.singleParamStatement);
                } else if (traceObj.multiParamStatements != null) {
                    StringBuilder builder = new StringBuilder();
                    for (String statement : traceObj.multiParamStatements) {
                        builder.append(statement);
                        builder.append(",");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    contentValues.put("ARGS", builder.toString());
                }
//                database.insert("method", null, contentValues);
            }
//            database.endTransaction();
            buffer.clear();
        }
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
    }
}
