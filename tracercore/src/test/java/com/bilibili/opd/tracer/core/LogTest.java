package com.bilibili.opd.tracer.core;

import org.junit.Test;

/**
 * Created by wq on 2018/4/16.
 */
public class LogTest {
    @Test
    public void methodTraceSwitcher() {
        MethodTraceSwitcher switcher = new MethodTraceSwitcher();

        switcher.disable(8);
        switcher.disable(0);
        switcher.disable(7);
        switcher.disable(13);

        assert !switcher.isEnable(8);
        assert !switcher.isEnable(0);
        assert !switcher.isEnable(7);
        assert !switcher.isEnable(13);
        assert switcher.isEnable(14);
        assert switcher.isEnable(6);
        assert switcher.isEnable(2);
    }
}
