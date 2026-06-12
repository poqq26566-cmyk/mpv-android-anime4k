package com.github.catvod.crawler;

import android.util.Log;

/**
 * TVBox Spider 调试日志类
 * JAR 加密代码中引用了此类，必须存在
 */
public class SpiderDebug {

    private static final String TAG = "SpiderDebug";

    public static void log(Throwable th) {
        if (th != null) th.printStackTrace();
    }

    public static void log(String msg) {
        if (msg != null && !msg.isEmpty()) Log.d(TAG, msg);
    }

    public static void log(String tag, String msg, Object... args) {
        if (msg != null && !msg.isEmpty()) Log.d(tag, args.length > 0 ? String.format(msg, args) : msg);
    }
}
