package com.fam4k007.videoplayer.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.fam4k007.videoplayer.R

/**
 * 主题色彩系统
 * 为每个主题定义完整的色阶，确保UI有层次感和协调性
 */
data class ThemeColors(
    val primary: Color,           // 主色 - 用于主要按钮、FAB、重点元素
    val primaryVariant: Color,    // 主色变体 - 用于次要按钮、选中状态
    val secondary: Color,         // 辅助色 - 用于强调、高亮
    val background: Color,        // 页面背景 - 极深色
    val surface: Color,           // 卡片/容器背景 - 深色
    val surfaceVariant: Color,    // 输入框/列表项背景 - 中深色
    val onPrimary: Color,         // 主色上的文字
    val onBackground: Color,      // 背景上的文字
    val onSurface: Color          // 表面上的文字
)

/**
 * 蓝紫主题（默认主题）
 */
val BluePurpleThemeColors = ThemeColors(
    primary = Color(0xFF667EEA),              // 中等蓝紫
    primaryVariant = Color(0xFF7C92FF),       // 亮蓝紫
    secondary = Color(0xFF90A4FF),            // 淡蓝紫
    background = Color(0xFFFFFFFF),           // 纯白色背景
    surface = Color(0xFF121828),              // 深蓝容器
    surfaceVariant = Color(0xFF1A2332),       // 中深蓝
    onPrimary = Color(0xFFFFFFFF),            // 白色文字
    onBackground = Color(0xFFE0E0E0),         // 浅灰文字
    onSurface = Color(0xFFBBDEFB)             // 淡蓝文字
)

/**
 * 绿色主题
 */
val GreenThemeColors = ThemeColors(
    primary = Color(0xFF4CAF50),              // 中等绿
    primaryVariant = Color(0xFF66BB6A),       // 亮绿
    secondary = Color(0xFF81C784),            // 淡绿
    background = Color(0xFFFFFFFF),           // 纯白色背景
    surface = Color(0xFF132819),              // 深绿容器
    surfaceVariant = Color(0xFF1B3523),       // 中深绿
    onPrimary = Color(0xFFFFFFFF),            // 白色文字
    onBackground = Color(0xFFE0E0E0),         // 浅灰文字
    onSurface = Color(0xFFC8E6C9)             // 淡绿文字
)

/**
 * 橙色主题
 */
val OrangeThemeColors = ThemeColors(
    primary = Color(0xFFFF9800),              // 中等橙
    primaryVariant = Color(0xFFFFB74D),       // 亮橙
    secondary = Color(0xFFFFCC80),            // 淡橙
    background = Color(0xFFFFFFFF),           // 纯白色背景
    surface = Color(0xFF281812),              // 深橙容器
    surfaceVariant = Color(0xFF32231A),       // 中深橙
    onPrimary = Color(0xFFFFFFFF),            // 白色文字
    onBackground = Color(0xFFE0E0E0),         // 浅灰文字
    onSurface = Color(0xFFFFE0B2)             // 淡橙文字
)

/**
 * 粉色主题
 */
val PinkThemeColors = ThemeColors(
    primary = Color(0xFFE91E63),              // 中等粉
    primaryVariant = Color(0xFFF06292),       // 亮粉
    secondary = Color(0xFFF8BBD0),            // 淡粉
    background = Color(0xFFFFFFFF),           // 纯白色背景
    surface = Color(0xFF281219),              // 深粉容器
    surfaceVariant = Color(0xFF321A23),       // 中深粉
    onPrimary = Color(0xFFFFFFFF),            // 白色文字
    onBackground = Color(0xFFE0E0E0),         // 浅灰文字
    onSurface = Color(0xFFF8BBD0)             // 淡粉文字
)

/**
 * 靛蓝主题
 */
val IndigoThemeColors = ThemeColors(
    primary = Color(0xFF3F51B5),              // 中等靛蓝
    primaryVariant = Color(0xFF5C6BC0),       // 亮靛蓝
    secondary = Color(0xFF7986CB),            // 淡靛蓝
    background = Color(0xFFFFFFFF),           // 纯白色背景
    surface = Color(0xFF121620),              // 深靛蓝容器
    surfaceVariant = Color(0xFF1A1F2C),       // 中深靛蓝
    onPrimary = Color(0xFFFFFFFF),            // 白色文字
    onBackground = Color(0xFFE0E0E0),         // 浅灰文字
    onSurface = Color(0xFFC5CAE9)             // 淡靛蓝文字
)

/**
 * 根据主题名称获取对应的ThemeColors
 */
fun getThemeColors(context: Context, themeName: String): ThemeColors {
    return when (themeName) {
        context.getString(R.string.theme_blue_purple) -> BluePurpleThemeColors
        context.getString(R.string.theme_green) -> GreenThemeColors
        context.getString(R.string.theme_orange) -> OrangeThemeColors
        context.getString(R.string.theme_pink) -> PinkThemeColors
        context.getString(R.string.theme_indigo) -> IndigoThemeColors
        else -> BluePurpleThemeColors  // 默认蓝紫主题
    }
}
