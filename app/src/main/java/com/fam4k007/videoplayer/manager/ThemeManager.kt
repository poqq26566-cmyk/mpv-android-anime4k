package com.fam4k007.videoplayer.manager

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.fam4k007.videoplayer.preferences.PreferencesManager

/**
 * 主题管理器 - 统一管理应用主题切换
 */
object ThemeManager {
    
    enum class ThemeMode {
        LIGHT,      // 亮色
        DARK,       // 深色
        SYSTEM      // 跟随系统
    }
    
    private const val PREF_KEY = "theme_mode"
    
    /**
     * 初始化主题 - 在应用启动时调用
     */
    fun initTheme(context: Context) {
        val preferencesManager = PreferencesManager.getInstance(context)
        val themeModeStr = preferencesManager.getThemeMode()
        applyTheme(parseThemeMode(themeModeStr))
    }
    
    /**
     * 应用主题
     */
    fun applyTheme(mode: ThemeMode) {
        when (mode) {
            ThemeMode.LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            ThemeMode.DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            ThemeMode.SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    // Android 9及以下使用电池节省模式作为判断
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }
    
    /**
     * 保存并应用主题
     */
    fun setTheme(context: Context, mode: ThemeMode) {
        val preferencesManager = PreferencesManager.getInstance(context)
        preferencesManager.setThemeMode(mode.name.lowercase())
        applyTheme(mode)
    }
    
    /**
     * 解析主题模式字符串
     */
    private fun parseThemeMode(modeStr: String): ThemeMode {
        return when (modeStr.lowercase()) {
            "dark" -> ThemeMode.DARK
            "system" -> ThemeMode.SYSTEM
            else -> ThemeMode.LIGHT
        }
    }
    
    /**
     * 获取当前主题模式
     */
    fun getCurrentThemeMode(context: Context): ThemeMode {
        val preferencesManager = PreferencesManager.getInstance(context)
        val themeModeStr = preferencesManager.getThemeMode()
        return parseThemeMode(themeModeStr)
    }
}
