package com.fam4k007.videoplayer.tvbox.repository

import android.util.Log
import com.fam4k007.videoplayer.tvbox.config.TvBoxConfigManager
import com.fam4k007.videoplayer.tvbox.model.ParseBean
import com.fam4k007.videoplayer.tvbox.model.SourceBean
import com.fam4k007.videoplayer.tvbox.model.VodInfo
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.github.catvod.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * TVBox 数据仓库
 *
 * 封装 Spider 调用和 HTTP API 调用，提供统一的数据访问接口。
 * 根据 SourceBean.type 分发到不同的数据获取方式：
 * - TYPE_SPIDER(3): 调用 JAR 中的 Spider
 * - TYPE_JSON(1) / TYPE_API(4): HTTP GET 请求
 * - TYPE_XML(0): HTTP GET 请求 + XML 解析（暂不实现）
 */
class TvBoxRepository(private val configManager: TvBoxConfigManager) {

    companion object {
        private const val TAG = "TvBoxRepository"
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 加载配置
     */
    suspend fun loadConfig(url: String): Result<List<SourceBean>> {
        return configManager.loadConfig(url)
    }

    /**
     * 获取已加载的可搜索站点列表
     */
    fun getSites(): List<SourceBean> {
        return configManager.currentSites
    }

    /**
     * 搜索影片
     * @param source 站点
     * @param keyword 搜索关键词
     * @return 影片列表
     */
    suspend fun search(source: SourceBean, keyword: String): Result<List<VodInfo>> = withContext(Dispatchers.IO) {
        try {
            when (source.type) {
                SourceBean.TYPE_SPIDER -> searchBySpider(source, keyword)
                SourceBean.TYPE_JSON, SourceBean.TYPE_API -> searchByHttp(source, keyword)
                else -> Result.failure(UnsupportedOperationException("不支持的源类型: ${source.type}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "搜索失败: source=${source.key}, keyword=$keyword", e)
            Result.failure(e)
        }
    }

    /**
     * 通过 Spider 搜索
     */
    private fun searchBySpider(source: SourceBean, keyword: String): Result<List<VodInfo>> {
        val spider = configManager.getSpider(source)
        val jsonStr = spider.searchContent(keyword, false)

        if (jsonStr.isBlank()) {
            return Result.success(emptyList())
        }

        return try {
            val vodList = parseVodList(jsonStr, source)
            Result.success(vodList)
        } catch (e: Exception) {
            // 部分站点的 Spider 返回非标准 JSON（如 HTML、空字符串等），
            // 不影响其他站点搜索，记录日志后返回空列表
            Log.w(TAG, "站点 ${source.name} 解析搜索结果失败: ${e.message}")
            Result.success(emptyList())
        }
    }

    /**
     * 通过 HTTP GET 搜索（JSON API / 扩展 API）
     */
    private suspend fun searchByHttp(source: SourceBean, keyword: String): Result<List<VodInfo>> {
        return try {
            val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
            val url = "${source.api}?wd=$encodedKeyword&ac=detail"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return Result.failure(RuntimeException("HTTP ${response.code}"))
            }

            val jsonStr = response.body!!.string()
            val vodList = parseVodList(jsonStr, source)
            Result.success(vodList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解析影片列表（CMS JSON 标准格式）
     * 容错处理：部分站点返回非标准 JSON，捕获异常后返回空列表
     */
    private fun parseVodList(jsonStr: String, source: SourceBean): List<VodInfo> {
        val json = try {
            JsonParser.parseString(jsonStr).asJsonObject
        } catch (e: Exception) {
            Log.w(TAG, "非 JSON 响应 (${source.name}): ${jsonStr.take(100)}")
            return emptyList()
        }
        val list = mutableListOf<VodInfo>()

        if (json.has("list") && json.get("list").isJsonArray) {
            val listArray = json.getAsJsonArray("list")
            for (element in listArray) {
                try {
                    val obj = element.asJsonObject
                    val vod = VodInfo(
                        vodId = safeGetString(obj, "vod_id", ""),
                        vodName = safeGetString(obj, "vod_name", ""),
                        vodPic = safeGetString(obj, "vod_pic", ""),
                        vodRemarks = safeGetString(obj, "vod_remarks", ""),
                        typeName = safeGetString(obj, "type_name", ""),
                        vodYear = safeGetString(obj, "vod_year", ""),
                        vodArea = safeGetString(obj, "vod_area", ""),
                        vodActor = safeGetString(obj, "vod_actor", ""),
                        vodDirector = safeGetString(obj, "vod_director", ""),
                        vodContent = safeGetString(obj, "vod_content", ""),
                        vodPlayFrom = safeGetString(obj, "vod_play_from", ""),
                        vodPlayUrl = safeGetString(obj, "vod_play_url", ""),
                        sourceKey = source.key,
                        sourceName = source.name
                    )
                    if (vod.vodName.isNotBlank()) {
                        list.add(vod)
                    }
                } catch (e: Exception) {
                    // 跳过解析失败的影片
                }
            }
        }

        return list
    }

    /**
     * 获取影片详情（含播放线路/集数）
     * @param source 站点
     * @param vodId  影片 ID
     * @return 完整的 VodInfo（含 vodPlayFrom / vodPlayUrl）
     */
    suspend fun getDetail(
        source: SourceBean,
        vodId: String
    ): Result<VodInfo> = withContext(Dispatchers.IO) {
        try {
            if (source.type == SourceBean.TYPE_SPIDER) {
                val spider = configManager.getSpider(source)
                val jsonStr = spider.detailContent(listOf(vodId))

                Log.d(TAG, "detailContent response: ${jsonStr.take(500)}")

                if (jsonStr.isBlank()) {
                    return@withContext Result.failure(RuntimeException("详情为空"))
                }

                val json = try {
                    JsonParser.parseString(jsonStr).asJsonObject
                } catch (e: Exception) {
                    Log.e(TAG, "detailContent 返回非 JSON: $jsonStr", e)
                    return@withContext Result.failure(RuntimeException("详情数据格式错误: ${e.message}"))
                }

                val list = json.getAsJsonArray("list")
                if (list != null && list.size() > 0) {
                    val obj = list[0].asJsonObject
                    val vodPlayFrom = safeGetString(obj, "vod_play_from", "")
                    val vodPlayUrl = safeGetString(obj, "vod_play_url", "")
                    Log.d(TAG, "vod_play_from=$vodPlayFrom")
                    Log.d(TAG, "vod_play_url=${vodPlayUrl.take(300)}")
                    val vod = VodInfo(
                        vodId = safeGetString(obj, "vod_id", vodId),
                        vodName = safeGetString(obj, "vod_name", ""),
                        vodPic = safeGetString(obj, "vod_pic", ""),
                        vodRemarks = safeGetString(obj, "vod_remarks", ""),
                        typeName = safeGetString(obj, "type_name", ""),
                        vodYear = safeGetString(obj, "vod_year", ""),
                        vodArea = safeGetString(obj, "vod_area", ""),
                        vodActor = safeGetString(obj, "vod_actor", ""),
                        vodDirector = safeGetString(obj, "vod_director", ""),
                        vodContent = safeGetString(obj, "vod_content", ""),
                        vodPlayFrom = vodPlayFrom,
                        vodPlayUrl = vodPlayUrl,
                        sourceKey = source.key,
                        sourceName = source.name
                    )
                    Result.success(vod)
                } else {
                    Result.failure(RuntimeException("详情列表为空"))
                }
            } else {
                Result.failure(UnsupportedOperationException("仅支持 Spider 类型源的详情"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取详情失败: vodId=$vodId", e)
            Result.failure(e)
        }
    }

    /**
     * 获取播放地址
     * @param source 站点
     * @param flag   播放线路标识
     * @param id     播放 ID/地址
     * @return 播放 URL 和 Header
     */
    suspend fun getPlayerUrl(
        source: SourceBean,
        flag: String,
        id: String
    ): Result<PlayerResult> = withContext(Dispatchers.IO) {
        try {
            if (source.type == SourceBean.TYPE_SPIDER) {
                val spider = configManager.getSpider(source)
                configManager.ensureProxyPort()
                Log.d(TAG, "playerContent: flag=$flag, id=$id, proxyPort=${Proxy.getPort()}")
                val jsonStr = spider.playerContent(flag, id, configManager.currentVipFlags)
                Log.d(TAG, "playerContent response: ${jsonStr.take(500)}")

                if (jsonStr.isBlank()) {
                    return@withContext Result.failure(RuntimeException("获取播放地址失败"))
                }

                val json = try {
                    JsonParser.parseString(jsonStr).asJsonObject
                } catch (e: Exception) {
                    Log.e(TAG, "playerContent 返回非 JSON: $jsonStr", e)
                    return@withContext Result.failure(RuntimeException("播放地址解析失败: ${e.message}"))
                }

                val parse = safeGetInt(json, "parse", 0)
                val rawUrl = safeGetString(json, "url", "")
                val headerMap = mutableMapOf<String, String>()

                if (json.has("header") && json.get("header").isJsonObject) {
                    val headerObj = json.getAsJsonObject("header")
                    for (key in headerObj.keySet()) {
                        headerMap[key] = headerObj.get(key).asString
                    }
                }

                if (rawUrl.isBlank()) {
                    return@withContext Result.failure(RuntimeException("播放地址为空"))
                }

                // 兜底替换：JAR 内的 com.github.catvod.spider.Proxy 端口默认为 -1，
                // 生成的代理 URL 形如 http://127.0.0.1:-1/proxy?...，
                // 替换为实际的代理服务器端口。
                val url = fixProxyUrl(rawUrl)

                if (parse == 0) {
                    Result.success(PlayerResult.Direct(url, headerMap))
                } else {
                    // parse=1 表示需要通过解析接口二次解析（如夸克网盘等）
                    val parsed = tryParseUrl(url, headerMap)
                    if (parsed != null) {
                        Log.d(TAG, "二次解析成功: ${parsed.take(200)}")
                        Result.success(PlayerResult.Direct(fixProxyUrl(parsed), headerMap))
                    } else {
                        Log.w(TAG, "二次解析失败，返回原始 URL")
                        Result.success(PlayerResult.Direct(url, headerMap))
                    }
                }
            } else {
                Result.failure(UnsupportedOperationException("仅支持 Spider 类型源的播放"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取播放地址失败 flag=$flag id=$id", e)
            Result.failure(e)
        }
    }

    /**
     * 通过配置中的解析线路二次解析 URL
     *
     * 当 playerContent 返回 parse=1 时，URL 需要通过解析接口获取真实地址。
     * 遍历配置中的 parses 列表，依次尝试直到获得有效 URL。
     *
     * @param videoUrl 待解析的 URL
     * @param header   请求头（部分源需要特定 Referer/UA）
     * @return 解析后的真实 URL，失败返回 null
     */
    private fun tryParseUrl(videoUrl: String, header: Map<String, String>): String? {
        val parses = configManager.currentParses
        if (parses.isEmpty()) {
            Log.w(TAG, "没有可用的解析线路")
            return null
        }

        for (parseBean in parses) {
            try {
                val result = when (parseBean.type) {
                    ParseBean.TYPE_JSON -> parseWithJsonApi(parseBean, videoUrl, header)
                    else -> parseWithJsonApi(parseBean, videoUrl, header) // 默认也尝试 JSON 解析
                }
                if (result != null) return result
            } catch (e: Exception) {
                Log.w(TAG, "解析线路 ${parseBean.name} 失败: ${e.message}")
            }
        }
        return null
    }

    /**
     * 通过 JSON 解析接口获取真实 URL
     *
     * 标准 TVBox JSON 解析接口格式：
     * GET ${parseUrl}${videoUrl} → 返回 JSON: { "url": "https://real-url.mp4" }
     */
    private fun parseWithJsonApi(
        parseBean: ParseBean,
        videoUrl: String,
        header: Map<String, String>
    ): String? {
        val parseUrl = if (videoUrl.startsWith("http")) {
            "${parseBean.url}$videoUrl"
        } else {
            "${parseBean.url}${java.net.URLEncoder.encode(videoUrl, "UTF-8")}"
        }

        Log.d(TAG, "解析请求: $parseUrl")

        val requestBuilder = Request.Builder()
            .url(parseUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")

        // 传递原始 header（Referer 等）
        header.forEach { (key, value) ->
            if (key.lowercase() !in listOf("user-agent", "user_agent")) {
                requestBuilder.header(key, value)
            }
        }

        val response = httpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful || response.body == null) {
            Log.w(TAG, "解析接口 HTTP ${response.code}")
            return null
        }

        val jsonStr = response.body!!.string()
        Log.d(TAG, "解析响应: ${jsonStr.take(500)}")

        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject
            val url = safeGetString(json, "url", "")
            if (url.isNotBlank()) url else null
        } catch (e: Exception) {
            Log.w(TAG, "解析响应非 JSON: ${e.message}")
            null
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        configManager.release()
    }

    // ==================== 工具方法 ====================

    /**
     * 修复 JAR Spider 返回的代理 URL 中端口为 -1 的问题
     *
     * JAR 内的 com.github.catvod.spider.Proxy 是独立的类（与 app 的 com.github.catvod.Proxy 不同），
     * 其端口默认为 -1，导致生成的 URL 形如 http://127.0.0.1:-1/proxy?...
     * 此方法将 :-1 替换为实际的代理服务器端口。
     */
    private fun fixProxyUrl(url: String): String {
        val serverPort = Proxy.getPort()
        if (serverPort <= 0) return url
        return url.replace("127.0.0.1:-1", "127.0.0.1:$serverPort")
    }

    private fun safeGetString(json: JsonObject, key: String, default: String): String {
        return try {
            if (json.has(key) && !json.get(key).isJsonNull) json.get(key).asString
            else default
        } catch (e: Exception) { default }
    }

    private fun safeGetInt(json: JsonObject, key: String, default: Int): Int {
        return try {
            if (json.has(key) && !json.get(key).isJsonNull) json.get(key).asInt
            else default
        } catch (e: Exception) { default }
    }
}

/**
 * 播放结果
 */
sealed class PlayerResult {
    /** 播放 URL（直链或解析后的 URL） */
    abstract val url: String
    /** 请求头 */
    abstract val header: Map<String, String>

    /** 直链，可直接播放 */
    data class Direct(
        override val url: String,
        override val header: Map<String, String> = emptyMap()
    ) : PlayerResult()

    /** 需要通过解析接口二次解析（目前 getPlayerUrl 内部已自动解析，此类型保留供扩展） */
    data class NeedParse(
        override val url: String,
        override val header: Map<String, String> = emptyMap()
    ) : PlayerResult()
}
