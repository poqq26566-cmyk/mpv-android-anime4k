package com.fam4k007.videoplayer.sniffer

import android.util.Log

/**
 * URL检测工具类（参考海阔视界的UrlDetector实现）
 */
object UrlDetector {
    private const val TAG = "UrlDetector"
    
    // 视频扩展名列表
    private val videoExtensions = listOf(
        ".mp4", ".MP4", ".m3u8", ".M3U8", ".mpd", ".MPD", ".flv", ".FLV",
        ".avi", ".AVI", ".3gp", ".3GP", ".mpeg", ".MPEG",
        ".wmv", ".WMV", ".mov", ".MOV", ".rmvb", ".RMVB",
        ".dat", ".DAT", ".mkv", ".MKV", ".webm", ".WEBM",
        "mime=video%2F", "mime_type=video"
    )
    
    // 音频扩展名
    private val audioExtensions = listOf(
        ".mp3", ".MP3", ".wav", ".WAV", ".ogg", ".OGG",
        ".flac", ".FLAC", ".m4a", ".M4A", ".aac", ".AAC"
    )
    
    // HTML/JS/CSS等非媒体资源
    private val nonMediaExtensions = listOf(
        ".css", ".js", ".html", ".htm", ".json", ".xml",
        ".txt", ".ico", ".png", ".jpg", ".jpeg", ".gif",
        ".webp", ".svg", ".woff", ".woff2", ".ttf", ".apk"
    )
    
    // 视频路径关键词（用于检测动态URL）
    private val videoPathKeywords = listOf(
        "/video/", "/play/", "/stream/", "/media/", "/vod/", "/movie/",
        "/tv/", "/anime/", "/film/", "/clip/", "/episode/"
    )
    
    // 视频参数关键词
    private val videoParamKeywords = listOf("video", "url", "src", "source", "stream")
    
    // 明确排除的URL模式
    private val excludePatterns = listOf(
        ".mp4.jpg", ".mp4.png", ".mp4.webp",  // 视频缩略图
        ".php?url=http", "/?url=http"          // 跳转链接
    )
    
    /**
     * 检测URL是否为视频
     */
    fun isVideo(url: String, headers: Map<String, String> = emptyMap()): Boolean {
        if (url.isEmpty() || url.contains("ignoreVideo=true")) {
            return false
        }
        
        // 检查是否明确排除
        for (pattern in excludePatterns) {
            if (url.contains(pattern, ignoreCase = true)) {
                return false
            }
        }
        
        // 协议检测（rtmp/rtsp直接判定为视频）
        if (url.startsWith("rtmp://") || url.startsWith("rtmps://") || url.startsWith("rtsp://")) {
            return true
        }
        
        // 强制标记
        if (url.contains("isVideo=true")) {
            return true
        }
        
        // 提取URL的路径部分（去除参数）
        val pathUrl = url.split("?")[0]
        
        // 检查是否为非媒体资源
        for (ext in nonMediaExtensions) {
            if (pathUrl.contains(ext, ignoreCase = true)) {
                return false
            }
        }
        
        // 检查视频扩展名
        for (ext in videoExtensions) {
            if (url.contains(ext, ignoreCase = true)) {
                logDebug("Detected video by extension: $ext in $url")
                return true
            }
        }
        
        // 检查路径关键词（用于动态URL检测）
        for (keyword in videoPathKeywords) {
            if (pathUrl.contains(keyword, ignoreCase = true)) {
                // 进一步检查：确保不是JS/CSS等资源
                val hasMediaExt = videoExtensions.any { pathUrl.contains(it, ignoreCase = true) } ||
                                 audioExtensions.any { pathUrl.contains(it, ignoreCase = true) }
                if (!hasMediaExt) {
                    logDebug("Detected video by path keyword: $keyword in $url")
                    return true
                }
            }
        }
        
        // 检查URL参数（有些网站通过参数传递视频信息）
        val queryParams = url.split("?").getOrNull(1)
        if (queryParams != null) {
            for (keyword in videoParamKeywords) {
                if (queryParams.contains("$keyword=", ignoreCase = true)) {
                    logDebug("Detected video by param keyword: $keyword in $url")
                    return true
                }
            }
        }
        
        // 检查Content-Type（如果有headers）
        val contentType = headers["Content-Type"]?.lowercase() ?: headers["content-type"]?.lowercase()
        if (contentType != null && (
                contentType.contains("video/") ||
                    contentType.contains("application/vnd.apple.mpegurl") ||
                    contentType.contains("application/x-mpegurl") ||
                    contentType.contains("application/dash+xml")
                )
        ) {
            logDebug("Detected video by Content-Type: $contentType")
            return true
        }
        
        return false
    }
    
    /**
     * 检测URL是否为音频
     */
    fun isAudio(url: String, headers: Map<String, String> = emptyMap()): Boolean {
        if (url.isEmpty() || url.contains("isMusic=true")) {
            return url.contains("isMusic=true")
        }
        
        val pathUrl = url.split("?")[0]
        
        for (ext in audioExtensions) {
            if (pathUrl.contains(ext, ignoreCase = true)) {
                return true
            }
        }
        
        val contentType = headers["Content-Type"]?.lowercase() ?: headers["content-type"]?.lowercase()
        if (contentType != null && contentType.contains("audio/")) {
            return true
        }
        
        return false
    }
    
    /**
     * 获取需要检查的URL（移除fragment等）
     */
    fun getNeedCheckUrl(url: String): String {
        return url.split("#")[0]
    }
    
    /**
     * 获取视频格式
     */
    fun getVideoFormat(url: String): String {
        return when {
            url.contains(".m3u8", ignoreCase = true) -> "M3U8"
            url.contains(".mpd", ignoreCase = true) -> "DASH"
            url.contains(".mp4", ignoreCase = true) -> "MP4"
            url.contains(".flv", ignoreCase = true) -> "FLV"
            url.contains(".avi", ignoreCase = true) -> "AVI"
            url.contains(".mkv", ignoreCase = true) -> "MKV"
            url.contains(".webm", ignoreCase = true) -> "WEBM"
            url.contains(".mov", ignoreCase = true) -> "MOV"
            url.contains("rtmp://") -> "RTMP"
            url.contains("rtsp://") -> "RTSP"
            else -> "UNKNOWN"
        }
    }

    private fun logDebug(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: RuntimeException) {
            // Ignore JVM unit-test environments without android.util.Log backend.
        }
    }
}
