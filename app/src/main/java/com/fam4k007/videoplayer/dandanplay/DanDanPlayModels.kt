package com.fam4k007.videoplayer.dandanplay

import com.google.gson.annotations.SerializedName

/**
 * DanDanPlay API 数据模型
 */

/**
 * 动漫类型别名（用于简化引用）
 */
typealias Anime = AnimeSearchInfo

// 搜索动漫请求
data class SearchAnimeRequest(
    val anime: String,
    val episode: String? = null
)

// 搜索动漫响应
data class SearchAnimeResponse(
    @SerializedName("hasMore")
    val hasMore: Boolean,
    @SerializedName("animes")
    val animes: List<AnimeSearchInfo>
)

// 动漫信息
data class AnimeSearchInfo(
    @SerializedName("animeId")
    val animeId: Int,
    @SerializedName("animeTitle")
    val animeTitle: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("typeDescription")
    val typeDescription: String,
    @SerializedName("episodes")
    val episodes: List<EpisodeInfo>
)

// 剧集信息
data class EpisodeInfo(
    @SerializedName("episodeId")
    val episodeId: Int,
    @SerializedName("episodeTitle")
    val episodeTitle: String
)

// 弹幕响应
data class DanmakuResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("comments")
    val comments: List<DanmakuComment>
)

// 弹幕评论
data class DanmakuComment(
    @SerializedName("cid")
    val cid: Long,
    @SerializedName("p")
    val p: String,  // 格式: "时间,模式,颜色,用户ID"
    @SerializedName("m")
    val m: String   // 弹幕内容
)

// 文件哈希匹配请求
data class MatchRequest(
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("fileHash")
    val fileHash: String,
    @SerializedName("fileSize")
    val fileSize: Long,
    @SerializedName("videoDuration")
    val videoDuration: Double? = null,  // 改为可空，不传0.0
    @SerializedName("matchMode")
    val matchMode: String? = null  // 改为可空，使用API默认值
)

// 文件哈希匹配响应
data class MatchResponse(
    @SerializedName("isMatched")
    val isMatched: Boolean,
    @SerializedName("matches")
    val matches: List<MatchInfo>?
)

// 匹配信息
data class MatchInfo(
    @SerializedName("episodeId")
    val episodeId: Int,
    @SerializedName("animeId")
    val animeId: Int,
    @SerializedName("animeTitle")
    val animeTitle: String,
    @SerializedName("episodeTitle")
    val episodeTitle: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("typeDescription")
    val typeDescription: String,
    @SerializedName("shift")
    val shift: Double = 0.0
)
