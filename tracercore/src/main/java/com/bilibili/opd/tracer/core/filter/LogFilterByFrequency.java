package com.bilibili.opd.tracer.core.filter;

import com.bilibili.opd.tracer.core.TraceObj;

/**
 * Created by wq on 2018/4/13.
 */
public class LogFilterByFrequency implements LogFilter {
    @Override
    public boolean shouldStore(TraceObj obj) {
        return false;
    }
}
