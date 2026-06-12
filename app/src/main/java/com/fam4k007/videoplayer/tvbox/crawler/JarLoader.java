package com.fam4k007.videoplayer.tvbox.crawler;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import androidx.annotation.NonNull;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.lang.reflect.Method;
import java.net.IDN;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * TVBox JAR 动态加载器
 * 移植自 TVBoxOS (com.github.catvod.crawler.JarLoader)
 *
 * 通过 DexClassLoader 加载外部 JAR 包中的 Spider 实现类，
 * 实现运行时动态扩展数据源。
 */
public class JarLoader {
    private static final String TAG = "JarLoader";

    private final ConcurrentHashMap<String, DexClassLoader> loaders;
    private final ConcurrentHashMap<String, Method> methods;
    private final ConcurrentHashMap<String, Spider> spiders;
    private volatile String recentJarKey = "";

    private final Context context;
    private final OkHttpClient httpClient;

    public JarLoader(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        this.loaders = new ConcurrentHashMap<>();
        this.methods = new ConcurrentHashMap<>();
        this.spiders = new ConcurrentHashMap<>();
        // 确保全局 Context 可用，JAR 的 native 解密库通过 com.github.catvod.Init.context() 获取
        com.github.catvod.Init.set(this.context);
    }

    /**
     * 加载主 JAR 文件
     * @param jarPath JAR 文件本地路径
     * @return 是否加载成功
     */
    public boolean load(String jarPath) {
        recentJarKey = "main";
        return loadClassLoader(jarPath, recentJarKey);
    }

    /**
     * 设置最近使用的 JAR Key
     */
    public void setRecentJarKey(String key) {
        if (key != null && !key.isEmpty()) {
            recentJarKey = key;
        }
    }

    /**
     * 清除所有已加载的 Spider 和 ClassLoader
     */
    public void clear() {
        for (Spider sp : spiders.values()) {
            try {
                sp.destroy();
            } catch (Exception e) {
                Log.e(TAG, "销毁 Spider 失败: " + e.getMessage());
            }
        }
        spiders.clear();
        methods.clear();
        loaders.clear();
    }

    /**
     * 通过 DexClassLoader 加载 JAR
     * 使用标准父优先委托（与 FongMi 一致），确保 JAR 内的 Spider
     * 通过父加载器访问 app 的 com.github.catvod.Proxy（端口已设置）。
     */
    private boolean loadClassLoader(String jarPath, String key) {
        if (loaders.containsKey(key)) return true;

        boolean success = false;
        try {
            File jarDir = new File(jarPath).getParentFile();
            if (jarDir == null || !jarDir.exists()) return false;

            // 标准 DexClassLoader（父优先委托），与 FongMi 的 JarLoader 一致
            String cachePath = jarDir.getAbsolutePath();
            final DexClassLoader classLoader = new DexClassLoader(
                    jarPath,
                    cachePath,
                    cachePath,
                    context.getClassLoader()
            );

            // 初始化 JAR 的 com.github.catvod.spider.Init
            int retryCount = 0;
            do {
                try {
                    final Class<?> classInit = classLoader.loadClass("com.github.catvod.spider.Init");
                    if (classInit != null) {
                        final Method initMethod = classInit.getMethod("init", Context.class);
                        initMethod.invoke(classInit, context);
                        Log.i(TAG, "Init.init() 调用成功");
                        success = true;
                        break;
                    }
                    Thread.sleep(200);
                } catch (Throwable th) {
                    Log.e(TAG, "加载 Init 类失败: " + th.getMessage());
                }
                retryCount++;
            } while (retryCount < 2);

            if (success) {
                // 设置 JAR 内的 com.github.catvod.Init（如果有）
                try {
                    Class<?> catvodInit = classLoader.loadClass("com.github.catvod.Init");
                    Method setMethod = catvodInit.getMethod("set", Context.class);
                    setMethod.invoke(null, context);
                    Log.i(TAG, "com.github.catvod.Init.set() 已通过 DexClassLoader 设置");
                } catch (ClassNotFoundException e) {
                    // JAR 中没有 com.github.catvod.Init，使用应用自带的版本
                } catch (Throwable th) {
                    Log.w(TAG, "设置 JAR 内 Init 失败: " + th.getMessage());
                }

                // 存储 JAR 内 com.github.catvod.spider.Proxy 的 proxy(Map) 方法
                // 用于处理 /proxy 请求（与 FongMi 的 invokeProxy 一致）
                invokeProxy(key, classLoader);

                loaders.put(key, classLoader);
            }
        } catch (Throwable th) {
            Log.e(TAG, "loadClassLoader 失败: " + th.getMessage());
        }
        return success;
    }

