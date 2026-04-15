package com.fam4k007.videoplayer.player

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import `is`.xyz.mpv.MPVLib
import java.lang.ref.WeakReference

/**
 * 手势处理管理器
 * 负责处理屏幕手势（左右滑动调节音量/亮度）
 */
class GestureHandler(
    private val contextRef: WeakReference<Context>,
    private val windowRef: WeakReference<android.view.Window>,
    private val callback: GestureCallback
) {

    companion object {
        private const val TAG = "GestureHandler"
        
        // 参考 mpvKt 的灵敏度设置
        private const val BRIGHTNESS_SENSITIVITY = 0.001f  // 亮度灵敏度
        private const val VOLUME_SENSITIVITY = 0.1f        // 音量灵敏度 (支持0.1%-100%) - 已降低敏感度
        
        // 进度灵敏度配置 - 参考 mpvEx 使用固定灵敏度，保证所有视频都能精确跳转
        // 使用中等灵敏度值，平衡精确度和操作便利性
        private const val SEEK_SENSITIVITY = 0.05f  // 固定灵敏度：每滑动1像素 ≈ 0.05秒
        
        // 音量配置 - 统一使用MPV音量控制
        private const val MIN_VOLUME = 0.1f       // 最小音量 0.1%
        private const val MAX_VOLUME = 300f       // 最大音量 300%
        private const val MAX_VOLUME_NO_BOOST = 100f  // 不开启音量增强时的最大音量 100%
        private const val DEFAULT_VOLUME = 100f   // 默认音量 100%
        
        // 偏好设置键名
        private const val PREF_NAME = "player_preferences"
        private const val KEY_PRECISE_SEEKING = "precise_seeking"
        private const val KEY_VOLUME_BOOST_ENABLED = "volume_boost_enabled"
    }
    
    // PlayerControlsManager引用（用于检查锁定状态）
    private var controlsManagerRef: WeakReference<PlayerControlsManager>? = null

    interface GestureCallback {
        fun onGestureStart()
        fun onGestureEnd()
        fun onLongPressRelease()  // 长按松手时调用
        fun onSingleTap()
        fun onDoubleTap()
        fun onLongPress()
        fun onSeekGesture(seekSeconds: Int, isRelativeSeek: Boolean = false)  // 双击用：相对seek
        fun onSeekStart(initialPosition: Int)  // 滑动开始，传递初始位置
        fun onSeekUpdate(targetPosition: Int, deltaSeconds: Int)  // 滑动中，实时更新目标位置
        fun onSeekEnd()  // 滑动结束
        fun getCurrentPosition(): Int  // 获取当前播放位置（秒）
    }

    // 音量和亮度控制
    private var audioManager: AudioManager? = null
    private var maxSystemVolume = 0        // 系统最大音量(仅用于确保系统音量在最大)
    private var maxBrightness = 255
    
    // 当前值 - 统一使用MPV音量 (0.1-300%)
    private var currentVolume = DEFAULT_VOLUME  // MPV音量值 (0.1%-300%)
    private var currentBrightness = 0.5f        // 亮度值 (0.0-1.0)
    
    // 精确进度控制设置
    private var usePreciseSeeking = false
    
    // 音量增强设置
    private var volumeBoostEnabled = false  // 默认关闭音量增强
    
    // 双击手势设置
    private var doubleTapMode = 1  // 0=暂停/播放, 1=快进/快退（默认为快进快退）
    private var doubleTapSeekSeconds = 10  // 双击跳转秒数
    
    // 保存进入播放器时的原始系统设置(退出时恢复)
    private var originalSystemVolume = -1
    private var originalSystemBrightness = -1f
    private var originalMPVVolumePercent = DEFAULT_VOLUME  // 保存进入时的MPV音量百分比
    
    // 手势起始值（每次手势开始时记录）
    private var gestureStartVolume = DEFAULT_VOLUME  // MPV音量起始值
    private var gestureStartBrightness = 0.5f
    private var gestureStartY = 0f
    
    // 手势状态
    var isAdjusting = false
        private set
    
    // 长按状态
    private var isLongPressing = false

    // 指示器隐藏Handler
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var brightnessHideRunnable: Runnable? = null
    private var volumeHideRunnable: Runnable? = null

    // 指示器视图
    private var brightnessIndicator: LinearLayout? = null
    private var volumeIndicator: LinearLayout? = null
    private var brightnessBar: View? = null
    private var volumeBar: View? = null
    private var brightnessText: TextView? = null
    private var volumeText: TextView? = null

    // 双击跳转指示器视图
    private var doubleTapSeekLeft: DoubleTapSeekIndicator? = null
    private var doubleTapSeekRight: DoubleTapSeekIndicator? = null
    
    // 双击手势标志位，用于防止触发滚动手势
    private var isDoubleTapping = false
    private val doubleTapResetDelay = 300L  // 双击后300ms内不响应滚动手势

    // 手势检测器
    val gestureDetector: GestureDetector by lazy {
        GestureDetector(contextRef.get(), GestureListener())
    }

    /**
     * 初始化
     */
    fun initialize() {
        val context = contextRef.get() ?: return
        val window = windowRef.get() ?: return
        
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxSystemVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        
        // 读取精确进度控制设置和音量增强设置
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        usePreciseSeeking = prefs.getBoolean(KEY_PRECISE_SEEKING, false)
        volumeBoostEnabled = prefs.getBoolean(KEY_VOLUME_BOOST_ENABLED, false)  // 默认关闭
        
        // 读取双击手势设置
        doubleTapMode = prefs.getInt(com.fam4k007.videoplayer.AppConstants.Preferences.DOUBLE_TAP_MODE, 
            com.fam4k007.videoplayer.AppConstants.Defaults.DEFAULT_DOUBLE_TAP_MODE)
        doubleTapSeekSeconds = prefs.getInt(com.fam4k007.videoplayer.AppConstants.Preferences.DOUBLE_TAP_SEEK_SECONDS,
            com.fam4k007.videoplayer.AppConstants.Defaults.DEFAULT_DOUBLE_TAP_SEEK_SECONDS)
        
        // 根据音量增强设置确定最大音量
        val effectiveMaxVolume = if (volumeBoostEnabled) MAX_VOLUME else MAX_VOLUME_NO_BOOST
        
        // 读取系统音量并转换为百分比
        val systemVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val systemVolumePercent = (systemVolume.toFloat() / maxSystemVolume * 100f).coerceIn(MIN_VOLUME, effectiveMaxVolume)
        
        // 保存原始系统音量和对应的MPV音量百分比(退出时恢复)
        originalSystemVolume = systemVolume
        originalMPVVolumePercent = systemVolumePercent  // 保存进入时的音量百分比
        
        if (volumeBoostEnabled) {
            // ========== 音量增强模式：系统音量设为最大，MPV完全控制 ==========
            // 将系统音量设置为最大，让硬件全功率输出
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxSystemVolume, 0)
            
            // MPV音量继承系统音量百分比
            try {
                currentVolume = systemVolumePercent
                MPVLib.setPropertyInt("volume", currentVolume.toInt())
                Log.d(TAG, "VolumeBoost ON - System: MAX, MPV: ${currentVolume.toInt()}% (from system $systemVolume/$maxSystemVolume)")
            } catch (e: Exception) {
                currentVolume = systemVolumePercent
                try {
                    MPVLib.setPropertyInt("volume", currentVolume.toInt())
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to set MPV volume", e2)
                }
            }
        } else {
            // ========== 普通模式：不调整系统音量，MPV音量=100%，系统音量控制实际音量 ==========
            // 不修改系统音量，保持原样
            // MPV音量固定100%，实际音量由系统控制
            try {
                // currentVolume 记录当前系统音量百分比(用于手势调节的起始值)
                currentVolume = systemVolumePercent
                // MPV固定100%
                MPVLib.setPropertyInt("volume", 100)
                Log.d(TAG, "VolumeBoost OFF - System: $systemVolume/$maxSystemVolume (${systemVolumePercent.toInt()}%), MPV: 100%, currentVolume: ${currentVolume.toInt()}%")
            } catch (e: Exception) {
                currentVolume = systemVolumePercent
                Log.e(TAG, "Failed to set MPV volume", e)
            }
        }
        
        // 保存并获取当前系统亮度
        originalSystemBrightness = window.attributes.screenBrightness
        
        if (originalSystemBrightness < 0) {
            try {
                val systemBrightness = Settings.System.getInt(
                    context.contentResolver, 
                    Settings.System.SCREEN_BRIGHTNESS
                ).toFloat()
                currentBrightness = systemBrightness / maxBrightness
                originalSystemBrightness = currentBrightness
            } catch (e: Exception) {
                currentBrightness = 0.5f
                originalSystemBrightness = 0.5f
            }
        } else {
            currentBrightness = originalSystemBrightness
        }
        
        Log.d(TAG, "Initialized - MPV Volume: ${currentVolume.toInt()}%, System Volume: MAX, Brightness: $currentBrightness, VolumeBoost: $volumeBoostEnabled")
    }

    /**
     * 重新应用音量增强设置
     * 用于从后台返回或Activity恢复时，重新应用音量增强模式的系统音量设置
     */
    fun reapplyVolumeBoostSettings() {
        val context = contextRef.get() ?: return
        
        // 重新读取音量增强设置(可能在设置界面被修改)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        volumeBoostEnabled = prefs.getBoolean(KEY_VOLUME_BOOST_ENABLED, false)  // 默认关闭
        
        if (volumeBoostEnabled) {
            // 音量增强模式：将系统音量重新设置为最大
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxSystemVolume, 0)
            Log.d(TAG, "Reapplied VolumeBoost - System volume set to MAX")
        }
        // 普通模式不需要做任何事，系统音量保持用户设置
    }

    /**
     * 绑定指示器视图
     */
    fun bindIndicatorViews(
        brightnessIndicator: LinearLayout,
        volumeIndicator: LinearLayout,
        brightnessBar: View,
        volumeBar: View,
        brightnessText: TextView,
        volumeText: TextView
    ) {
        this.brightnessIndicator = brightnessIndicator
        this.volumeIndicator = volumeIndicator
        this.brightnessBar = brightnessBar
        this.volumeBar = volumeBar
        this.brightnessText = brightnessText
        this.volumeText = volumeText
    }
    
    /**
     * 绑定双击跳转指示器视图
     */
    fun bindDoubleTapSeekIndicators(
        left: DoubleTapSeekIndicator,
        right: DoubleTapSeekIndicator
    ) {
        this.doubleTapSeekLeft = left
        this.doubleTapSeekRight = right
        
        // 初始化属性
        left.isForward = false  // 左侧是后退
        right.isForward = true  // 右侧是前进
    }
    
    /**
     * 设置PlayerControlsManager引用（用于检查锁定状态）
     */
    fun setControlsManager(controlsManager: PlayerControlsManager) {
        this.controlsManagerRef = WeakReference(controlsManager)
    }

    /**
     * 处理触摸事件
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        // 如果控制已锁定，只处理单击手势（用于切换解锁按钮显示），不处理其他手势
        if (controlsManagerRef?.get()?.isControlsLocked() == true) {
            // 锁定状态下，只让手势检测器处理单击事件
            val result = gestureDetector.onTouchEvent(event)
            // 不处理滑动手势的后续逻辑
            return result
        }
        
        val result = gestureDetector.onTouchEvent(event)
        
        // 触摸结束时重置并隐藏指示器
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            // 如果正在seek中，通知结束
            if (isAdjusting) {
                callback.onSeekEnd()
            }
            
            // 如果是长按松手，调用释放回调
            if (isLongPressing) {
                callback.onLongPressRelease()
                isLongPressing = false
            }
            
            // 重置手势起始值
            gestureStartY = 0f
            gestureStartVolume = currentVolume
            gestureStartBrightness = currentBrightness
            
            // 如果是长按倍速，触摸结束时恢复正常速度
            callback.onGestureEnd()
            
            // 延迟隐藏指示器
            hideIndicatorDelayed(brightnessIndicator, 1500)
            hideIndicatorDelayed(volumeIndicator, 1500)
            
            // 使用已有handler延迟重置手势标志
            hideHandler.postDelayed({
                isAdjusting = false
            }, 100)
        }
        
        return result
    }

    /**
     * 计算新亮度值（纯计算，无副作用）
     * @param startY 手势开始Y坐标
     * @param currentY 当前Y坐标
     * @param baseValue 基础亮度值
     * @return 计算后的亮度值（0.01-1.0）
     */
    private fun calculateBrightness(startY: Float, currentY: Float, baseValue: Float): Float {
        val newBrightness = baseValue + ((startY - currentY) * BRIGHTNESS_SENSITIVITY)
        return newBrightness.coerceIn(0.01f, 1.0f)
    }

    /**
     * 调整亮度 - 使用 mpvKt 的算法
     * @param startY 手势开始的Y坐标
     * @param currentY 当前Y坐标
     */
    private fun adjustBrightness(startY: Float, currentY: Float) {
        if (gestureStartY == 0f) {
            gestureStartY = startY
            gestureStartBrightness = currentBrightness
        }
        
        // 计算新亮度值
        currentBrightness = calculateBrightness(gestureStartY, currentY, gestureStartBrightness)
        
        // 应用亮度
        try {
            windowRef.get()?.let { window ->
                val layoutParams = window.attributes
                layoutParams.screenBrightness = currentBrightness
                window.attributes = layoutParams
            }
            Log.d(TAG, "Brightness: ${(currentBrightness * 100).toInt()}%")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
        }
        
        showBrightnessIndicator()
    }

    /**
     * 计算系统音量（纯计算，无副作用）
     * @param startY 手势开始Y坐标
     * @param currentY 当前Y坐标
     * @param baseValue 基础音量值
     * @return 计算后的音量值（0-maxVolume）
     */
    /**
     * 计算MPV音量值（纯计算，无副作用）
     * @param startY 手势开始Y坐标
     * @param currentY 当前Y坐标
     * @param baseValue 基础音量（0.1-300%）
     * @return 计算后的音量值（根据音量增强设置: 0.1-100% 或 0.1-300%）
     */
    private fun calculateVolume(startY: Float, currentY: Float, baseValue: Float): Float {
        val dragAmount = startY - currentY
        val newVolume = baseValue + (dragAmount * VOLUME_SENSITIVITY)
        
        // 根据音量增强设置限制最大值
        val maxVol = if (volumeBoostEnabled) MAX_VOLUME else MAX_VOLUME_NO_BOOST
        return newVolume.coerceIn(MIN_VOLUME, maxVol)
    }

    /**
     * 调整音量
     * 根据音量增强模式采用不同策略
     * @param startY 手势开始的Y坐标
     * @param currentY 当前Y坐标
     */
    private fun adjustVolume(startY: Float, currentY: Float) {
        // 记录手势起始值
        if (gestureStartY == 0f) {
            gestureStartY = startY
            gestureStartVolume = currentVolume
        }
        
        // 计算新音量
        currentVolume = calculateVolume(gestureStartY, currentY, gestureStartVolume)
        
        if (volumeBoostEnabled) {
            // ========== 音量增强模式：只调整MPV音量（系统音量已在最大） ==========
            try {
                MPVLib.setPropertyInt("volume", currentVolume.toInt())
                Log.d(TAG, "VolumeBoost ON - MPV Volume: ${String.format("%.1f", currentVolume)}%")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set MPV volume", e)
            }
        } else {
            // ========== 普通模式：同时调整系统音量，MPV保持100% ==========
            try {
                // 将MPV的百分比转换为系统音量值
                val systemVolumeValue = (currentVolume / 100f * maxSystemVolume).toInt()
                    .coerceIn(0, maxSystemVolume)
                
                // 调整系统音量
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, systemVolumeValue, 0)
                
                // MPV保持100%
                MPVLib.setPropertyInt("volume", 100)
                
                Log.d(TAG, "VolumeBoost OFF - System: $systemVolumeValue/$maxSystemVolume (${currentVolume.toInt()}%), MPV: 100%")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to adjust volume", e)
            }
        }
        
        showVolumeIndicator()
    }

    /**
     * 显示亮度指示器
     */
    private fun showBrightnessIndicator() {
        brightnessIndicator?.visibility = View.VISIBLE
        
        val context = contextRef.get() ?: return
        val maxHeight = 120 * context.resources.displayMetrics.density
        
        // 直接使用百分比计算高度
        val barHeight = (maxHeight * currentBrightness).toInt()
        
        brightnessBar?.layoutParams?.height = barHeight
        brightnessBar?.requestLayout()
        brightnessText?.text = "${(currentBrightness * 100).toInt()}"
    }

    /**
     * 显示音量指示器
     */
    private fun showVolumeIndicator() {
        volumeIndicator?.visibility = View.VISIBLE
        
        val context = contextRef.get() ?: return
        val maxHeight = 120 * context.resources.displayMetrics.density
        
        // 根据音量增强设置调整显示逻辑
        val (displayPercent, barPercent) = if (volumeBoostEnabled) {
            // 音量增强启用: 0.1%-100% 映射到 0-50% 高度, 100%-300% 映射到 50-100% 高度
            when {
                currentVolume <= 100f -> {
                    val percent = currentVolume / 100f  // 0.001-1.0
                    Pair(currentVolume, percent * 0.5f)  // 高度占0-50%
                }
                else -> {
                    val percent = (currentVolume - 100f) / 200f  // 0-1.0
                    Pair(currentVolume, 0.5f + percent * 0.5f)  // 高度占50-100%
                }
            }
        } else {
            // 音量增强关闭: 1%-100% 线性映射到 0-100% 高度
            val percent = currentVolume / 100f  // 0.01-1.0
            Pair(currentVolume, percent)  // 高度占0-100%
        }
        
        val barHeight = (maxHeight * barPercent).toInt()
        volumeBar?.layoutParams?.height = barHeight
        volumeBar?.requestLayout()
        
        // 显示格式: 0.1% - 99.9% 显示一位小数, 100%+ 显示整数
        val text = if (currentVolume < 100f) {
            String.format("%.1f%%", displayPercent)
        } else {
            "${displayPercent.toInt()}%"
        }
        volumeText?.text = text
    }

    private fun hideIndicatorDelayed(indicator: LinearLayout?, delayMillis: Long) {
        hideHandler.postDelayed({
            indicator?.visibility = View.GONE
        }, delayMillis)
    }

    /**
     * 显示双击跳转动画
     * @param isForward true=快进(右侧), false=快退(左侧)
     * @param seconds 跳转秒数
     */
    private fun showDoubleTapSeekAnimation(isForward: Boolean, seconds: Int) {
        if (isForward) {
            // 显示右侧快进动画
            doubleTapSeekRight?.let { indicator ->
                indicator.seconds = seconds
                indicator.visibility = View.VISIBLE
                indicator.start()
                
                // 600ms后自动隐藏
                hideHandler.postDelayed({
                    indicator.stop()
                    indicator.visibility = View.GONE
                }, 600)
            }
        } else {
            // 显示左侧快退动画
            doubleTapSeekLeft?.let { indicator ->
                indicator.seconds = -seconds
                indicator.visibility = View.VISIBLE
                indicator.start()
                
                // 600ms后自动隐藏
                hideHandler.postDelayed({
                    indicator.stop()
                    indicator.visibility = View.GONE
                }, 600)
            }
        }
    }

    /**
     * 计算Seek值（纯计算，无副作用）
     * 参考 mpvEx 的算法：使用固定灵敏度，保证所有视频都能精确跳转
     * @param deltaX 水平滑动距离（像素）
     * @return seek偏移秒数（相对于手势开始位置）
     */
    private fun calculateSeek(deltaX: Float): Int {
        // 使用固定灵敏度：滑动距离 × 灵敏度系数 = 视频跳转时间
        // 这样无论视频多长，都能通过滑动精确控制跳转位置
        return (deltaX * SEEK_SENSITIVITY).toInt()
    }

    /**
     * 手势类型枚举
     */
    private enum class GestureType {
        NONE,
        BRIGHTNESS,  // 左侧上下滑动
        VOLUME,      // 右侧上下滑动
        SEEK         // 中间左右滑动
    }

    /**
     * 手势监听器
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private var gestureType = GestureType.NONE
        private var initialX = 0f
        private var initialY = 0f
        
        // 缓存屏幕尺寸，避免每次 onScroll 都重新获取
        private var cachedScreenWidth = 0
        private var cachedScreenHeight = 0
        
        // 滑动seek的状态（参考 mpvEx）
        private var isSeekingActive = false
        private var seekInitialPosition = 0  // seek开始时的视频位置（秒）

        override fun onDown(e: MotionEvent): Boolean {
            initialX = e.x
            initialY = e.y
            gestureType = GestureType.NONE
            isSeekingActive = false  // 重置seek状态
            
            // 首次或屏幕尺寸变化时更新缓存（避免每次 onScroll 都获取）
            val context = contextRef.get()
              if (context != null) {
                  val width = context.resources.displayMetrics.widthPixels
                  val height = context.resources.displayMetrics.heightPixels
                  if (width != cachedScreenWidth || height != cachedScreenHeight) {
                      cachedScreenWidth = width
                      cachedScreenHeight = height
                      Log.d(TAG, "Screen size cached: ${cachedScreenWidth}x${cachedScreenHeight}")
                  }
              }
              
              return true
          }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isAdjusting) {
                callback.onSingleTap()
            }
            return true
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // 如果控制已锁定，不响应双击
            if (controlsManagerRef?.get()?.isControlsLocked() == true) {
                return false
            }
            
            // 设置双击标志位，防止触发滚动手势
            isDoubleTapping = true
            hideHandler.postDelayed({
                isDoubleTapping = false
            }, doubleTapResetDelay)
            
            // 根据双击模式选择行为
            when (doubleTapMode) {
                0 -> {
                    // 模式0: 双击暂停/播放（全屏任意位置）
                    callback.onDoubleTap()
                }
                1 -> {
                    // 模式1: 双击快进/快退（左半屏快退，右半屏快进）
                    val screenWidth = cachedScreenWidth
                    val isLeftHalf = e.x < screenWidth / 2
                    
                    if (isLeftHalf) {
                        // 左半屏：快退，显示左侧动画
                        showDoubleTapSeekAnimation(false, doubleTapSeekSeconds)
                        callback.onSeekGesture(-doubleTapSeekSeconds, isRelativeSeek = true)
                    } else {
                        // 右半屏：快进，显示右侧动画
                        showDoubleTapSeekAnimation(true, doubleTapSeekSeconds)
                        callback.onSeekGesture(doubleTapSeekSeconds, isRelativeSeek = true)
                    }
                }
            }
            
            return true
        }
        
        override fun onLongPress(e: MotionEvent) {
            // 如果控制已锁定，不响应长按
            if (controlsManagerRef?.get()?.isControlsLocked() == true) {
                return
            }
            // 长按全屏响应 - 触发倍速播放
            isLongPressing = true
            callback.onLongPress()
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (e1 == null) return false
            
            // 如果控制已锁定，不处理滑动手势
            if (controlsManagerRef?.get()?.isControlsLocked() == true) {
                return false
            }
            
            // 如果刚刚双击，忽略滚动手势（防止手势冲突）
            if (isDoubleTapping) {
                return false
            }
            
            // 使用缓存的屏幕尺寸，避免每次都调用 context.resources.displayMetrics
            val screenWidth = cachedScreenWidth
            val screenHeight = cachedScreenHeight
            
            // 计算滑动距离
            val deltaX = e2.x - initialX
            val deltaY = e2.y - initialY
            
            // 首次判断手势类型（根据滑动方向和位置）
            if (gestureType == GestureType.NONE) {
                val absX = kotlin.math.abs(deltaX)
                val absY = kotlin.math.abs(deltaY)
                
                // 判断是横向还是纵向滑动（降低阈值，减少死区）
                if (absX > 20 || absY > 20) {  // 从30px降低到20px
                    if (absX > absY * 1.2f) {  // 从1.5降低到1.2，减少斜向滑动死区
                        // 横向滑动 - 简化进度条区域检测
                        // 只保护屏幕底部15%区域（进度条通常在底部）
                        if (initialY > screenHeight * 0.85f) {
                            // 在底部区域，可能是点击进度条，不响应横向手势
                            return false
                        }
                        gestureType = GestureType.SEEK
                        // 记录seek开始时的视频位置（只记录一次）
                        seekInitialPosition = callback.getCurrentPosition()
                    } else if (absY > absX * 1.2f) {  // 从1.5降低到1.2
                        // 纵向滑动 - 调整音量/亮度（参考 mpvKt：屏幕左半边=亮度，右半边=音量）
                        if (initialX < screenWidth / 2) {
                            gestureType = GestureType.BRIGHTNESS
                        } else {
                            gestureType = GestureType.VOLUME
                        }
                    }
                }
            }
            
            // 根据手势类型执行相应操作（参考 mpvEx 的实现）
            when (gestureType) {
                GestureType.BRIGHTNESS -> {
                    if (!isAdjusting) {
                        isAdjusting = true
                        callback.onGestureStart()
                    }
                    adjustBrightness(initialY, e2.y)
                }
                GestureType.VOLUME -> {
                    if (!isAdjusting) {
                        isAdjusting = true
                        callback.onGestureStart()
                    }
                    adjustVolume(initialY, e2.y)
                }
                GestureType.SEEK -> {
                    // 第一次进入seek模式时，初始化
                    if (!isSeekingActive) {
                        isSeekingActive = true
                        isAdjusting = true
                        // 通知Activity开始seek，传递初始位置
                        callback.onSeekStart(seekInitialPosition)
                    }
                    
                    // 计算目标位置（基于初始位置 + 滑动偏移）
                    val seekOffset = calculateSeek(deltaX)
                    val targetPosition = seekInitialPosition + seekOffset
                    
                    // 通知Activity实时更新seek位置
                    callback.onSeekUpdate(targetPosition, seekOffset)
                }
                GestureType.NONE -> {
                    // 还未确定手势类型
                }
            }
            
            return true
        }
    }

    /**
     * 获取精确进度控制设置
     */
    fun isPreciseSeekingEnabled(): Boolean = usePreciseSeeking
    
    /**
     * 设置精确进度控制
     */
    fun setPreciseSeeking(enabled: Boolean) {
        usePreciseSeeking = enabled
        val context = contextRef.get() ?: return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PRECISE_SEEKING, enabled).apply()
        Log.d(TAG, "Precise seeking ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 更新双击手势设置（从设置界面调用）
     */
    fun updateDoubleTapSettings(mode: Int, seekSeconds: Int) {
        doubleTapMode = mode
        doubleTapSeekSeconds = seekSeconds
        Log.d(TAG, "Double tap settings updated - mode: $mode, seekSeconds: $seekSeconds")
    }
    
    /**
     * 恢复原始系统设置(退出播放器时调用)
     */
    fun restoreOriginalSettings() {
        try {
            // 先恢复MPV音量，再恢复系统音量，避免音量状态混乱
            if (volumeBoostEnabled) {
                // 音量增强模式：将MPV恢复到原始百分比
                // 这样下次进入时会重新读取系统音量并初始化
                try {
                    MPVLib.setPropertyInt("volume", originalMPVVolumePercent.toInt())
                    Log.d(TAG, "VolumeBoost ON - Restored MPV volume to original: ${originalMPVVolumePercent.toInt()}%")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore MPV volume", e)
                }
            } else {
                // 普通模式：MPV保持100%
                try {
                    MPVLib.setPropertyInt("volume", 100)
                    Log.d(TAG, "VolumeBoost OFF - Restored MPV volume to: 100%")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore MPV volume", e)
                }
            }
            
            // 恢复原始系统音量
            if (originalSystemVolume >= 0) {
                try {
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalSystemVolume, 0)
                    Log.d(TAG, "Restored system volume: $originalSystemVolume/$maxSystemVolume")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore system volume", e)
                }
            }
            
            // 恢复原始亮度
            if (originalSystemBrightness >= 0) {
                try {
                    windowRef.get()?.let { window ->
                        val layoutParams = window.attributes
                        layoutParams.screenBrightness = originalSystemBrightness
                        window.attributes = layoutParams
                        Log.d(TAG, "Restored original brightness: $originalSystemBrightness")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore original brightness", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during restoreOriginalSettings", e)
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 移除所有 pending callbacks，防止内存泄漏
        brightnessHideRunnable?.let { hideHandler.removeCallbacks(it) }
        volumeHideRunnable?.let { hideHandler.removeCallbacks(it) }
        
        // 清空引用
        audioManager = null
        brightnessIndicator = null
        volumeIndicator = null
        brightnessBar = null
        volumeBar = null
        brightnessText = null
        volumeText = null
        
        Log.d(TAG, "Cleanup completed - all handlers cleared")
    }
}
