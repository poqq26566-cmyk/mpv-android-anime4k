package com.fam4k007.videoplayer.danmaku

import android.content.Context
import com.fam4k007.videoplayer.preferences.PreferencesManager

/**
 * 弹幕配置管理
 * 参考 DanDanPlay 的 DanmuConfig
 */
object DanmakuConfig {
    
    private lateinit var preferencesManager: PreferencesManager
    
    // 弹幕开关
    var isEnabled: Boolean = true
        private set
    
    // 弹幕大小 (0-100)
    var size: Int = 50
        private set
    
    // 弹幕速度 (0-100)
    var speed: Int = 50
        private set
    
    // 弹幕透明度 (0-100)
    var alpha: Int = 100
        private set
    
    // 弹幕描边 (0-100)
    var stroke: Int = 50
        private set
    
    // 弹幕偏移时间（毫秒）
    var offsetTime: Long = 0L
        private set
    
    // 显示滚动弹幕
    var showScrollDanmaku: Boolean = true
        private set
    
    // 显示顶部弹幕
    var showTopDanmaku: Boolean = true
        private set
    
    // 显示底部弹幕
    var showBottomDanmaku: Boolean = true
        private set
    
    // 最大滚动弹幕行数 (0表示不限制)
    var maxScrollLine: Int = 0
        private set
    
    // 最大顶部弹幕行数
    var maxTopLine: Int = 0
        private set
    
    // 最大底部弹幕行数
    var maxBottomLine: Int = 0
        private set
    
    // 最大同屏弹幕数量 (0表示不限制，DanDanPlay 默认也是不限制)
    var maxScreenNum: Int = 0
        private set
    
    // 使用 Choreographer 更新（高刷新率适配，适配 60/90/120Hz 屏幕）
    var updateInChoreographer: Boolean = true
        private set
    
    // 弹幕调试模式
    var isDebug: Boolean = false
        private set
    
    fun init(context: Context) {
        preferencesManager = PreferencesManager.getInstance(context)
        loadConfig()
    }
    
    private fun loadConfig() {
        if (!::preferencesManager.isInitialized) return
        
        isEnabled = preferencesManager.getDanmakuEnabled()
        size = preferencesManager.getDanmakuSize()
        speed = preferencesManager.getDanmakuSpeed()
        alpha = preferencesManager.getDanmakuAlpha()
        stroke = preferencesManager.getDanmakuStroke()
        offsetTime = preferencesManager.getDanmakuOffsetTime()
        showScrollDanmaku = preferencesManager.getDanmakuShowScroll()
        showTopDanmaku = preferencesManager.getDanmakuShowTop()
        showBottomDanmaku = preferencesManager.getDanmakuShowBottom()
        maxScrollLine = preferencesManager.getDanmakuMaxScrollLine()
        maxTopLine = preferencesManager.getDanmakuMaxTopLine()
        maxBottomLine = preferencesManager.getDanmakuMaxBottomLine()
        maxScreenNum = preferencesManager.getDanmakuMaxScreenNum()
        updateInChoreographer = preferencesManager.getDanmakuUseChoreographer()
        isDebug = preferencesManager.getDanmakuDebug()
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuEnabled(enabled)
        }
    }
    
    fun setSize(value: Int) {
        size = value.coerceIn(0, 100)
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuSize(size)
        }
    }
    
    fun setSpeed(value: Int) {
        speed = value.coerceIn(0, 100)
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuSpeed(speed)
        }
    }
    
    fun setAlpha(value: Int) {
        alpha = value.coerceIn(0, 100)
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuAlpha(alpha)
        }
    }
    
    fun setStroke(value: Int) {
        stroke = value.coerceIn(0, 100)
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuStroke(stroke)
        }
    }
    
    fun setOffsetTime(time: Long) {
        offsetTime = time
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuOffsetTime(time)
        }
    }
    
    fun setShowScrollDanmaku(show: Boolean) {
        showScrollDanmaku = show
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuShowScroll(show)
        }
    }
    
    fun setShowTopDanmaku(show: Boolean) {
        showTopDanmaku = show
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuShowTop(show)
        }
    }
    
    fun setShowBottomDanmaku(show: Boolean) {
        showBottomDanmaku = show
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuShowBottom(show)
        }
    }
    
    fun setMaxScrollLine(line: Int) {
        maxScrollLine = line.coerceAtLeast(0)
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuMaxScrollLine(maxScrollLine)
        }
    }
    
    fun setMaxTopLine(line: Int) {
        maxTopLine = line.coerceAtLeast(0)
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuMaxTopLine(maxTopLine)
        }
    }
    
    fun setMaxBottomLine(line: Int) {
        maxBottomLine = line.coerceAtLeast(0)
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuMaxBottomLine(maxBottomLine)
        }
    }
    
    fun setMaxScreenNum(num: Int) {
        maxScreenNum = num.coerceAtLeast(0)
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuMaxScreenNum(maxScreenNum)
        }
    }
    
    fun setUpdateInChoreographer(use: Boolean) {
        updateInChoreographer = use
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuUseChoreographer(use)
        }
    }
    
    fun setDebug(debug: Boolean) {
        isDebug = debug
        if (::preferencesManager.isInitialized) {
            preferencesManager.setDanmakuDebug(debug)
        }
    }
}
