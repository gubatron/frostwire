package com.frostwire.android.python;


public class PythonBridge {
    public static native int square(int n);

    static {
        System.loadLibrary("python_bridge");
    }
}
