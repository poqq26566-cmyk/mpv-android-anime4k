package com.fam4k007.videoplayer.dandanplay

import kotlinx.serialization.Serializable

/**
 * 弹幕服务器配置
 */
@Serializable
data class DanmakuServer(
    val id: String,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false
) {
    companion object {
        const val DEFAULT_URL = "https://api.dandanplay.net"
        const val DEFAULT_NAME = "弹弹Play（默认）"
        const val DEFAULT_ID = "default"

        fun createDefault(): DanmakuServer = DanmakuServer(
            id = DEFAULT_ID,
            name = DEFAULT_NAME,
            url = DEFAULT_URL,
            isEnabled = true,
            isDefault = true
        )
    }
}
