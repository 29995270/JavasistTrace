package com.bilibili.opd.tracer.core;

/**
 * Created by wq on 2018/4/16.
 */
public interface Recorder {

    boolean isEnable(int methodIndex);

    void enqueue(int methodIndex, boolean isStatic, String methodSignature, String singleParam);
}
