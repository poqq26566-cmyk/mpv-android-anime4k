package com.github.catvod;

/**
 * TVBox 代理端口管理
 * 与 FongMi 的 com.github.catvod.Proxy 保持一致的 API：
 * - set(int)     设置端口
 * - getPort()    获取端口
 * - getUrl()     生成代理 URL
 *
 * 注意：JAR 内部的 com.github.catvod.spider.Proxy 是独立的类，
 * 其端口值无法通过本类设置。对于 JAR Spider 返回的含 :−1 端口的代理 URL，
 * 需在 getPlayerUrl() 中做兜底字符串替换。
 */
public class Proxy {

    private static int port = -1;

    public static void set(int port) {
        Proxy.port = port;
    }

    public static int getPort() {
        return port;
    }

    public static String getUrl(boolean local) {
        return "http://127.0.0.1:" + getPort() + "/proxy";
    }
}
