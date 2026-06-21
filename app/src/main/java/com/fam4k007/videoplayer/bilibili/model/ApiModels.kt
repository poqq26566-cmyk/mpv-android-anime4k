package com.fam4k007.videoplayer.bilibili.model

import com.google.gson.annotations.SerializedName

/**
 * B站API通用响应
 */
data class BiliApiResponse<T>(
    val code: Int,          // 0:成功
    val message: String?,
    val ttl: Int = 1,
    val data: T?
)

/**
 * 时间表API响应（使用 result 字段）
 */
data class TimelineApiResponse(
    val code: Int,
    val message: String?,
    val result: List<TimelineResponse>?
)

/**
 * 生成二维码响应
 */
data class QRCodeResponse(
    val url: String,                // 二维码内容
    @SerializedName("qrcode_key")
    val qrcodeKey: String           // 轮询key
)

/**
 * 二维码登录轮询响应
 */
data class QRLoginPollResponse(
    val url: String? = null,        // 游戏分站跳转URL (通常为空)
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    val timestamp: Long = 0,
    val code: Int = 0,              // 0:成功 86038:过期 86090:已扫未确认 86101:未扫
    val message: String? = null
)

/**
 * 用户信息响应
 */
data class UserInfoResponse(
    val mid: Long,
    val uname: String,
    val face: String,
    @SerializedName("vip")
    val vipInfo: VipInfo? = null
)

data class VipInfo(
    val type: Int,          // 大会员类型
    val status: Int,        // 状态
    @SerializedName("theme_type")
    val themeType: Int = 0
)

/**
 * 番剧详情响应
 */
data class BangumiDetailResponse(
    @SerializedName("season_id")
    val seasonId: Long,
    val title: String,
    val cover: String,
    @SerializedName("square_cover")
    val squareCover: String = "",
    val evaluate: String = "",
    val rating: RatingInfo? = null,
    val stat: StatInfo? = null,
    @SerializedName("media_id")
    val mediaId: Long = 0,
    val episodes: List<EpisodeInfo>? = null
)

data class RatingInfo(
    val score: Float,
    val count: Int
)

data class StatInfo(
    val coins: Long = 0,
    val danmakus: Long = 0,
    val favorites: Long = 0,
    val views: Long = 0
)

data class EpisodeInfo(
    val id: Long,
    val cid: Long,
    val aid: Long,
    val bvid: String = "",
    val title: String,
    @SerializedName("long_title")
    val longTitle: String = "",
    val cover: String = "",
    val duration: Long = 0,
    val badge: String = "",
    @SerializedName("badge_type")
    val badgeType: Int = 0
)

/**
 * 番剧列表响应
 */
data class BangumiListResponse(
    val list: List<BangumiCardInfo>? = null,
    val total: Int = 0,
    @SerializedName("has_next")
    val hasNext: Boolean = false
)

data class BangumiCardInfo(
    @SerializedName("season_id")
    val seasonId: Long,
    val title: String,
    val cover: String,
    val badge: String = "",
    val rating: RatingInfo? = null,
    @SerializedName("new_ep")
    val newEp: NewEpInfo? = null
)

data class NewEpInfo(
    val id: Long,
    @SerializedName("index_show")
    val indexShow: String   // "更新至第12话"
)



data class DurlInfo(
    val url: String,
    val size: Long
)

data class DashInfo(
    val duration: Long,
    val video: List<MediaInfo>? = null,
    val audio: List<MediaInfo>? = null
)

data class MediaInfo(
    val id: Int,                // 清晰度标识
    @SerializedName("base_url")
    val baseUrl: String,
    val bandwidth: Int,
    val codecid: Int = 0
)

/**
 * 时间表响应
 */
data class TimelineResponse(
    val date: String,
    @SerializedName("date_ts")
    val dateTs: Long,
    @SerializedName("day_of_week")
    val dayOfWeek: Int,
    @SerializedName("is_today")
    val isToday: Int,
    val episodes: List<TimelineEpisodeInfo>? = null
)

data class TimelineEpisodeInfo(
    @SerializedName("season_id")
    val seasonId: Long,
    val title: String,
    @SerializedName("square_cover")
    val squareCover: String,
    @SerializedName("pub_index")
    val pubIndex: String,
    @SerializedName("pub_time")
    val pubTime: String,
    @SerializedName("pub_ts")
    val pubTs: Long
)

/**
 * 搜索番剧结果
 */
data class SearchBangumiResult(
    val result: List<SearchBangumiItem>?,
    val numResults: Int,
    val numPages: Int
)

data class SearchBangumiItem(
    @SerializedName("season_id")
    val season_id: Long,
    val title: String,
    val cover: String,
    @SerializedName("index_show")
    val index_show: String?,
    val badge: String?,
    @SerializedName("media_score")
    val media_score: MediaScore?
) {
    val score: Float?
        get() = media_score?.score
}

data class MediaScore(
    val score: Float,
    val user_count: Int
)
