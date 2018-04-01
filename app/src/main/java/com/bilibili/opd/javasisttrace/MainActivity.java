package com.bilibili.opd.javasisttrace;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

/**
 * Created by wq on 2018/3/8.
 */

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.root)
                .setOnClickListener((view) -> {
                    System.out.println("AAA");
                });
        findViewById(R.id.root)
                .setOnDragListener((v, event) -> false);

        staticMethod(getWindowManager());
        publicMethod(new Bean());
    }

    @Override
    protected void onResume() {
        super.onResume();
        publicMethod(new Bean());
    }

    private static void staticMethod(WindowManager windowManager) {
        System.out.println("AAA");
    }

    public void publicMethod(Bean b) {
        System.out.println(1);
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

        private int anInt;
    }
}
