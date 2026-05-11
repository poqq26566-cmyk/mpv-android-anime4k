package com.fam4k007.videoplayer.navigation

import kotlinx.serialization.Serializable

/**
 * 应用导航路由定义
 * 使用 @Serializable 实现类型安全的导航
 */
sealed interface AppScreen {

    /** 主页 */
    @Serializable
    data object Home : AppScreen

    /** 设置页 */
    @Serializable
    data object Settings : AppScreen

    /** 播放设置页 */
    @Serializable
    data object PlaybackSettings : AppScreen

    /** 播放历史页 */
    @Serializable
    data object PlaybackHistory : AppScreen

    /** 关于页 */
    @Serializable
    data object About : AppScreen

    /** WebDAV 账户列表页 */
    @Serializable
    data object WebDavAccounts : AppScreen

    /** WebDAV 文件浏览页 */
    @Serializable
    data class WebDavBrowser(val accountId: String) : AppScreen
}
