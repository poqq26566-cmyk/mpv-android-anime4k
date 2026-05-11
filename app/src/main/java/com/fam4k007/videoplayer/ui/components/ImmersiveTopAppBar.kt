package com.fam4k007.videoplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 沉浸式 TopAppBar 组件
 * 统一管理所有页面的沉浸式标题栏，支持渐变背景和状态栏延伸
 * 
 * @param title 标题内容
 * @param navigationIcon 左侧导航图标（通常是返回按钮）
 * @param actions 右侧操作按钮
 * @param colors TopAppBar 的颜色配置，默认使用透明背景
 * @param gradientColors 渐变背景的颜色，默认使用主题的 primary 和 primaryContainer
 * @param scrollBehavior 滚动行为（可选）
 * @param topAppBarHeight TopAppBar 的高度（默认56dp，比标准的64dp略小）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
    ),
    gradientColors: Pair<Color, Color>? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    topAppBarHeight: Dp = 48.dp  // 默认48dp，更紧凑的高度
) {
    // 使用传入的渐变色或默认主题色
    val (startColor, endColor) = gradientColors ?: (
        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(startColor, endColor)
                )
            )
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topAppBarHeight)  // 设置 TopAppBar 的固定高度
        ) {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                scrollBehavior = scrollBehavior
            )
        }
    }
}
