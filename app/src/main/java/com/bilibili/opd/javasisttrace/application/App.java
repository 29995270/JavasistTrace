package com.bilibili.opd.javasisttrace.application;

import android.app.Application;

import com.bilibili.opd.tracer.core.LogRecorder;

/**
 * Created by wq on 2018/4/8.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogRecorder.init(this);
    }
}
