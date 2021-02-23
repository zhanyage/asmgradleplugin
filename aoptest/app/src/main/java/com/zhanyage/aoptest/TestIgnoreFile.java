package com.zhanyage.aoptest;

public class TestIgnoreFile {
    public static void TestGc() {
        for (int i = 0; i < 10000; i ++) {
            TestInner();
        }
    }

    private static void TestInner() {
        int[] test = new int[10000];
    }
}
