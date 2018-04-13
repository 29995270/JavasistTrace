package com.bilibili.opd.tracer.core.store;

import com.bilibili.opd.tracer.core.TraceObj;

import java.util.List;

/**
 * Created by wq on 2018/4/13.
 */
public interface LogStorage {
    //存储一堆
    void store(List<TraceObj> logs);
    //一个个存
    void collectStore(TraceObj obj);
    //一次存储过程完成后调用的清理工作
    void flush();
}
