package com.github.catvod;

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * TVBox 全局上下文持有者
 * JAR 中的 Init.init() 和 native 解密库依赖此类的 context() 方法获取应用上下文
 */
public class Init {

    private WeakReference<Context> context;

    private static Init get() {
        return Loader.INSTANCE;
    }

    public static void set(Context context) {
        get().context = new WeakReference<>(context);
    }

    public static Context context() {
        return get().context.get();
    }

    private static class Loader {
        static volatile Init INSTANCE = new Init();
    }
}
