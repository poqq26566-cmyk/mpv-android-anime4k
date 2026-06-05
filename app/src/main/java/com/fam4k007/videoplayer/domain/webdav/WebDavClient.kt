package com.fam4k007.videoplayer.domain.webdav

import android.util.Base64
import com.xyoye.sardine.Sardine
import com.xyoye.sardine.impl.OkHttpSardine
import com.xyoye.sardine.DavResource
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * WebDAV 配置数据类（用于客户端）
 */
data class WebDavConfig(
    val serverUrl: String,
    val account: String = "",
    val password: String = "",
    val isAnonymous: Boolean = false
)

/**
 * WebDAV 文件类别枚举
 */
enum class WebDavFileCategory {
    VIDEO,      // 视频
    AUDIO,      // 音频
    IMAGE,      // 图片
    DOCUMENT,   // 文档/文本
    ARCHIVE,    // 压缩包
    SUBTITLE,   // 字幕
    OTHER       // 其他
}

/**
 * WebDAV 客户端工具类
 * Domain Layer - 核心业务逻辑（WebDAV 操作）
 * 封装 Sardine 库的操作
 */
class WebDavClient(internal val config: WebDavConfig) {

    private val sardine: Sardine
    
    data class WebDavFile(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val modifiedTime: Long
    )

    init {
        // 创建不验证 SSL 证书的 OkHttpClient（用于自签名证书）
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val okHttpClientBuilder = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // 如果不是匿名访问，添加认证拦截器
        if (!config.isAnonymous && config.account.isNotEmpty()) {
            okHttpClientBuilder.addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val originalRequest = chain.request()
                    val authenticatedRequest = originalRequest.newBuilder()
                        .header("Authorization", Credentials.basic(config.account, config.password))
                        .build()
                    return chain.proceed(authenticatedRequest)
                }
            })
        }

        val okHttpClient = okHttpClientBuilder.build()
        sardine = OkHttpSardine(okHttpClient)
    }

    /**
     * 测试连接
     */
    fun testConnection(): Boolean {
        return try {
            sardine.list(config.serverUrl, 0)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 列出目录内容
     * @param path 相对路径（相对于 serverUrl）
     * @return 文件列表
     */
    fun listFiles(path: String = ""): List<WebDavFile> {
        try {
            val fullUrl = buildUrl(path)
            val resources: List<DavResource> = sardine.list(fullUrl, 1)

            return resources
                .drop(1)  // 跳过第一个资源（目录本身）
                .mapNotNull { resource ->
                    val rawName = resource.name
                    if (rawName.isNullOrBlank()) return@mapNotNull null
                    val name = rawName.trim()
                    
                    // 构建相对路径
                    val relativePath = if (path.isEmpty() || path == "/") {
                        name
                    } else {
                        "${path.trimEnd('/')}/$name"
                    }
                    
                    WebDavFile(
                        name = name,
                        path = relativePath,
                        isDirectory = resource.isDirectory,
                        size = resource.contentLength ?: 0L,
                        modifiedTime = resource.modified?.time ?: 0L
                    )
                }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.orEmpty() }))
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 获取文件流 URL（用于播放器）
     */
    fun getFileUrl(path: String): String {
        return buildUrl(path)
    }

    /**
     * 获取认证头信息（用于播放器）
     */
    fun getAuthHeader(): Map<String, String>? {
        if (config.isAnonymous || config.account.isEmpty()) {
            return null
        }
        
        return mapOf("Authorization" to Credentials.basic(config.account, config.password))
    }

    /**
     * 
     * 构建完整 URL
     */
    private fun buildUrl(path: String): String {
        val cleanPath = path.trimStart('/')
        return if (cleanPath.isEmpty()) {
            config.serverUrl
        } else {
            "${config.serverUrl.trimEnd('/')}/$cleanPath"
        }
    }

    companion object {
        /**
         * 判断是否为视频文件
         */
        fun isVideoFile(fileName: String): Boolean {
            return getFileCategory(fileName) == WebDavFileCategory.VIDEO
        }

        /**
         * 根据文件名判断文件类别
         */
        fun getFileCategory(fileName: String): WebDavFileCategory {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return when (extension) {
                in setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v",
                    "3gp", "3g2", "ts", "m2ts", "mts", "rmvb", "rm", "asf") -> WebDavFileCategory.VIDEO
                in setOf("mp3", "flac", "wav", "ogg", "aac", "m4a", "wma", "opus",
                    "aiff", "alac", "ape", "dsf", "dff") -> WebDavFileCategory.AUDIO
                in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
                    "ico", "tiff", "tif", "heic", "heif", "raw") -> WebDavFileCategory.IMAGE
                in setOf("txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                    "epub", "mobi", "md", "log", "csv", "json", "xml", "html", "htm",
                    "css", "js", "py", "java", "kt", "sql", "yaml", "yml", "toml",
                    "ini", "cfg", "conf") -> WebDavFileCategory.DOCUMENT
                in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "iso") -> WebDavFileCategory.ARCHIVE
                in setOf("srt", "ass", "ssa", "vtt", "sub", "idx", "sup", "pgs") -> WebDavFileCategory.SUBTITLE
                else -> WebDavFileCategory.OTHER
            }
        }
    }
}
