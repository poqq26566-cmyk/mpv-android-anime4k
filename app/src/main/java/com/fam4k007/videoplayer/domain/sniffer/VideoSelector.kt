package com.fam4k007.videoplayer.domain.sniffer

import com.fam4k007.videoplayer.sniffer.DetectedVideo

/**
 * 视频选择器 - 从嗅探到的视频列表中智能选择最佳视频
 * 纯领域逻辑，无副作用
 */
object VideoSelector {

    /**
     * 智能选择最佳视频
     * 优先级：.m3u8/.mpd > .mp4 > 其他格式
     * 同格式内按质量评分排序
     */
    fun selectBest(videos: List<DetectedVideo>): DetectedVideo {
        if (videos.isEmpty()) throw IllegalArgumentException("视频列表为空")
        if (videos.size == 1) return videos.first()

        // 优先选择流媒体清单（HLS / DASH）
        val manifestVideos = videos.filter {
            it.url.contains(".m3u8", ignoreCase = true) || it.url.contains(".mpd", ignoreCase = true)
        }
        if (manifestVideos.isNotEmpty()) {
            return selectBestInGroup(manifestVideos)
        }

        // 其次选择 mp4
        val mp4Videos = videos.filter { it.url.contains(".mp4", ignoreCase = true) }
        if (mp4Videos.isNotEmpty()) {
            return selectBestInGroup(mp4Videos)
        }

        // 最后选择其他格式
        return selectBestInGroup(videos)
    }

    /**
     * 在视频组内选择最佳的（按质量评分）
     */
    private fun selectBestInGroup(videos: List<DetectedVideo>): DetectedVideo {
        if (videos.size == 1) return videos.first()

        return videos
            .map { it to calculateQualityScore(it.url) }
            .sortedWith(
                compareByDescending<Pair<DetectedVideo, Int>> { it.second }
                    .thenByDescending { it.first.url.length }
            )
            .first()
            .first
    }

    /**
     * 计算视频URL的质量评分
     */
    private fun calculateQualityScore(url: String): Int {
        val lowerUrl = url.lowercase()

        val highQualityKeywords = listOf(
            "1080p", "1080", "4k", "2160p", "2160", "uhd",
            "hd", "high", "best", "master", "premium", "vip"
        )
        val mediumQualityKeywords = listOf("720p", "720", "fhd", "fullhd")
        val lowQualityKeywords = listOf("360p", "360", "480p", "480", "sd", "low", "mobile")

        var score = 0

        highQualityKeywords.forEach { if (lowerUrl.contains(it)) score += 10 }
        mediumQualityKeywords.forEach { if (lowerUrl.contains(it)) score += 5 }
        lowQualityKeywords.forEach { if (lowerUrl.contains(it)) score -= 5 }

        // 降低重定向/包装URL的分数
        if (lowerUrl.contains("php?") || lowerUrl.contains("?url=") || lowerUrl.contains("redirect")) {
            score -= 10
        }

        // 解析分辨率数字
        val resolutionPattern = Regex("(\\d{3,4})p")
        resolutionPattern.find(lowerUrl)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { res ->
                score += when {
                    res >= 1080 -> 10
                    res >= 720 -> 7
                    res >= 480 -> 4
                    res >= 360 -> 3
                    else -> 1
                }
            }
        }

        return score.coerceAtLeast(1) // 基础分数
    }
}
