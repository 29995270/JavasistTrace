package com.bilibili.opd.tracer.core.store;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by wq on 2018/4/12.
 */
public class LogDBOpenHelper extends SQLiteOpenHelper {

    public static final int VERSION = 1;
    public static final String DB_NAME = "tracer.db";

    public static final String TABLE_NAME = "method_log";

    public static final String COLUMN_TIME = "TIME";
    public static final String COLUMN_T_NAME = "T_NAME";
    public static final String COLUMN_METHOD = "METHOD";
    public static final String COLUMN_PARAMS = "PARAMS";

    public LogDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS \"" + TABLE_NAME + "\" (\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT, \"" + COLUMN_TIME + "\" TEXT, \"" + COLUMN_T_NAME + "\" TEXT, \"" + COLUMN_METHOD + "\" TEXT, \"" + COLUMN_PARAMS + "\" TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
    }
}