    /**
     * 获取 JAR 内 com.github.catvod.spider.Proxy.proxy(Map) 方法并缓存
     * 与 FongMi 的 JarLoader.invokeProxy() 一致
     */
    private void invokeProxy(String key, DexClassLoader loader) {
        try {
            Class<?> clz = loader.loadClass("com.github.catvod.spider.Proxy");
            Method method = clz.getMethod("proxy", Map.class);
            methods.put(key, method);
            Log.i(TAG, "invokeProxy: 已缓存 spider.Proxy.proxy(Map) method, key=" + key);
        } catch (Throwable e) {
            Log.d(TAG, "invokeProxy: JAR 无 spider.Proxy 类或无 proxy 方法: " + e.getMessage());
        }
    }

    /**
     * 下载并加载远程 JAR
     * @param jarUrl JAR 的远程 URL
     * @param md5    预期的 MD5 值（可为空）
     * @return DexClassLoader 实例，失败返回 null
     */
    public DexClassLoader loadRemoteJar(String jarUrl, String md5) {
        String jarKey = string2MD5(jarUrl);

        // 检查缓存
        if (loaders.containsKey(jarKey)) {
            return loaders.get(jarKey);
        }

        // Android 14+ 禁止从 files/ 加载可写 dex 文件，需使用 cache/ 目录
        File cacheDir = new File(context.getCacheDir().getAbsolutePath() + "/tvbox_csp");
        if (!cacheDir.exists()) cacheDir.mkdirs();

        File cacheFile = new File(cacheDir, jarKey + ".jar");

        // 如果缓存文件存在且 MD5 匹配，直接加载
        if (!md5.isEmpty() && cacheFile.exists()) {
            if (getFileMd5(cacheFile).equalsIgnoreCase(md5)) {
                cacheFile.setReadOnly(); // Android 14+ 要求 dex 文件只读
                if (loadClassLoader(cacheFile.getAbsolutePath(), jarKey)) {
                    return loaders.get(jarKey);
                }
            }
        } else if (cacheFile.exists()) {
            // 无 MD5 校验，尝试使用缓存
            cacheFile.setReadOnly(); // Android 14+ 要求 dex 文件只读
            if (loadClassLoader(cacheFile.getAbsolutePath(), jarKey)) {
                return loaders.get(jarKey);
            }
        }

        // 下载 JAR
        try {
            // 如果缓存文件存在且为只读，删除后重新下载
            if (cacheFile.exists()) {
                if (!cacheFile.canWrite()) {
                    cacheFile.setWritable(true);
                }
                cacheFile.delete();
            }

            // 处理 IDN 域名（中文域名 → Punycode），防止 OkHttp 解析失败
            String encodedJarUrl = encodeIdnUrl(jarUrl);
            Request request = new Request.Builder()
                    .url(encodedJarUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "下载 JAR 失败: HTTP " + response.code());
                    return null;
                }

                InputStream is = response.body().byteStream();
                OutputStream os = new FileOutputStream(cacheFile);
                try {
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                    os.flush();
                } finally {
                    is.close();
                    os.close();
                }
            }

            // Android 14+ 禁止从可写目录加载 dex 文件，下载后设为只读
            cacheFile.setReadOnly();

