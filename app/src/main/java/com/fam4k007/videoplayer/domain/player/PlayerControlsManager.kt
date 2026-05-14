package com.fam4k007.videoplayer.domain.player

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fam4k007.videoplayer.domain.player.GestureHandler
import java.lang.ref.WeakReference


/**
 * 播放器控制管理器
 * 负责UI控制面板的显示/隐藏、按钮事件、进度更新等
 * 使用 WeakReference 防止内存泄漏
 */
class PlayerControlsManager(
    private val activityRef: WeakReference<AppCompatActivity>,
    private val callback: ControlsCallback,
    private val gestureHandlerRef: WeakReference<GestureHandler>? = null
) {

    companion object {
        private const val TAG = "PlayerControlsManager"
        private const val AUTO_HIDE_DELAY = 5000L
    }

    interface ControlsCallback {
        fun onPlayPauseClick()
        fun onPreviousClick()
        fun onNextClick()
        fun onRewindClick()
        fun onForwardClick()
        fun onAudioTrackClick()
        fun onSubtitleClick()  // 新增：字幕按钮回调
        fun onDecoderClick()
        fun onAnime4KClick()
        fun onMoreClick()
        fun onSpeedClick()
        fun onSeekBarChange(position: Double)
        fun onBackClick()
        fun onControlsVisibilityChanged(visible: Boolean)
        fun onAspectRatioClick()  // 新增：画面比例按钮回调
        fun onLockClick()  // 新增：锁定按钮回调
        fun onVideoTitleClick()  // 新增：视频标题点击回调
        fun onControlsShown()  // 新增：控制栏显示时回调（用于隐藏暂停指示器）
    }

    // UI 组件（顶部面板已迁移至 Compose，仅保留底部面板和提示）
    private var resumePlaybackPrompt: LinearLayout? = null
    private var tvResumeConfirm: TextView? = null

    // 状态
    var isVisible = true
        private set
    private var isPlaying = true  // 记录播放状态
    private var hasActivePopup = false  // 记录是否有弹窗显示
    var isLocked = false  // 锁定状态
        private set
    
    // Handler（使用 WeakReference）
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable {
        hideControls()
    }


    /**
     * 绑定UI组件（顶部面板已迁到 Compose，仅保留恢复播放提示）
     */
    fun bindViews(
        resumePlaybackPrompt: LinearLayout,
        tvResumeConfirm: TextView
    ) {
        this.resumePlaybackPrompt = resumePlaybackPrompt
        this.tvResumeConfirm = tvResumeConfirm
        setupClickListeners()
    }

    /**
     * 初始化
     */
    fun initialize() {
        // 立即隐藏系统 UI
        hideSystemUI()
        
        // 启动自动隐藏
        resetAutoHideTimer()
        notifyControlsVisibilityChanged()
        
        Log.d(TAG, "PlayerControlsManager initialized")
    }

    /**
     * 设置按钮点击监听
     */
    private fun setupClickListeners() {
        tvResumeConfirm?.setOnClickListener {
            resumePlaybackPrompt?.visibility = View.GONE
        }
    }

    /**
     * 显示恢复播放提示
     */
    fun showResumePrompt() {
        resumePlaybackPrompt?.visibility = View.VISIBLE
        
        // 5秒后自动隐藏
        handler.postDelayed({
            resumePlaybackPrompt?.visibility = View.GONE
        }, 5000)
    }

    /**
     * 切换控制面板显示/隐藏
     */
    fun toggleControls() {
        if (isVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    /**
     * 显示控制面板
     */
    fun showControls() {
        if (isVisible) return
        
        // 显示控制栏时立即隐藏暂停指示器
        callback.onControlsShown()
        
        isVisible = true
        notifyControlsVisibilityChanged()
        resetAutoHideTimer()
    }

    /**
     * 隐藏控制面板
     */
    fun hideControls() {
        if (!isVisible) return
        
        isVisible = false
        notifyControlsVisibilityChanged()
        handler.removeCallbacks(hideControlsRunnable)
    }

    /**
     * 重置自动隐藏定时器
     */
    fun resetAutoHideTimer() {
        handler.removeCallbacks(hideControlsRunnable)
        // 只有在播放中且没有弹窗时才启动自动隐藏
        if (isPlaying && !hasActivePopup) {
            handler.postDelayed(hideControlsRunnable, AUTO_HIDE_DELAY)
        }
    }

    /**
     * 停止自动隐藏（暂停时）
     */
    fun stopAutoHide() {
        handler.removeCallbacks(hideControlsRunnable)
    }

    /**
     * 设置弹窗显示状态
     */
    fun setPopupVisible(visible: Boolean) {
        hasActivePopup = visible
        if (visible) {
            // 弹窗显示时，停止自动隐藏
            handler.removeCallbacks(hideControlsRunnable)
        } else {
            // 弹窗关闭时，立即重新隐藏系统UI(修复Android 12及以下版本系统栏显示问题)
            hideSystemUI()
            // 如果在播放中则重新启动自动隐藏
            if (isPlaying) {
                resetAutoHideTimer()
            }
        }
    }

    /**
     * 隐藏系统UI
     */
    fun hideSystemUI() {
        val activity = activityRef.get() ?: return
        val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController.apply {
            // 允许通过手势临时呼出系统栏
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun formatTime(seconds: Double): String {
        val totalSeconds = seconds.toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        
        return if (hours > 0) {
            // 超过1小时，显示 时:分:秒
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            // 小于1小时，显示 分:秒
            String.format("%02d:%02d", minutes, secs)
        }
    }

    /**
     * 切换锁定状态
     */
    fun toggleLock() {
        isLocked = !isLocked
        if (isLocked) {
            // 锁定：停止自动隐藏定时器
            handler.removeCallbacks(hideControlsRunnable)
            Log.d(TAG, "Controls locked")
        } else {
            // 解锁：重新启动自动隐藏定时器
            resetAutoHideTimer()
            Log.d(TAG, "Controls unlocked")
        }
        notifyControlsVisibilityChanged()
    }
    
    /**
     * 获取锁定状态
     */
    fun isControlsLocked(): Boolean {
        return isLocked
    }

    private fun notifyControlsVisibilityChanged() {
        callback.onControlsVisibilityChanged(isVisible && !isLocked)
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up PlayerControlsManager")
        handler.removeCallbacks(hideControlsRunnable)
        resumePlaybackPrompt = null
        tvResumeConfirm = null
    }
}
