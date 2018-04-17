package com.bilibili.opd.tracer.core;

import android.content.Context;
import android.support.annotation.Keep;
import android.util.Log;
import android.util.SparseIntArray;

import com.bilibili.opd.tracer.core.store.DBLogStorage;
import com.bilibili.opd.tracer.core.store.LogStorage;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by wq on 2018/4/8.
 */

@Keep
public class LogRecorder implements Recorder {

    private final Context context;
//    private static Recorder INSTANCE = (Recorder) Proxy.newProxyInstance(LogRecorder.class.getClassLoader(), new Class[]{Recorder.class}, new InvocationHandler() {
//        @Override
//        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//            if (method.getReturnType() == boolean.class) {
//                return false;
//            }
//            return null;
//        }
//    });

    private static Recorder EMPTY = new Recorder() {
        @Override
        public boolean isEnable(int methodIndex) {
            return false;
        }

        @Override
        public void enqueue(int methodIndex, boolean isStatic, String methodSignature, String singleParam) {/*do nothing*/}
    };

    private static Recorder INSTANCE = EMPTY;

    private final LogStorage logStorage;

    private final MethodTraceSwitcher methodTraceSwitcher;

    private ConcurrentLinkedQueue<TraceObj> queue = new ConcurrentLinkedQueue<>();
    private AtomicInteger wip = new AtomicInteger(0);
    private final LogFrequencyStatistic frequencyStatistic;

    public static void init(Context context) {
        if (INSTANCE == EMPTY) {
            INSTANCE = new LogRecorder(context);
        }
    }

    public static Recorder getInstance() {
        return INSTANCE;
    }

    public MethodTraceSwitcher getMethodTraceSwitcher() {
        return methodTraceSwitcher;
    }

    private LogRecorder(Context context) {
        this.context = context.getApplicationContext();
        logStorage = new DBLogStorage(context);
        methodTraceSwitcher = new MethodTraceSwitcher();
        frequencyStatistic = new LogFrequencyStatistic();
        Observable.interval(10, 10, TimeUnit.SECONDS, Schedulers.io())
                .onBackpressureDrop()
                .subscribe((along) -> {
                    long startTime = System.currentTimeMillis();
                    frequencyStatistic.start();
                    TraceObj obj;
                    while ((obj = queue.poll()) != null) {
                        frequencyStatistic.input(obj);
                        logStorage.collectStore(obj);
                    }
                    frequencyStatistic.stop();
                    logStorage.flush();
                    Log.e("AAA", "db duration:" + (System.currentTimeMillis() - startTime));
                }, throwable -> {
                    // TODO: 2018/4/16
                });
    }

    @Override
    public boolean isEnable(int methodIndex) {
        return methodTraceSwitcher.isEnable(methodIndex) /*&& globalEnable*/;
    }

    @Override
    public void enqueue(int methodIndex, boolean isStatic, String methodSignature, String singleParam) {
        long time = System.nanoTime();
        String name = Thread.currentThread().getName();
        queue.offer(TraceObj.obtain(methodIndex, isStatic, time, name, methodSignature, singleParam));
//        enqueue(TraceObj.obtain(methodIndex, isStatic, time, name, methodSignature, singleParam));
    }

//    private void enqueue(MethodTraceObj traceObj) {
//        queue.offer(traceObj);
//        if (wip.getAndIncrement() == 0) {
//            do {
//                MethodTraceObj poll = queue.poll();
//                executorService.execute(() -> write(poll));
//            } while (wip.decrementAndGet() != 0);
//        }
//    }

//    private void enqueue(TraceObj traceObj) {
//        if (wip.compareAndSet(0, 1)) {
//            collect(traceObj);
//            if (wip.decrementAndGet() == 0) {
//                return;
//            }
//        } else {
//            queue.offer(traceObj);
//            if (wip.getAndIncrement() != 0) {
//                return;
//            }
//        }
//        do {
//            TraceObj poll = queue.poll();
//            collect(poll);
//        } while (wip.decrementAndGet() != 0);
//    }

//    private void enqueue(MethodTraceObj traceObj) {
//        queue.offer(traceObj);
//        if (wip.getAndIncrement() == 0) {
//            do {
//                 wip.set(1);
//                MethodTraceObj obj;
//                while ((obj = queue.poll()) != null) {
//                    collect(obj);
//                }
//            } while (wip.decrementAndGet() != 0);
//        }
//    }

    /**
     * Created by wq on 2018/4/13.
     */
    class LogFrequencyStatistic {

        private SparseIntArray countMap = new SparseIntArray();
        private int groupCount = 0;

        void start() {
            countMap.clear();
            groupCount = 0;
        }

        void input(TraceObj obj) {
            groupCount++;
            countMap.put(obj.methodIndex, countMap.get(obj.methodIndex) + 1);
        }

        void stop() {
            if (3000 <= groupCount) return;
            for (int i = 0; i < countMap.size(); i++) {
                if (countMap.valueAt(i) > 1000) {
                    methodTraceSwitcher.disable(countMap.keyAt(i));
                }
            }
        }
    }
}
