package com.fam4k007.videoplayer.utils

import android.content.Context
import android.widget.Toast

/**
 * Toast 工具类
 * 对话框功能已迁移到 Compose 组件
 */
object DialogUtils {

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    fun showToastShort(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_SHORT)
    }

    fun showToastLong(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_LONG)
    }
}
