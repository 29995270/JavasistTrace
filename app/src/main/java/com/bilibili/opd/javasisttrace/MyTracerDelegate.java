package com.bilibili.opd.javasisttrace;

import android.support.annotation.Keep;

import java.lang.reflect.Method;

/**
 * Created by wq on 2018/4/16.
 */
@Keep
public class MyTracerDelegate {

    static {
        try {
            Class<?> recorderClass = Class.forName("com.bilibili.opd.tracer.core.LogRecorder");
            recorderClazz = recorderClass;
            instanceMethod = recorderClass.getDeclaredMethod("getInstance");
            isEnableMethod = recorderClass.getDeclaredMethod("isEnable", int.class);
            enqueueMethod = recorderClass.getDeclaredMethod("enqueue", int.class, boolean.class, String.class, String.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private static Method isEnableMethod;
    private static Method enqueueMethod;
    private static Method instanceMethod;
    private static Class<?> recorderClazz;

    public static boolean isEnable(int index) {
        try {
            return (boolean)(isEnableMethod.invoke(instanceMethod.invoke(recorderClazz), index));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void enqueue(int methodIndex, boolean isStatic, String methodSignature, String singleParam) {
        try {
            enqueueMethod.invoke(instanceMethod.invoke(recorderClazz), methodIndex, isStatic, methodSignature, singleParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
