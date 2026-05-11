package com.fam4k007.videoplayer.bilibili.model

/**
 * 番剧详情（简化版，用于ViewModel）
 */
data class BangumiDetail(
    val seasonId: String,
    val title: String,
    val cover: String,
    val description: String,
    val episodes: List<Episode>  // 使用 BiliBiliModels.kt 中定义的 Episode
)
