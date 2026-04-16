package com.fam4k007.videoplayer

/**
 * 应用全局常量定义
 * 统一管理所有配置常量、URL、默认值等
 */
object AppConstants {

    /**
     * SharedPreferences 配置
     */
    object Preferences {
        // 播放相关配置存储位置
        const val VIDEO_PLAYBACK = "VideoPlayback"
        const val PLAYER_PREFS = "player_preferences"
        const val PLAYBACK_HISTORY = "playback_history"

        // 播放设置 Key
        const val PRECISE_SEEKING = "precise_seeking"
        const val VOLUME_BOOST_ENABLED = "volume_boost_enabled"
        const val SEEK_TIME = "seek_time"
        const val LONG_PRESS_SPEED = "long_press_speed"
        const val ANIME4K_MEMORY_ENABLED = "anime4k_memory_enabled"
        const val ANIME4K_LAST_MODE = "anime4k_last_mode"
        
        // 双击手势设置 Key
        const val DOUBLE_TAP_MODE = "double_tap_mode"  // 0=暂停, 1=快进快退
        const val DOUBLE_TAP_SEEK_SECONDS = "double_tap_seek_seconds"  // 双击跳转秒数

        // 播放历史记录 Key
        const val HISTORY_LIST = "history_list"
    }

    /**
     * URL 配置
     */
    object URLs {
        const val GITHUB_URL = "https://github.com/azxcvn/mpv-android-anime4k"
        const val CONTACT_URL = "https://github.com/azxcvn"
        const val HOME_PAGE = "https://github.com/azxcvn/mpv-android-anime4k"
        const val GITHUB_ISSUES_URL = "https://github.com/azxcvn/mpv-android-anime4k/issues"
    }

    /**
     * 默认值配置
     */
    object Defaults {
        // 快进/快退时长（秒）
        const val DEFAULT_SEEK_TIME = 5
        const val MIN_SEEK_TIME = 3
        const val MAX_SEEK_TIME = 30

        // 长按倍速
        const val DEFAULT_LONG_PRESS_SPEED = 2.0f
        const val MIN_LONG_PRESS_SPEED = 1.5f
        const val MAX_LONG_PRESS_SPEED = 3.5f
        
        // 双击手势默认配置
        const val DEFAULT_DOUBLE_TAP_MODE = 0  // 默认为暂停/播放模式
        const val DEFAULT_DOUBLE_TAP_SEEK_SECONDS = 10  // 默认10秒
        const val MIN_DOUBLE_TAP_SEEK_SECONDS = 5
        const val MAX_DOUBLE_TAP_SEEK_SECONDS = 30

        // 播放历史最大记录数
        const val MAX_HISTORY_SIZE = 50
    }

    /**
     * UI 时间配置（毫秒）
     */
    object Timings {
        // 控制条自动隐藏时间
        const val CONTROL_AUTO_HIDE_TIME = 5000L

        // Toast 显示时长
        const val TOAST_DURATION = 2000L

        // 进度更新间隔
        const val PROGRESS_UPDATE_INTERVAL = 500L
    }

    /**
     * 文件相关常量
     */
    object Files {
        // 截图保存目录相对路径
        const val SCREENSHOT_DIR_NAME = "Screenshots"

        // 支持的视频文件扩展名
        val SUPPORTED_VIDEO_EXTENSIONS = arrayOf(
            "mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "m3u8", "mpd", "ts",
            "3gp", "3g2", "mxf", "ogv", "m2ts", "mts"
        )

        // 支持的字幕文件扩展名
        val SUPPORTED_SUBTITLE_EXTENSIONS = arrayOf(
            "srt", "ass", "ssa", "sub", "vtt", "sbv", "json"
        )
    }

    /**
     * 日志标签前缀
     */
    object Logging {
        const val TAG_PREFIX = "FAM4K007"
        const val TAG_PLAYBACK = "Playback"
        const val TAG_PLAYER = "Player"
        const val TAG_GESTURE = "Gesture"
        const val TAG_CONTROLS = "Controls"
    }
}
