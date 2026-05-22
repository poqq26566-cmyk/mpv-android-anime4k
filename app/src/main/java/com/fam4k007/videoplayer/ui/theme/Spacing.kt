package com.fam4k007.videoplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing 系统 - 统一的间距标准
 * 参考 mpvEx 的设计，提供一致的 UI 间距
 */
data class Spacing(
    val extraSmall: Dp = 4.dp,   // 超小间距 - 用于紧密排列的元素
    val smaller: Dp = 8.dp,       // 较小间距 - 用于相关元素之间
    val small: Dp = 12.dp,        // 小间距 - 用于组内元素
    val medium: Dp = 16.dp,       // 中等间距 - 标准间距（最常用）
    val large: Dp = 24.dp,        // 大间距 - 用于分组之间
    val larger: Dp = 32.dp,       // 较大间距 - 用于明显分隔
    val extraLarge: Dp = 48.dp,   // 超大间距 - 用于主要区域分隔
    val largest: Dp = 64.dp,      // 最大间距 - 用于顶层分隔
)

/**
 * CompositionLocal 用于在组件树中提供 Spacing
 */
@Suppress("CompositionLocalAllowlist")
val LocalSpacing = compositionLocalOf { Spacing() }

/**
 * MaterialTheme 扩展属性，便捷访问 Spacing
 * 使用方式：MaterialTheme.spacing.medium
 */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
