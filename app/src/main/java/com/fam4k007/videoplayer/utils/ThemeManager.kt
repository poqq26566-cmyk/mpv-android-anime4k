package com.fam4k007.videoplayer.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.fam4k007.videoplayer.R

/**
 * 主题管理器
 * 负责主题的保存、加载和应用
 * 注意：Anime4K 相关的UI不受主题系统影响
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_DARK_MODE = "dark_mode"
    
    /**
     * 主题枚举
     */
    enum class Theme(val themeName: String, @StyleRes val styleRes: Int) {
        BLUE_PURPLE("Blue Purple", R.style.Theme_VideoPlayer_BluePurple),
        GREEN("Green", R.style.Theme_VideoPlayer_Green),
        ORANGE("Orange", R.style.Theme_VideoPlayer_Orange),
        PINK("Pink", R.style.Theme_VideoPlayer_Pink),
        INDIGO("Indigo", R.style.Theme_VideoPlayer_Indigo);
        
        companion object {
            fun fromName(name: String): Theme {
                return values().find { it.themeName == name } ?: BLUE_PURPLE
            }
        }
    }
    
    /**
     * 夜间模式枚举
     */
    enum class DarkMode(val modeName: String, val modeValue: Int) {
        FOLLOW_SYSTEM("跟随系统", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        LIGHT("日间模式", AppCompatDelegate.MODE_NIGHT_NO),
        DARK("夜间模式", AppCompatDelegate.MODE_NIGHT_YES);
        
        companion object {
            fun fromName(name: String): DarkMode {
                return values().find { it.modeName == name } ?: FOLLOW_SYSTEM
            }
        }
    }
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 获取当前主题
     */
    fun getCurrentTheme(context: Context): Theme {
        val prefs = getPreferences(context)
        val themeName = prefs.getString(KEY_THEME, Theme.BLUE_PURPLE.themeName)
        return Theme.fromName(themeName ?: Theme.BLUE_PURPLE.themeName)
    }
    
    /**
     * 设置主题
     * @param context Context
     * @param theme 要设置的主题
     */
    fun setTheme(context: Context, theme: Theme) {
        getPreferences(context).edit()
            .putString(KEY_THEME, theme.themeName)
            .apply()
    }
    
    /**
     * 获取夜间模式设置
     */
    fun getDarkMode(context: Context): DarkMode {
        val prefs = getPreferences(context)
        val modeName = prefs.getString(KEY_DARK_MODE, DarkMode.FOLLOW_SYSTEM.modeName)
        return DarkMode.fromName(modeName ?: DarkMode.FOLLOW_SYSTEM.modeName)
    }
    
    /**
     * 设置夜间模式
     * @param context Context
     * @param mode 要设置的夜间模式
     */
    fun setDarkMode(context: Context, mode: DarkMode) {
        getPreferences(context).edit()
            .putString(KEY_DARK_MODE, mode.modeName)
            .apply()
        
        // 立即应用夜间模式
        AppCompatDelegate.setDefaultNightMode(mode.modeValue)
    }
    
    /**
     * 应用主题到 Activity
     * 应该在 Activity 的 onCreate 中，super.onCreate() 之前调用
     */
    fun applyTheme(activity: AppCompatActivity) {
        // 应用主题样式
        val theme = getCurrentTheme(activity)
        activity.setTheme(theme.styleRes)
    }
    
    /**
     * 获取主题颜色属性值
     * @param context Context
     * @param attrResId 主题属性资源ID
     * @return 颜色值
     */
    fun getThemeColor(context: Context, attrResId: Int): Int {
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }
    
    /**
     * 获取所有可用主题
     */
    fun getAllThemes(): List<Theme> {
        return Theme.values().toList()
    }
    
    /**
     * 获取所有夜间模式选项
     */
    fun getAllDarkModes(): List<DarkMode> {
        return DarkMode.values().toList()
    }
    
    /**
     * 判断当前是否为夜间模式
     */
    fun isNightMode(context: Context): Boolean {
        val nightMode = context.resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
