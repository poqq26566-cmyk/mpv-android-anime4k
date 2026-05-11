package com.fam4k007.videoplayer.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.fam4k007.videoplayer.R

/**
 * 应用主题枚举
 * 参考 mpvEx 设计，每个主题都有独特的配色方案
 * 支持亮色和暗色模式
 */
enum class AppTheme(
    @StringRes val titleRes: Int,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
    val tertiaryLight: Color,
    val tertiaryDark: Color,
    val backgroundLight: Color,
    val backgroundDark: Color,
    val isDynamic: Boolean = false,
) {
    /**
     * 默认主题 - 蓝紫色系
     */
    Default(
        titleRes = R.string.theme_default,
        primaryLight = Color(0xFF794F81),
        primaryDark = Color(0xFFE8B5EF),
        secondaryLight = Color(0xFF6A596C),
        secondaryDark = Color(0xFFD6C0D6),
        tertiaryLight = Color(0xFF82524D),
        tertiaryDark = Color(0xFFF5B7B0),
        backgroundLight = Color(0xFFFFF7FB),
        backgroundDark = Color(0xFF161217),
    ),

    /**
     * 动态主题 - 基于系统壁纸（Android 12+）
     */
    Dynamic(
        titleRes = R.string.theme_dynamic,
        primaryLight = Color(0xFF6750A4),
        primaryDark = Color(0xFFD0BCFF),
        secondaryLight = Color(0xFF625B71),
        secondaryDark = Color(0xFFCCC2DC),
        tertiaryLight = Color(0xFF7D5260),
        tertiaryDark = Color(0xFFEFB8C8),
        backgroundLight = Color(0xFFFFFBFF),
        backgroundDark = Color(0xFF1C1B1F),
        isDynamic = true,
    ),

    /**
     * Mocha 主题 - 摩卡棕色系
     */
    Mocha(
        titleRes = R.string.theme_mocha,
        primaryLight = Color(0xFF795548),
        primaryDark = Color(0xFFBCAAA4),
        secondaryLight = Color(0xFF5D4037),
        secondaryDark = Color(0xFFA1887F),
        tertiaryLight = Color(0xFF6D4C41),
        tertiaryDark = Color(0xFFD7CCC8),
        backgroundLight = Color(0xFFFFF9F5),
        backgroundDark = Color(0xFF1A1512),
    ),

    /**
     * Ocean 主题 - 海洋蓝色系
     */
    Ocean(
        titleRes = R.string.theme_ocean,
        primaryLight = Color(0xFF006064),
        primaryDark = Color(0xFF4DD0E1),
        secondaryLight = Color(0xFF00838F),
        secondaryDark = Color(0xFF80DEEA),
        tertiaryLight = Color(0xFF0097A7),
        tertiaryDark = Color(0xFF26C6DA),
        backgroundLight = Color(0xFFF0FFFF),
        backgroundDark = Color(0xFF0A1A1C),
    ),

    /**
     * Forest 主题 - 森林绿色系
     */
    Forest(
        titleRes = R.string.theme_forest,
        primaryLight = Color(0xFF1B5E20),
        primaryDark = Color(0xFF66BB6A),
        secondaryLight = Color(0xFF33691E),
        secondaryDark = Color(0xFF9CCC65),
        tertiaryLight = Color(0xFF2E7D32),
        tertiaryDark = Color(0xFFA5D6A7),
        backgroundLight = Color(0xFFF1F8E9),
        backgroundDark = Color(0xFF0D1A0D),
    ),

    /**
     * Midnight 主题 - 午夜蓝色系
     */
    Midnight(
        titleRes = R.string.theme_midnight,
        primaryLight = Color(0xFF0D47A1),
        primaryDark = Color(0xFF90CAF9),
        secondaryLight = Color(0xFF455A64),
        secondaryDark = Color(0xFFB0BEC5),
        tertiaryLight = Color(0xFF1565C0),
        tertiaryDark = Color(0xFF64B5F6),
        backgroundLight = Color(0xFFF5F9FF),
        backgroundDark = Color(0xFF0D1117),
    ),

    /**
     * Sunset 主题 - 日落橙色系
     */
    Sunset(
        titleRes = R.string.theme_sunset,
        primaryLight = Color(0xFFE65100),
        primaryDark = Color(0xFFFF9E80),
        secondaryLight = Color(0xFFEF6C00),
        secondaryDark = Color(0xFFFFCC80),
        tertiaryLight = Color(0xFFF4511E),
        tertiaryDark = Color(0xFFFF8A65),
        backgroundLight = Color(0xFFFFF5F0),
        backgroundDark = Color(0xFF1A120D),
    ),

    /**
     * RoseGold 主题 - 玫瑰金色系
     */
    RoseGold(
        titleRes = R.string.theme_rose_gold,
        primaryLight = Color(0xFFB76E79),
        primaryDark = Color(0xFFE8A9B0),
        secondaryLight = Color(0xFFAD8075),
        secondaryDark = Color(0xFFDDBFB8),
        tertiaryLight = Color(0xFFD4A5A5),
        tertiaryDark = Color(0xFFF5D5D5),
        backgroundLight = Color(0xFFFFF5F5),
        backgroundDark = Color(0xFF1A1212),
    );

    /**
     * 获取亮色配色方案
     */
    fun getLightColorScheme(): ColorScheme {
        return lightColorScheme(
            primary = primaryLight,
            secondary = secondaryLight,
            tertiary = tertiaryLight,
            background = backgroundLight,
            surface = backgroundLight,
            surfaceVariant = backgroundLight.copy(alpha = 0.9f),
        )
    }

    /**
     * 获取暗色配色方案
     */
    fun getDarkColorScheme(): ColorScheme {
        return darkColorScheme(
            primary = primaryDark,
            secondary = secondaryDark,
            tertiary = tertiaryDark,
            background = backgroundDark,
            surface = backgroundDark,
            surfaceVariant = backgroundDark.copy(alpha = 0.9f),
        )
    }

    /**
     * 获取 AMOLED 纯黑配色方案（为深色模式优化，省电）
     */
    fun getAmoledColorScheme(): ColorScheme {
        return darkColorScheme(
            primary = primaryDark,
            secondary = secondaryDark,
            tertiary = tertiaryDark,
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0A0A0A),
            surfaceDim = Color.Black,
            surfaceBright = Color(0xFF1F1F1F),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF121212),
            surfaceContainerHigh = Color(0xFF1A1A1A),
            surfaceContainerHighest = Color(0xFF242424),
        )
    }

    companion object {
        /**
         * 从名称获取主题
         */
        fun fromName(name: String): AppTheme {
            return entries.find { it.name == name } ?: Default
        }

        /**
         * 从资源 ID 获取主题
         */
        fun fromTitleRes(@StringRes titleRes: Int): AppTheme {
            return entries.find { it.titleRes == titleRes } ?: Default
        }
    }
}

/**
 * 暗色模式枚举
 */
enum class DarkMode {
    Light,   // 始终亮色
    Dark,    // 始终暗色
    System;  // 跟随系统

    companion object {
        fun fromName(name: String): DarkMode {
            return entries.find { it.name == name } ?: System
        }
    }
}
