package com.bilibili.opd.javasisttrace.application;

import android.app.Application;

import com.bilibili.opd.javasisttrace.XXXXAdapter;
import com.bilibili.opd.tracer.core.LogRecorder;
import com.facebook.stetho.Stetho;

/**
 * Created by wq on 2018/4/8.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogRecorder.getInstance().isEnable(123);
        Stetho.initializeWithDefaults(this);
        LogRecorder.init(this);
        int itemCount = new XXXXAdapter().getItemCount();
        System.out.print(itemCount);
    }
}
