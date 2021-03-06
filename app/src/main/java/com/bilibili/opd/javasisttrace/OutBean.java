package com.bilibili.opd.javasisttrace;

import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.IntegerRes;
import android.support.annotation.StringDef;

import com.bilibili.opd.tracer.core.annotation.TraceField;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wq on 2018/4/3.
 */
public class OutBean {

    public static final int A = 1;
    public static final int B = 2;
    public static final int C = 3;
    public static final int D = 4;

    @IntDef({A, B, C, D})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ParamsType {
    }

    @ParamsType
    @TraceField
    private int fieldA;
    @TraceField
    private int fieldB;

    private long xxxid;

    private int wwwwId;

    private List<String> stringList = null;

    private String[] arry = new String[2];

    private Map<String, String> map = new HashMap<>();

    private boolean aBoolean = false;

    private String aa = "fasdfasdf";

    private byte aByte = 1;

    private double aDouble = 23d;

    private float aFloat = 23.4f;

    public OutBean(int fieldA, int fieldB) {
        this.fieldA = fieldA;
        this.fieldB = fieldB;
    }

    public int getFieldA() {
        return fieldA;
    }

    public void setFieldA(int fieldA) {
        this.fieldA = fieldA;
    }

    public int getFieldB() {
        return fieldB;
    }

    public void setFieldB(int fieldB) {
        this.fieldB = fieldB;
    }
}
