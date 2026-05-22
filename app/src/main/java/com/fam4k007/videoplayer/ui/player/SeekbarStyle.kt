package com.fam4k007.videoplayer.ui.player

/**
 * 进度条样式枚举
 * 参考 mpvEx 设计，提供三种进度条样式
 */
enum class SeekbarStyle(val displayName: String) {
    Standard("标准"),
    Wavy("波浪"),
    Thick("粗条");

    companion object {
        fun fromName(name: String): SeekbarStyle {
            return entries.find { it.name == name } ?: Standard
        }
    }
}
