package com.bilibili.opd.javasisttrace;

import com.bilibili.opd.tracer.core.LogRecorder;
import com.bilibili.opd.tracer.core.Recorder;

/**
 * Created by wq on 2018/4/16.
 */
public class MyTracerDelegate implements Recorder{

    public static Recorder getInstance() {
        return LogRecorder.getInstance();
    }

    @Override
    public boolean isEnable(int methodIndex) {
        return LogRecorder.getInstance().isEnable(methodIndex);
    }

    @Override
    public void enqueue(int methodIndex, boolean isStatic, String methodSignature, String singleParam) {
        LogRecorder.getInstance().enqueue(methodIndex, isStatic, methodSignature, singleParam);
    }
}
