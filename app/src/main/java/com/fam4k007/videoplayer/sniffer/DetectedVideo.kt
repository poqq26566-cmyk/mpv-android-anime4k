package com.fam4k007.videoplayer.sniffer

import com.fam4k007.videoplayer.remote.RemotePlaybackHeaders
import com.fam4k007.videoplayer.remote.RemotePlaybackRequest

/**
 * 视频URL检测结果
 */
data class DetectedVideo(
    val url: String,              // 视频URL
    val title: String = "",       // 视频标题
    val pageUrl: String = "",     // 来源页面URL
    val headers: Map<String, String> = emptyMap(),  // HTTP头信息
    val timestamp: Long = System.currentTimeMillis()  // 检测时间戳
) {
    fun toRemotePlaybackRequest(): RemotePlaybackRequest {
        return RemotePlaybackRequest(
            url = url,
            title = title,
            sourcePageUrl = pageUrl,
            headers = RemotePlaybackHeaders.normalize(headers),
            source = RemotePlaybackRequest.Source.WEB_SNIFFER
        )
    }

    /**
     * 转换为完整的URL字符串（包含HTTP头）
     * 格式：url;{Cookie@xxx&&User-Agent@yyy}
     */
    fun toFullUrlString(): String {
        val filteredHeaders = RemotePlaybackHeaders.normalize(headers)
        if (filteredHeaders.isEmpty()) {
            return url
        }
        
        val headerStr = filteredHeaders.entries.joinToString("&&") { (key, value) ->
            // 将半角分号转换为全角分号（避免与分隔符冲突）
            "$key@${value.replace("; ", "；；")}"
        }
        
        return "$url;{$headerStr}"
    }
    
    /**
     * 获取格式化的显示文本
     */
    fun getDisplayText(): String {
        val format = when {
            url.contains(".m3u8", ignoreCase = true) -> "M3U8"
            url.contains(".mpd", ignoreCase = true) -> "DASH"
            url.contains(".mp4", ignoreCase = true) -> "MP4"
            url.contains(".flv", ignoreCase = true) -> "FLV"
            url.contains(".avi", ignoreCase = true) -> "AVI"
            url.contains(".mkv", ignoreCase = true) -> "MKV"
            url.contains("rtmp://", ignoreCase = true) -> "RTMP"
            url.contains("rtsp://", ignoreCase = true) -> "RTSP"
            else -> "VIDEO"
        }
        
        return if (title.isNotEmpty()) {
            "$title ($format)"
        } else {
            format
        }
    }
}
