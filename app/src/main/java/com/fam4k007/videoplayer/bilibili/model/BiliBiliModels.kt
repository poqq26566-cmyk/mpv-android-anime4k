package com.fam4k007.videoplayer.bilibili.model

/**
 * 番剧列表项（类型别名）
 * 用于番剧列表展示，实际就是 BangumiCard
 */
typealias BangumiItem = BangumiCard

/**
 * B站用户信息
 */
data class UserInfo(
    val mid: Long,              // 用户UID
    val uname: String,          // 用户名
    val face: String = "",      // 头像URL
    val vipStatus: Int = 0,     // 大会员状态 0:无 1:有
    val vipType: Int = 0        // 大会员类型 0:无 1:月度 2:年度
)

/**
 * 登录二维码信息
 */
data class QRCodeInfo(
    val url: String,            // 二维码内容URL
    val qrcodeKey: String       // 轮询key
)

/**
 * 登录结果
 */
sealed class LoginResult {
    object Success : LoginResult()              // 登录成功
    object WaitingScan : LoginResult()          // 等待扫码
    object WaitingConfirm : LoginResult()       // 已扫码，等待确认
    object Expired : LoginResult()              // 二维码已过期
    data class Failed(val message: String) : LoginResult()  // 登录失败
}

/**
 * 番剧基本信息
 */
data class BangumiInfo(
    val seasonId: String,           // 季度ID (ssXXXX)
    val title: String,              // 标题
    val cover: String,              // 封面URL
    val squareCover: String = "",   // 方形封面
    val evaluate: String = "",      // 简介
    val rating: String = "",        // 评分
    val stat: BangumiStat? = null,  // 统计数据
    val episodes: List<Episode> = emptyList(),  // 集数列表
    val mediaId: Long = 0           // 媒体ID
)

/**
 * 番剧统计数据
 */
data class BangumiStat(
    val coins: Long = 0,        // 投币数
    val danmakus: Long = 0,     // 弹幕数
    val favorites: Long = 0,    // 收藏数
    val views: Long = 0         // 播放数
)

/**
 * 番剧集数信息
 */
data class Episode(
    val id: Long,               // 集ID (epXXXX)
    val cid: Long,              // 视频CID
    val aid: Long,              // 稿件avid
    val bvid: String = "",      // 稿件bvid
    val title: String,          // 标题 "第1话"
    val longTitle: String,      // 副标题
    val cover: String = "",     // 封面
    val duration: Long = 0,     // 时长(秒)
    val badge: String = "",     // 标签 (会员、限免等)
    val badgeType: Int = 0      // 0:普通 1:会员 2:限免
)

/**
 * 播放地址信息
 */
data class PlayUrlInfo(
    val url: String,            // 视频URL
    val quality: Int,           // 画质ID
    val format: String,         // 格式 (dash/flv/mp4)
    val duration: Long = 0,     // 时长
    val audioUrl: String = "",  // DASH音频URL
    val needLogin: Boolean = false,     // 是否需要登录
    val needVip: Boolean = false        // 是否需要大会员
)

/**
 * 番剧卡片（用于列表展示）
 */
data class BangumiCard(
    val seasonId: String,
    val title: String,
    val cover: String,
    val badge: String = "",         // 角标文本
    val rating: String = "",        // 评分
    val newEp: String = ""          // 更新信息 "更新至第12话"
)

/**
 * 番剧时间表项
 */
data class TimelineItem(
    val date: String,               // 日期 "2024-01-01"
    val dateTs: Long,               // 时间戳
    val dayOfWeek: Int,             // 星期 1-7
    val isToday: Boolean,           // 是否今天
    val episodes: List<TimelineEpisode>
)

/**
 * 时间表集数
 */
data class TimelineEpisode(
    val seasonId: String,
    val title: String,
    val cover: String,
    val pubIndex: String,           // "第12话"
    val pubTime: String,            // "12:00"
    val pubTs: Long                 // 发布时间戳
)
