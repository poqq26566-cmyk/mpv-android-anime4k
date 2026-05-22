package com.fam4k007.videoplayer.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * 主题扩展工具类
 * 提供便捷的主题颜色获取方法
 */

/**
 * 从Context获取主题属性颜色
 * @param attrResId 主题属性资源ID（如 R.attr.colorDialogBackground）
 * @return 颜色值
 */
@ColorInt
fun Context.getThemeAttrColor(@AttrRes attrResId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrResId, typedValue, true)
    return typedValue.data
}

/**
 * 从View获取主题属性颜色
 * @param attrResId 主题属性资源ID
 * @return 颜色值
 */
@ColorInt
fun View.getThemeAttrColor(@AttrRes attrResId: Int): Int {
    return context.getThemeAttrColor(attrResId)
}

/**
 * 从Context获取主题属性资源ID
 * @param attrResId 主题属性资源ID
 * @return 资源ID
 */
fun Context.getThemeAttrResourceId(@AttrRes attrResId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrResId, typedValue, true)
    return typedValue.resourceId
}

/**
 * 设置TextView的文本颜色为主题颜色
 * @param attrResId 主题属性资源ID
 */
fun TextView.setThemeTextColor(@AttrRes attrResId: Int) {
    setTextColor(context.getThemeAttrColor(attrResId))
}

/**
 * 设置View的背景颜色为主题颜色
 * @param attrResId 主题属性资源ID
 */
fun View.setThemeBackgroundColor(@AttrRes attrResId: Int) {
    setBackgroundColor(context.getThemeAttrColor(attrResId))
}

/**
 * 批量获取多个主题颜色
 * @param attrResIds 主题属性资源ID数组
 * @return 颜色值数组
 */
fun Context.getThemeAttrColors(@AttrRes vararg attrResIds: Int): IntArray {
    return attrResIds.map { getThemeAttrColor(it) }.toIntArray()
}

/**
 * 判断当前是否为深色主题
 */
fun Context.isDarkTheme(): Boolean {
    val nightMode = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
}

/**
 * 判断当前View是否在深色主题下
 */
fun View.isDarkTheme(): Boolean {
    return context.isDarkTheme()
}
