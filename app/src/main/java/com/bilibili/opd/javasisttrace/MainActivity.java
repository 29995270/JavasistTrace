package com.bilibili.opd.javasisttrace;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.os.TraceCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.bilibili.opd.tracer.core.annotation.TraceField;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by wq on 2018/3/8.
 */

public class MainActivity extends AppCompatActivity {

    private Random random = new Random();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.root)
                .setOnClickListener((view) -> {
                    TraceCompat.beginSection("onClick");
                    publicMethod(new OutBean(random.nextInt(100), random.nextInt(100)));
                    TraceCompat.endSection();
                });
        findViewById(R.id.root)
                .setOnDragListener((v, event) -> false);

        staticMethod(getWindowManager());
        publicMethod(new Bean());
        publicMethod(new OutBean(12, 21));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
//            publicMethod(new OutBean(random.nextInt(100), random.nextInt(100)));
//            publicMethod();
            publicMethod(new OutBean(123, 2314));
        }
        Log.e("AAA", "duration: " + (System.currentTimeMillis() - start));

        listMethod(Collections.emptyList());
    }

    @Override
    protected void onResume() {
        super.onResume();
        publicMethod(new Bean());
        listMethod(Collections.emptyList());
    }

    private static void staticMethod(WindowManager windowManager) {
        System.out.println("AAA");
    }

    public void publicMethod(Bean b) {
        System.out.println(1);
        System.out.println(1);
    }

    public void publicMethod() {
        System.out.println(1);
        System.out.println(1);
    }

    public void publicMethod(OutBean b) {
        System.out.println(b.getFieldA());
        System.out.println(b.getFieldB());
    }

    public void listMethod(List<OutBean> list) {
        System.out.println(list.size());
        System.out.println(list.size());
    }

    public void donotTrace(String a) {
        System.out.println(a);
    }


    public static class Bean {
        public int getAnInt() {
            return anInt;
        }

        public void setAnInt(int anInt) {
            this.anInt = anInt;
        }

        @TraceField
        private int anInt;
    }
}
