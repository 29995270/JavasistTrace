package com.bilibili.opd.tracer.core.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.bilibili.opd.tracer.core.TraceObj;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wq on 2018/4/13.
 */
public class DBLogStorage implements LogStorage {

    private LogDBOpenHelper openHelper;
    private SQLiteDatabase database;
    private Context context;
    private ArrayList<TraceObj> buffer = new ArrayList<>(50);


    public DBLogStorage(Context context) {
        this.context = context;
    }

    @Override
    public void store(List<TraceObj> logs) {
        if (openHelper == null) {
            openHelper = new LogDBOpenHelper(context, LogDBOpenHelper.DB_NAME, null, LogDBOpenHelper.VERSION);
            database = openHelper.getWritableDatabase();
        }

        database.beginTransaction();
        try {
            for (TraceObj traceObj : logs) {
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
    }

    @Override
    public void collectStore(TraceObj obj) {
        buffer.add(obj);
        if (buffer.size() >= 50) {
            store(buffer);
            buffer.clear();
        }
    }

    @Override
    public void flush() {
        if (buffer.size() != 0) {
            store(buffer);
            buffer.clear();
        }
    }
}
