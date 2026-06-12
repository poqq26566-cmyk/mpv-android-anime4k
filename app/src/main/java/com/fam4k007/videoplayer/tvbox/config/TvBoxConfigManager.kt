package com.fam4k007.videoplayer.tvbox.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.fam4k007.videoplayer.tvbox.crawler.JarLoader
import com.fam4k007.videoplayer.tvbox.server.TvBoxProxyServer
import com.github.catvod.Proxy
import com.github.catvod.crawler.Spider
import com.github.catvod.crawler.SpiderNull
import com.fam4k007.videoplayer.tvbox.model.ParseBean
import com.fam4k007.videoplayer.tvbox.model.SourceBean
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.IDN
import java.util.concurrent.TimeUnit

/**
 * TVBox 配置管理器
 *
 * 负责：
 * 1. 管理配置接口 URL 列表（增删切换）
 * 2. 下载并解析 JSON 配置
 * 3. 下载并加载 Spider JAR
 * 4. 提供 Spider 实例的获取入口
 */
class TvBoxConfigManager(private val context: Context) {

    companion object {
        private const val TAG = "TvBoxConfigManager"
        private const val PREFS_NAME = "tvbox_config"
        private const val KEY_CONFIG_URLS = "config_urls"
        private const val KEY_ACTIVE_URL = "active_url"
        private const val KEY_CACHED_CONFIG = "cached_config_"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val jarLoader = JarLoader(context)

    /** 所有已加载的 Spider（用于代理服务器） */
    val allSpiders: Map<String, Spider>
        get() = jarLoader.getAllSpiders()

    // 当前活跃配置
    var currentSites: List<SourceBean> = emptyList()
        private set
    var currentParses: List<ParseBean> = emptyList()
        private set
    var currentVipFlags: List<String> = emptyList()
        private set
    var isConfigLoaded: Boolean = false
        private set

    /**
     * 获取已保存的配置 URL 列表
     */
    fun getSavedUrls(): List<String> {
        val json = prefs.getString(KEY_CONFIG_URLS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取当前活跃的配置 URL
     */
    fun getActiveUrl(): String {
        return prefs.getString(KEY_ACTIVE_URL, "") ?: ""
    }

    /**
     * 添加配置 URL
     */
    fun addUrl(url: String) {
        val urls = getSavedUrls().toMutableList()
        if (!urls.contains(url)) {
            urls.add(url)
            prefs.edit().putString(KEY_CONFIG_URLS, gson.toJson(urls)).apply()
        }
    }

    /**
     * 删除配置 URL
     */
    fun removeUrl(url: String) {
        val urls = getSavedUrls().toMutableList()
        urls.remove(url)
        prefs.edit().putString(KEY_CONFIG_URLS, gson.toJson(urls)).apply()
        if (getActiveUrl() == url) {
            prefs.edit().putString(KEY_ACTIVE_URL, "").apply()
        }
    }

    /**
     * 加载配置（下载 JSON → 解析 → 下载 JAR → 加载）
     * @param url 配置接口 URL
     * @return 加载结果（成功返回站点列表，失败返回错误信息）
     */
    suspend fun loadConfig(url: String): Result<List<SourceBean>> = withContext(Dispatchers.IO) {
        try {
            // 确保 URL 有 scheme（自动补全 http://）
            val fullUrl = getFullConfigUrl(url)

            // 1. 下载 JSON 配置
            val jsonStr = downloadConfig(fullUrl)

            // 2. 解析配置
            val config = ConfigParser.parse(jsonStr)

            // 3. 保存到缓存
            prefs.edit()
                .putString(KEY_CACHED_CONFIG + JarLoader.string2MD5(fullUrl), jsonStr)
                .apply()

            // 4. 先启动代理服务器，再加载 JAR（JAR 初始化时会探测端口）
            ensureProxyServer()

            // 5. 下载并加载 Spider JAR（如果有）
            if (config.spiderUrl.isNotEmpty()) {
                val cleanUrl = config.spiderUrl.replace("img+", "")
                val parts = cleanUrl.split(";md5;")
                val jarUrl = encodeIdnUrl(parts[0])
                val md5 = if (parts.size > 1) parts[1].trim() else ""

                val classLoader = jarLoader.loadRemoteJar(jarUrl, md5)
                if (classLoader == null) {
                    Log.w(TAG, "主 JAR 加载失败，部分站点可能不可用")
                } else {
                    // 注册为主 JAR，使无自定义 JAR 的站点能通过 "main" key 找到
                    jarLoader.registerAsMain(jarUrl)
                    Log.i(TAG, "主 JAR 加载成功")
                }
                // JAR 初始化可能覆盖了端口，JAR 加载后重新设置所有 Proxy 类
                proxyServer?.let { notifyProxyPort(it.serverPort) }
            }

            // 6. 更新状态
            currentSites = config.sites.filter { it.searchable }
            currentParses = config.parses
            currentVipFlags = config.vipFlags
            isConfigLoaded = true

            // 7. 确保代理服务器运行并设置端口（最终确认）
            ensureProxyServer()
            proxyServer?.let { server ->
                notifyProxyPort(server.serverPort)
                // 延迟重试：部分 JAR 的 Init.init() 会异步重置端口，2 秒后再设一次
                kotlinx.coroutines.delay(2000)
                notifyProxyPort(server.serverPort)
                Log.i(TAG, "延迟重设代理端口: ${server.serverPort}, 验证=${Proxy.getPort()}")
            }

            // 8. 设置为活跃 URL
            addUrl(fullUrl)
            prefs.edit().putString(KEY_ACTIVE_URL, fullUrl).apply()

            Result.success(currentSites)
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 获取 Spider 实例
     * 每次获取前自动恢复代理端口，防止 JAR 初始化异步重置
     */
    fun getSpider(sourceBean: SourceBean): Spider {
        // 确保代理端口正确（JAR 的 Init.init() 可能异步重置了端口）
        proxyServer?.let { server ->
            if (Proxy.getPort() != server.serverPort) {
                Log.w(TAG, "代理端口被重置: ${Proxy.getPort()} → ${server.serverPort}")
                notifyProxyPort(server.serverPort)
            }
        }
        return try {
            jarLoader.getSpider(
                sourceBean.key,
                sourceBean.api,
                sourceBean.ext,
                sourceBean.jar
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取 Spider 失败: ${sourceBean.key}", e)
            SpiderNull()
        }
    }

    /**
     * 将包含 IDN（国际化域名，如中文域名）的 URL 转换为 Punycode 格式
     *
     * OkHttp 的 HttpUrl 解析器在处理包含非 ASCII 字符的域名时可能抛出异常，
     * 这里提前手动转换，确保 OkHttp 能正常解析。
     *
     * 例如: https://tv.菜妮丝.top → https://tv.xn--yhqu5zs87a.top
     */
    private fun encodeIdnUrl(url: String): String {
        return try {
            val schemeEnd = url.indexOf("://")
            if (schemeEnd == -1) return url

            val hostStart = schemeEnd + 3
            // 找到域名结束位置（路径开始或字符串结束）
            val pathStart = url.indexOf('/', hostStart).let { if (it == -1) url.length else it }
            // 检查端口号
            val portIndex = url.indexOf(':', hostStart)
            val hostEnd = if (portIndex != -1 && portIndex < pathStart) portIndex else pathStart

            val host = url.substring(hostStart, hostEnd)

            // 如果域名包含非 ASCII 字符，转换为 Punycode
            if (host.any { it.code > 127 }) {
                val punycodeHost = IDN.toASCII(host)
                url.replaceRange(hostStart, hostEnd, punycodeHost)
            } else {
                url
            }
        } catch (e: Exception) {
            // 转换失败时使用原始 URL
            url
        }
    }

    @Volatile
    private var proxyServer: TvBoxProxyServer? = null

    /**
     * 确保代理服务器已启动
     * 从端口 9978 开始尝试，用于处理 Spider 返回的代理 URL
     */
    private fun ensureProxyServer() {
        if (proxyServer?.isAlive == true) return

        var port = 9978
        while (port < 9999) {
            try {
                val server = TvBoxProxyServer(port, this)
                server.start()
                proxyServer = server
                // 通知所有 Proxy 类代理端口
                notifyProxyPort(port)
                Log.i(TAG, "代理服务器已启动: port=$port")
                return
            } catch (e: Exception) {
                port++
            }
        }
        Log.w(TAG, "无法启动代理服务器（端口 9978-9998 均被占用）")
    }

    /**
     * 通知所有 Proxy 类当前代理端口
     * 设置 app 内的 com.github.catvod.Proxy（与 FongMi 的 Server.start() 一致）
     *
     * 注意：JAR 内的 com.github.catvod.spider.Proxy 是独立的类，端口无法通过此方法设置。
     * 对于 JAR Spider 返回的含 :−1 端口的代理 URL，在 getPlayerUrl() 中做兜底字符串替换。
     */
    private fun notifyProxyPort(port: Int) {
        Proxy.set(port)
        Log.d(TAG, "代理端口已设置: port=$port, Proxy.getPort()=${Proxy.getPort()}")
    }

    /**
     * 确保代理端口正确（JAR 初始化可能异步重置了端口）
     * 应在每次调用 Spider 关键方法前调用
     */
    fun ensureProxyPort() {
        proxyServer?.let { server ->
            if (Proxy.getPort() != server.serverPort) {
                Log.w(TAG, "代理端口被重置: ${Proxy.getPort()} → ${server.serverPort}")
                notifyProxyPort(server.serverPort)
            }
        }
    }

    /**
     * 停止代理服务器
     */
    fun stopProxyServer() {
        proxyServer?.stop()
        proxyServer = null
        Proxy.set(-1)
    }

    /**
     * 下载配置 JSON
     */
    private fun downloadConfig(url: String): String {
        // 处理 IDN 域名（中文域名 → Punycode），防止 OkHttp 解析失败
        val encodedUrl = encodeIdnUrl(url)

        // 使用 TVBox / OkHttp 常见的 User-Agent，部分服务器根据 UA 决定返回 JSON 还是 HTML
        val request = Request.Builder()
            .url(encodedUrl)
            .header("User-Agent", "okhttp/4.12.0")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful || response.body == null) {
            throw RuntimeException("下载配置失败: HTTP ${response.code}")
        }
        val body = response.body!!.string()

        // 基本校验：返回内容必须看起来像 JSON
        val trimmed = body.trimStart()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            throw RuntimeException("返回内容不是 JSON 配置，请检查地址是否正确")
        }

        return body
    }

    /**
     * 获取完整的配置地址
     * 如果 URL 没有 scheme，自动添加 http://
     */
    private fun getFullConfigUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return trimmed
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("clan://")) {
            "http://$trimmed"
        } else {
            trimmed
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        jarLoader.clear()
        isConfigLoaded = false
        currentSites = emptyList()
        currentParses = emptyList()
    }
}
