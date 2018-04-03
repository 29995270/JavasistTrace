package com.bilibili.opd.javasisttrace;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.bilibili.opd.tracer.core.annotation.TraceField;

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
                .setOnClickListener((view) -> publicMethod(new OutBean(random.nextInt(100), random.nextInt(100))));
        findViewById(R.id.root)
                .setOnDragListener((v, event) -> false);

        staticMethod(getWindowManager());
        publicMethod(new Bean());
        publicMethod(new OutBean(12, 21));
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
        System.out.println(1);
    }

    public void publicMethod(OutBean b) {
        System.out.println(1);
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

        @TraceField
        private int anInt;
    }
}
