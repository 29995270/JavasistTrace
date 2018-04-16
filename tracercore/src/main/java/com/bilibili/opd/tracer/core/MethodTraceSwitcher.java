package com.bilibili.opd.tracer.core;

public class MethodTraceSwitcher {
    private byte[] bitmap;
    private int byteLen = 30000;
    private int bitmapMaxLen = byteLen << 3;

    private int[] mask = {
            0x1,
            0x2,
            0x4,
            0x8,
            0x16,
            0x32,
            0x64,
            0x128
    };

    public boolean isEnable(int methodIndex) {
        return bitmap == null
//                || methodIndex < 0
//                || bitmapMaxLen <= methodIndex
                || (bitmap[methodIndex >> 3] & mask[(methodIndex & 0x7)]) == 0;
    }

    public void disable(int methodIndex) {
        if (bitmap == null) {
            bitmap = new byte[byteLen];
        }
        int index = methodIndex >> 3;
        bitmap[index] = (byte) (bitmap[index] | mask[(methodIndex % 8)]);
    }
}