            if (loadClassLoader(cacheFile.getAbsolutePath(), jarKey)) {
                return loaders.get(jarKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "下载 JAR 异常: " + e.getMessage());
        }
        return null;
    }

    /**
     * 将已加载的 JAR 注册为 "main" 主 JAR
     *
     * loadRemoteJar() 以 URL 的 MD5 为 key 存储，但站点无自定义 jar 时按 "main" 查找。
     * 加载主 spider JAR 后需调用此方法注册。
     */
    public void registerAsMain(String jarUrl) {
        String jarKey = string2MD5(jarUrl);
        DexClassLoader cl = loaders.get(jarKey);
        if (cl != null) {
            loaders.put("main", cl);
            Log.i(TAG, "主 JAR 已注册为 main");
        }
    }

    /**
     * 获取 Spider 实例
     * @param key 站点 key
     * @param cls 爬虫类名（如 "csp_BiliBili" 或 "com.github.catvod.spider.BiliBili"）
     * @param ext 扩展配置
     * @param jar 自定义 JAR 地址（空则使用主 JAR）
     * @return Spider 实例
     */
    public Spider getSpider(String key, String cls, String ext, String jar) {
        if (spiders.containsKey(key)) {
            return spiders.get(key);
        }

        // 提取类名（去掉 "csp_" 前缀和包名）
        String clsKey = cls.replace("csp_", "");
        if (clsKey.contains(".")) {
            // 如果包含包名，取最后一段
            clsKey = clsKey.substring(clsKey.lastIndexOf('.') + 1);
        }

        String jarUrl = "", jarMd5 = "", jarKey;
        if (jar == null || jar.isEmpty()) {
            jarKey = "main";
        } else {
            String[] urls = jar.split(";md5;");
            jarUrl = urls[0];
            jarKey = string2MD5(jarUrl);
            jarMd5 = urls.length > 1 ? urls[1].trim() : "";
        }

        recentJarKey = jarKey;

        DexClassLoader classLoader;
        if (jarKey.equals("main")) {
            classLoader = loaders.get("main");
        } else {
            classLoader = loadRemoteJar(jarUrl, jarMd5);
        }

        if (classLoader == null) {
            Log.w(TAG, "ClassLoader 为空，返回 SpiderNull: key=" + key);
            return new SpiderNull();
        }

        try {
            Spider sp = (Spider) classLoader.loadClass("com.github.catvod.spider." + clsKey).newInstance();
            sp.siteKey = key;
            sp.init(context, ext);
            spiders.put(key, sp);
            return sp;
        } catch (Throwable th) {
            Log.e(TAG, "实例化 Spider 失败: cls=" + clsKey + ", error=" + th.getMessage());
        }

        return new SpiderNull();
    }

    /**
     * 销毁指定 Spider
     */
    public void destroySpider(String key) {
        Spider sp = spiders.remove(key);
        if (sp != null) {
            try {
                sp.destroy();
            } catch (Exception e) {
                Log.e(TAG, "销毁 Spider 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 获取所有已加载的 Spider 实例
     */
    public Map<String, Spider> getAllSpiders() {
        return new java.util.HashMap<>(spiders);
    }

    /**
     * 从主 JAR 的 ClassLoader 加载指定类
     * 用于通过反射设置 JAR 内部类
     */
    public Class<?> loadClass(String className) {
        DexClassLoader mainLoader = loaders.get("main");
        if (mainLoader == null) return null;
        try {
            return mainLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 调用 JAR 内 spider.Proxy.proxy(Map) 处理代理请求
     * 与 FongMi 的 JarLoader.proxy() 一致：
     * 优先使用最近加载的 JAR，失败则遍历其他 JAR。
     *
     * @param params 代理请求参数（URL query + headers + POST body）
     * @return Object[] { statusCode, contentType, inputStream[, headers] }，失败返回 null
     */
    public Object[] proxy(Map<String, String> params) {
        Method method = (recentJarKey != null) ? methods.get(recentJarKey) : null;
        Object[] result = proxyInvoke(method, params);
        if (result != null) return result;
        return tryOtherProxyMethods(params);
    }

    private Object[] tryOtherProxyMethods(Map<String, String> params) {
        for (Map.Entry<String, Method> entry : methods.entrySet()) {
            if (entry.getKey().equals(recentJarKey)) continue;
            Object[] result = proxyInvoke(entry.getValue(), params);
            if (result != null) return result;
        }
        return null;
    }

    private Object[] proxyInvoke(Method method, Map<String, String> params) {
        try {
            return method == null ? null : (Object[]) method.invoke(null, params);
        } catch (Throwable e) {
            Log.d(TAG, "proxyInvoke 失败: " + e.getMessage());
            return null;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 将包含 IDN（国际化域名，如中文域名）的 URL 转换为 Punycode 格式
     * 例如: https://tv.菜妮丝.top/jar/fty0528.jar → https://tv.xn--yhqu5zs87a.top/jar/fty0528.jar
     */
    public static String encodeIdnUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        try {
            int schemeEnd = url.indexOf("://");
            if (schemeEnd == -1) return url;

            int hostStart = schemeEnd + 3;
            int pathStart = url.indexOf('/', hostStart);
            if (pathStart == -1) pathStart = url.length();
            int portIndex = url.indexOf(':', hostStart);
            int hostEnd = (portIndex != -1 && portIndex < pathStart) ? portIndex : pathStart;

            String host = url.substring(hostStart, hostEnd);

            // 检查域名是否包含非 ASCII 字符
            boolean hasNonAscii = false;
            for (int i = 0; i < host.length(); i++) {
                if (host.charAt(i) > 127) {
                    hasNonAscii = true;
                    break;
                }
            }

            if (hasNonAscii) {
                String punycodeHost = IDN.toASCII(host);
                return url.substring(0, hostStart) + punycodeHost + url.substring(hostEnd);
            }
            return url;
        } catch (Exception e) {
            Log.e(TAG, "IDN URL 转换失败: " + e.getMessage());
            return url;
        }
    }

    /**
     * 计算字符串的 MD5
     */
    public static String string2MD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * 计算文件的 MD5
     */
    public static String getFileMd5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = new java.io.FileInputStream(file);
            try {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    md.update(buffer, 0, length);
                }
            } finally {
                is.close();
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
