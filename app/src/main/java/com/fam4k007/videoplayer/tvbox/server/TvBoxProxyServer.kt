package com.fam4k007.videoplayer.tvbox.server

import android.util.Log
import com.fam4k007.videoplayer.tvbox.config.TvBoxConfigManager
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream

/**
 * TVBox 本地代理服务器
 *
 * 处理 JAR Spider 返回的代理 URL 请求（如 /proxy?do=m3u8&url=...）。
 * Spider.playerContent() 返回类似 http://127.0.0.1:9978/proxy?do=m3u8&url=... 的地址，
 * 播放器请求此地址时，由本服务器接收并调用 JAR 的 spider.Proxy.proxy(Map) 获取流数据返回。
 *
 * 与 FongMi 的 server/process/Proxy.java 逻辑一致：
 * 通过 JarLoader.proxy(params) 调用 JAR 内 spider.Proxy 的 proxy(Map) 静态方法。
 */
class TvBoxProxyServer(
    port: Int,
    private val configManager: TvBoxConfigManager
) : NanoHTTPD(port) {

    /** 服务器监听端口 */
    val serverPort: Int = port

    companion object {
        private const val TAG = "TvBoxProxyServer"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        Log.d(TAG, "serve: $uri")

        return when {
            uri.startsWith("/proxy") -> handleProxy(session)
            else -> newFixedLengthResponse(
                Response.Status.OK,
                "text/plain",
                "OK"
            )
        }
    }

    private fun handleProxy(session: IHTTPSession): Response {
        try {
            // 收集所有参数（URL query + POST body + headers），与 FongMi 一致
            val params = mutableMapOf<String, String>()
            session.parms?.let { params.putAll(it) }
            session.headers?.let { params.putAll(it) }

            Log.d(TAG, "proxy params: $params")

            // 通过 JarLoader 调用 JAR 内 spider.Proxy.proxy(Map)
            // 与 FongMi 的 BaseLoader.get().proxy(params) 一致
            val rs = configManager.jarLoader.proxy(params)
            if (rs != null && rs.isNotEmpty()) {
                // rs[0] = statusCode (Int)
                val statusCode = rs[0] as? Int ?: 200
                // rs[1] = contentType (String)
                val contentType = if (rs.size > 1 && rs[1] is String) rs[1] as String else "application/octet-stream"
                // rs[2] = data (InputStream 或 byte[])
                val data = if (rs.size > 2) rs[2] else null
                // rs[3] = headers (Map<String, String>)
                @Suppress("UNCHECKED_CAST")
                val headers = if (rs.size > 3 && rs[3] is Map<*, *>) rs[3] as? Map<String, String> else null

                val response = when (data) {
                    is java.io.InputStream -> newChunkedResponse(
                        Response.Status.lookup(statusCode),
                        contentType,
                        data
                    )
                    is ByteArray -> newFixedLengthResponse(
                        Response.Status.lookup(statusCode),
                        contentType,
                        ByteArrayInputStream(data),
                        data.size.toLong()
                    )
                    else -> newFixedLengthResponse(
                        Response.Status.lookup(statusCode),
                        contentType,
                        ByteArrayInputStream(ByteArray(0)),
                        0L
                    )
                }

                headers?.forEach { (k, v) -> response.addHeader(k, v) }
                return response
            }

            Log.w(TAG, "proxy: JAR 未处理此请求")
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "text/plain",
                "Not Found"
            )
        } catch (e: Exception) {
            Log.e(TAG, "proxy error", e)
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                e.message ?: "Internal Error"
            )
        }
    }
}
