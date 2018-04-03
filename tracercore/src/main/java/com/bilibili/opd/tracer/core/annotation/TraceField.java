package com.bilibili.opd.tracer.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wq on 2018/4/3.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface TraceField {
}
