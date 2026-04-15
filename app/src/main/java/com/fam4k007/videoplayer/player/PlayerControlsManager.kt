package com.fam4k007.videoplayer.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

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

    // UI 组件
    private var topInfoPanel: LinearLayout? = null
    private var controlPanel: LinearLayout? = null
    private var topGradientBackground: View? = null  // 顶部渐变阴影背景层
    private var bottomGradientBackground: View? = null  // 底部渐变阴影背景层
    
    private var tvFileName: TextView? = null
    private var titleClickArea: android.view.View? = null  // 标题点击区域
    private var tvBattery: TextView? = null
    private var tvTime: TextView? = null
    private var tvTimeInfo: TextView? = null
    
    private var btnPlayPause: ImageView? = null
    private var btnPrevious: ImageView? = null
    private var btnNext: ImageView? = null
    private var btnRewind: ImageView? = null
    private var btnForward: ImageView? = null
    private var btnBack: ImageView? = null
    private var btnSubtitle: ImageView? = null  // 新增：字幕按钮
    private var btnAspectRatio: ImageView? = null  // 新增：画面比例按钮
    private var btnLock: ImageView? = null  // 新增：锁定按钮
    private var btnUnlock: ImageView? = null  // 新增：解锁按钮（左侧）
    private var btnUnlockRight: ImageView? = null  // 新增：解锁按钮（右侧）
    private var btnMore: ImageView? = null
    private var btnSpeed: ImageView? = null
    private var btnAnime4K: Button? = null
    
    private var seekBar: SeekBar? = null
    
    // 恢复播放提示
    private var resumePlaybackPrompt: LinearLayout? = null
    private var tvResumeConfirm: TextView? = null

    // 状态
    var isVisible = true
        private set
    private var isPlaying = true  // 记录播放状态
    private var hasActivePopup = false  // 记录是否有弹窗显示
    var isLocked = false  // 新增：锁定状态
        private set
    private var isUnlockButtonVisible = true  // 新增：解锁按钮可见状态
    
    // Handler（使用 WeakReference）
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable {
        hideControls()
    }
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 60000) // 每分钟更新
        }
    }
    private val hideUnlockButtonRunnable = Runnable {
        // 自动隐藏解锁按钮（左右两侧）
        if (isLocked && isUnlockButtonVisible) {
            isUnlockButtonVisible = false
            // 左侧按钮淡出
            btnUnlock?.animate()
                ?.alpha(0f)
                ?.setDuration(300)
                ?.setInterpolator(android.view.animation.AccelerateInterpolator())
                ?.withEndAction {
                    btnUnlock?.visibility = View.GONE
                }
                ?.start()
            // 右侧按钮淡出
            btnUnlockRight?.animate()
                ?.alpha(0f)
                ?.setDuration(300)
                ?.setInterpolator(android.view.animation.AccelerateInterpolator())
                ?.withEndAction {
                    btnUnlockRight?.visibility = View.GONE
                }
                ?.start()
            Log.d(TAG, "Unlock buttons auto-hidden")
        }
    }

    // 电池监听
    private var batteryReceiver: BroadcastReceiver? = null

    /**
     * 绑定UI组件
     */
    fun bindViews(
        topInfoPanel: LinearLayout,
        controlPanel: LinearLayout,
        topGradientBackground: View,  // 新增参数
        bottomGradientBackground: View,  // 新增参数
        tvFileName: TextView,
        titleClickArea: android.view.View,  // 标题点击区域
        tvBattery: TextView,
        tvTime: TextView,
        tvTimeInfo: TextView,
        btnPlayPause: ImageView,
        btnPrevious: ImageView,
        btnNext: ImageView,
        btnRewind: ImageView,
        btnForward: ImageView,
        btnBack: ImageView,
        btnSubtitle: ImageView,  // 新增参数
        btnAspectRatio: ImageView,  // 新增参数
        btnLock: ImageView,  // 新增参数
        btnUnlock: ImageView,  // 新增参数（左侧）
        btnUnlockRight: ImageView,  // 新增参数（右侧）
        btnMore: ImageView,
        btnSpeed: ImageView,
        btnAnime4K: Button,
        seekBar: SeekBar,
        resumePlaybackPrompt: LinearLayout,
        tvResumeConfirm: TextView
    ) {
        this.topInfoPanel = topInfoPanel
        this.controlPanel = controlPanel
        this.topGradientBackground = topGradientBackground
        this.bottomGradientBackground = bottomGradientBackground
        
        this.tvFileName = tvFileName
        this.titleClickArea = titleClickArea
        this.tvBattery = tvBattery
        this.tvTime = tvTime
        this.tvTimeInfo = tvTimeInfo
        
        this.btnPlayPause = btnPlayPause
        this.btnPrevious = btnPrevious
        this.btnNext = btnNext
        this.btnRewind = btnRewind
        this.btnForward = btnForward
        this.btnBack = btnBack
        this.btnSubtitle = btnSubtitle  // 初始化字幕按钮
        this.btnAspectRatio = btnAspectRatio  // 初始化画面比例按钮
        this.btnLock = btnLock  // 初始化锁定按钮
        this.btnUnlock = btnUnlock  // 初始化解锁按钮（左侧）
        this.btnUnlockRight = btnUnlockRight  // 初始化解锁按钮（右侧）
        this.btnMore = btnMore
        this.btnSpeed = btnSpeed
        this.btnAnime4K = btnAnime4K
        
        this.seekBar = seekBar
        
        this.resumePlaybackPrompt = resumePlaybackPrompt
        this.tvResumeConfirm = tvResumeConfirm
        
        setupClickListeners()
        setupSeekBar()
    }

    /**
     * 初始化
     */
    fun initialize() {
        val activity = activityRef.get() ?: return
        
        // 立即隐藏系统UI
        hideSystemUI()
        
        // 初始化电池监听
        initBatteryMonitor(activity)
        
        // 初始化时间
        updateTime()
        handler.post(timeUpdateRunnable)
        
        // 启动自动隐藏
        resetAutoHideTimer()
        notifyControlsVisibilityChanged()
        
        Log.d(TAG, "PlayerControlsManager initialized")
    }

    /**
     * 设置按钮点击监听
     */
    private fun setupClickListeners() {
        btnBack?.setOnClickListener {
            callback.onBackClick()
        }
        
        // 视频标题区域点击（显示视频列表）- 整个区域都可以点击
        titleClickArea?.setOnClickListener {
            callback.onVideoTitleClick()
            resetAutoHideTimer()
        }
        
        btnPlayPause?.setOnClickListener {
            callback.onPlayPauseClick()
            resetAutoHideTimer()
        }
        
        btnPrevious?.setOnClickListener {
            callback.onPreviousClick()
        }
        
        btnNext?.setOnClickListener {
            callback.onNextClick()
        }
        
        btnRewind?.setOnClickListener {
            callback.onRewindClick()
            resetAutoHideTimer()
        }
        
        btnForward?.setOnClickListener {
            callback.onForwardClick()
            resetAutoHideTimer()
        }
        
        btnSubtitle?.setOnClickListener {
            callback.onSubtitleClick()
            resetAutoHideTimer()
        }
        
        btnAspectRatio?.setOnClickListener {
            callback.onAspectRatioClick()
            resetAutoHideTimer()
        }
        
        btnLock?.setOnClickListener {
            callback.onLockClick()
            // 锁定时不重置自动隐藏定时器
        }
        
        btnUnlock?.setOnClickListener {
            callback.onLockClick()
            // 解锁时不重置自动隐藏定时器
        }
        
        btnUnlockRight?.setOnClickListener {
            callback.onLockClick()
            // 解锁时不重置自动隐藏定时器
        }
        
        btnAnime4K?.setOnClickListener {
            callback.onAnime4KClick()
            resetAutoHideTimer()
        }
        
        btnMore?.setOnClickListener {
            callback.onMoreClick()
            resetAutoHideTimer()
        }
        
        btnSpeed?.setOnClickListener {
            callback.onSpeedClick()
            resetAutoHideTimer()
        }
        
        tvResumeConfirm?.setOnClickListener {
            resumePlaybackPrompt?.visibility = View.GONE
        }
    }

    // 当前视频时长（用于进度条计算）
    private var currentDuration: Double = 0.0
    
    // 用于防止进度条拖动时频繁触发seek
    private var isUserSeeking = false

    // 保存SeekBar监听器的引用，供外部获取
    private var seekBarListener: SeekBar.OnSeekBarChangeListener? = null
    
    /**
     * 获取SeekBar监听器（供SeekBarThumbnailHelper包装使用）
     */
    fun getSeekBarListener(): SeekBar.OnSeekBarChangeListener? = seekBarListener
    
    /**
     * 设置进度条监听
     */
    private fun setupSeekBar() {
        seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 只在拖动完成时才seek,避免频繁触发
                if (fromUser && currentDuration > 0) {
                    // progress 已经是秒数，不需要再计算
                    val position = progress.toDouble()
                    // 实时更新时间显示,但不执行seek
                    tvTimeInfo?.text = "${formatTime(position)}/${formatTime(currentDuration)}"
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                handler.removeCallbacks(hideControlsRunnable)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                // 只在拖动结束时执行一次seek
                seekBar?.let {
                    val progress = it.progress
                    if (currentDuration > 0) {
                        // progress 已经是秒数，不需要再计算
                        val position = progress.toDouble()
                        callback.onSeekBarChange(position)
                    }
                }
                resetAutoHideTimer()
            }
        }
        seekBar?.setOnSeekBarChangeListener(seekBarListener)
    }

    /**
     * 更新播放进度显示
     */
    fun updateProgress(position: Double, duration: Double) {
        if (duration > 0) {
            currentDuration = duration  // 更新当前时长
            
            // 设置 SeekBar 的 max 为视频总秒数，实现秒级精度
            val durationSeconds = duration.toInt()
            if (seekBar?.max != durationSeconds) {
                seekBar?.max = durationSeconds
            }
            
            // 只在用户不拖动时更新进度条和时间
            if (!isUserSeeking) {
                val positionSeconds = position.toInt()
                seekBar?.progress = positionSeconds
                
                // 更新合并的时间显示：当前时间/总时长
                tvTimeInfo?.text = "${formatTime(position)}/${formatTime(duration)}"
            }
        }
    }

    /**
     * 更新播放按钮图标
     */
    fun updatePlayPauseButton(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        btnPlayPause?.setImageResource(
            if (isPlaying) com.fam4k007.videoplayer.R.drawable.pause
            else com.fam4k007.videoplayer.R.drawable.play
        )
    }

    /**
     * 更新上下集按钮状态
     */
    fun updateEpisodeButtons(hasPrevious: Boolean, hasNext: Boolean) {
        btnPrevious?.isEnabled = hasPrevious
        btnPrevious?.alpha = if (hasPrevious) 1.0f else 0.5f
        
        btnNext?.isEnabled = hasNext
        btnNext?.alpha = if (hasNext) 1.0f else 0.5f
    }

    /**
     * 设置文件名
     */
    fun setFileName(fileName: String) {
        tvFileName?.text = fileName
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
        
        // 设置可见性
        controlPanel?.visibility = View.VISIBLE
        topInfoPanel?.visibility = View.VISIBLE
        topGradientBackground?.visibility = View.VISIBLE
        bottomGradientBackground?.visibility = View.VISIBLE
        
        // 入场动画：淡入 + 从下往上滑入
        controlPanel?.apply {
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        
        // 顶部信息栏：淡入 + 从上往下滑入
        topInfoPanel?.apply {
            alpha = 0f
            translationY = -100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        
        // 顶部渐变背景：淡入 + 从上往下滑入
        topGradientBackground?.apply {
            alpha = 0f
            translationY = -100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        
        // 底部渐变背景：淡入 + 从下往上滑入
        bottomGradientBackground?.apply {
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        
        isVisible = true
        notifyControlsVisibilityChanged()
        resetAutoHideTimer()
    }

    /**
     * 隐藏控制面板
     */
    fun hideControls() {
        if (!isVisible) return
        
        // 出场动画：淡出
        controlPanel?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setInterpolator(android.view.animation.AccelerateInterpolator())
            ?.withEndAction {
                controlPanel?.visibility = View.GONE
                controlPanel?.alpha = 1f
                // 控制面板隐藏后同时隐藏系统UI
                hideSystemUI()
            }
            ?.start()
        
        topInfoPanel?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setInterpolator(android.view.animation.AccelerateInterpolator())
            ?.withEndAction {
                topInfoPanel?.visibility = View.GONE
                topInfoPanel?.alpha = 1f
            }
            ?.start()
        
        topGradientBackground?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setInterpolator(android.view.animation.AccelerateInterpolator())
            ?.withEndAction {
                topGradientBackground?.visibility = View.GONE
                topGradientBackground?.alpha = 1f
            }
            ?.start()
        
        bottomGradientBackground?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setInterpolator(android.view.animation.AccelerateInterpolator())
            ?.withEndAction {
                bottomGradientBackground?.visibility = View.GONE
                bottomGradientBackground?.alpha = 1f
            }
            ?.start()
        
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

    private fun initBatteryMonitor(context: Context) {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val batteryPct = (level / scale.toFloat() * 100).toInt()
                tvBattery?.text = "$batteryPct%"
            }
        }
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvTime?.text = sdf.format(Date())
    }
    
    /**
     * 切换锁定状态
     */
    fun toggleLock() {
        isLocked = !isLocked
        if (isLocked) {
            // 锁定：隐藏所有控制组件，显示解锁按钮
            // 顶部控制面板淡出
            topInfoPanel?.animate()
                ?.alpha(0f)
                ?.setDuration(200)
                ?.withEndAction {
                    topInfoPanel?.visibility = View.GONE
                    topInfoPanel?.alpha = 1f
                }
                ?.start()
            
            // 顶部渐变背景淡出
            topGradientBackground?.animate()
                ?.alpha(0f)
                ?.setDuration(200)
                ?.withEndAction {
                    topGradientBackground?.visibility = View.GONE
                    topGradientBackground?.alpha = 1f
                }
                ?.start()
            
            controlPanel?.animate()
                ?.alpha(0f)
                ?.setDuration(200)
                ?.withEndAction {
                    controlPanel?.visibility = View.GONE
                    controlPanel?.alpha = 1f
                }
                ?.start()
            
            // 底部渐变背景淡出
            bottomGradientBackground?.animate()
                ?.alpha(0f)
                ?.setDuration(200)
                ?.withEndAction {
                    bottomGradientBackground?.visibility = View.GONE
                    bottomGradientBackground?.alpha = 1f
                }
                ?.start()
            
            // 左侧解锁按钮淡入
            btnUnlock?.let { unlockBtn ->
                unlockBtn.visibility = View.VISIBLE
                unlockBtn.alpha = 0f
                unlockBtn.scaleX = 0.8f
                unlockBtn.scaleY = 0.8f
                unlockBtn.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
            }
            // 右侧解锁按钮淡入
            btnUnlockRight?.let { unlockBtn ->
                unlockBtn.visibility = View.VISIBLE
                unlockBtn.alpha = 0f
                unlockBtn.scaleX = 0.8f
                unlockBtn.scaleY = 0.8f
                unlockBtn.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
            }
            isUnlockButtonVisible = true
            
            // 停止自动隐藏定时器
            handler.removeCallbacks(hideControlsRunnable)
            
            // 启动解锁按钮的自动隐藏定时器（5秒后）
            handler.postDelayed(hideUnlockButtonRunnable, 5000)
            
            Log.d(TAG, "Controls locked")
        } else {
            // 解锁：显示所有控制组件，隐藏解锁按钮
            // 顶部控制面板淡入
            topInfoPanel?.let { panel ->
                panel.visibility = View.VISIBLE
                panel.alpha = 0f
                panel.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            
            // 顶部渐变背景淡入
            topGradientBackground?.let { bg ->
                bg.visibility = View.VISIBLE
                bg.alpha = 0f
                bg.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            
            controlPanel?.let { panel ->
                panel.visibility = View.VISIBLE
                panel.alpha = 0f
                panel.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            
            // 底部渐变背景淡入
            bottomGradientBackground?.let { bg ->
                bg.visibility = View.VISIBLE
                bg.alpha = 0f
                bg.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            
            // 取消解锁按钮的自动隐藏定时器
            handler.removeCallbacks(hideUnlockButtonRunnable)
            
            // 左侧解锁按钮淡出
            btnUnlock?.animate()
                ?.alpha(0f)
                ?.scaleX(0.8f)
                ?.scaleY(0.8f)
                ?.setDuration(200)
                ?.withEndAction {
                    btnUnlock?.visibility = View.GONE
                    btnUnlock?.alpha = 1f
                    btnUnlock?.scaleX = 1f
                    btnUnlock?.scaleY = 1f
                }
                ?.start()
            // 右侧解锁按钮淡出
            btnUnlockRight?.animate()
                ?.alpha(0f)
                ?.scaleX(0.8f)
                ?.scaleY(0.8f)
                ?.setDuration(200)
                ?.withEndAction {
                    btnUnlockRight?.visibility = View.GONE
                    btnUnlockRight?.alpha = 1f
                    btnUnlockRight?.scaleX = 1f
                    btnUnlockRight?.scaleY = 1f
                }
                ?.start()
            isUnlockButtonVisible = true
            
            // 重新启动自动隐藏定时器
            resetAutoHideTimer()
            
            Log.d(TAG, "Controls unlocked")
        }

        notifyControlsVisibilityChanged()
    }
    
    /**
     * 切换解锁按钮的显示/隐藏（锁定状态下）
     */
    fun toggleUnlockButtonVisibility() {
        if (!isLocked) return  // 只在锁定状态下有效
        
        // 先取消之前的自动隐藏定时器
        handler.removeCallbacks(hideUnlockButtonRunnable)
        
        isUnlockButtonVisible = !isUnlockButtonVisible
        
        // 左侧解锁按钮
        btnUnlock?.let { unlockBtn ->
            if (isUnlockButtonVisible) {
                // 显示：淡入动画
                unlockBtn.visibility = View.VISIBLE
                unlockBtn.alpha = 0f
                unlockBtn.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            } else {
                // 隐藏：淡出动画
                unlockBtn.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        unlockBtn.visibility = View.GONE
                    }
                    .start()
            }
        }
        
        // 右侧解锁按钮
        btnUnlockRight?.let { unlockBtn ->
            if (isUnlockButtonVisible) {
                // 显示：淡入动画
                unlockBtn.visibility = View.VISIBLE
                unlockBtn.alpha = 0f
                unlockBtn.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
                
                // 启动5秒后自动隐藏（只需要设置一次）
                handler.postDelayed(hideUnlockButtonRunnable, 5000)
            } else {
                // 隐藏：淡出动画
                unlockBtn.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        unlockBtn.visibility = View.GONE
                    }
                    .start()
            }
        }
        
        Log.d(TAG, "Unlock button visibility: $isUnlockButtonVisible")
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
        handler.removeCallbacks(timeUpdateRunnable)
        handler.removeCallbacks(hideUnlockButtonRunnable)
        
        activityRef.get()?.let { activity ->
            batteryReceiver?.let {
                try {
                    activity.unregisterReceiver(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to unregister battery receiver", e)
                }
            }
        }
        
        batteryReceiver = null
        
        // 清空所有视图引用
        topInfoPanel = null
        controlPanel = null
        tvFileName = null
        titleClickArea = null
        tvBattery = null
        tvTime = null
        tvTimeInfo = null
        btnPlayPause = null
        btnPrevious = null
        btnNext = null
        btnRewind = null
        btnForward = null
        btnBack = null
        btnSubtitle = null
        btnAspectRatio = null
        btnLock = null
        btnUnlock = null
        btnUnlockRight = null
        btnMore = null
        btnSpeed = null
        btnAnime4K = null
        seekBar = null
        resumePlaybackPrompt = null
        tvResumeConfirm = null
    }
}
