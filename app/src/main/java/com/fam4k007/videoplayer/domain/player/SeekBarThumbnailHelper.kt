package com.fam4k007.videoplayer.domain.player

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.manager.VideoThumbnailManager
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * 进度条缩略图预览助手 - 重构优化版
 * 核心优化：
 * 1. 防抖机制：避免快速拖动时过度提取
 * 2. 智能预加载：触发预加载周围帧
 * 3. 降级显示：立即显示附近帧，避免等待
 */
class SeekBarThumbnailHelper(
    context: Context,
    private val seekBar: SeekBar,
    private val containerView: ViewGroup,
    private val thumbnailManager: VideoThumbnailManager,
    private val originalListener: SeekBar.OnSeekBarChangeListener? = null
) {
    
    private val contextRef = WeakReference(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var popupWindow: PopupWindow? = null
    private var thumbnailImageView: ImageView? = null
    private var thumbnailTimeText: TextView? = null
    
    private var isShowing = false
    private var videoDuration: Double = 0.0
    
    // 防抖控制
    private var loadJob: Job? = null
    private val debounceDelay = 10L  // 10ms 防抖延迟（快速响应）
    
    // 预加载控制
    private var preloadJob: Job? = null
    private var lastRequestedPosition: Long = -1
    
    companion object {
        private const val POPUP_WIDTH = 640
        private const val POPUP_HEIGHT = 410
        private const val TAG = "SeekBarThumbnail"
    }
    
    init {
        setupSeekBarListener()
    }
    
    /**
     * 设置进度条监听
     */
    private fun setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                originalListener?.onProgressChanged(seekBar, progress, fromUser)
                
                if (fromUser && thumbnailManager.isThumbnailSupported()) {
                    val positionSec = progress.toLong()
                    showThumbnail(positionSec)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                originalListener?.onStartTrackingTouch(seekBar)
                // 开始拖动时，立即预加载当前位置周围的缩略图
                val currentSec = (seekBar?.progress ?: 0).toLong()
                startPreload(currentSec)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                originalListener?.onStopTrackingTouch(seekBar)
                stopPreload()
                hideThumbnail()
            }
        })
    }
    
    /**
     * 显示缩略图（立即响应 + 降级显示）
     */
    private fun showThumbnail(positionSec: Long) {
        if (!isShowing) {
            createPopupWindow()
            isShowing = true
        }
        
        // 更新时间文字
        thumbnailTimeText?.text = formatTime(positionSec.toDouble())
        
        // 策略1：立即显示精确缓存
        val exactCached = thumbnailManager.getThumbnailAt(positionSec)
        if (exactCached != null) {
            thumbnailImageView?.setImageBitmap(exactCached)
            lastRequestedPosition = positionSec
            return
        }
        
        // 策略2：立即显示附近的缓存帧（降级方案）
        val nearbyBitmap = findNearbyThumbnail(positionSec, maxDistance = 3)
        if (nearbyBitmap != null) {
            thumbnailImageView?.setImageBitmap(nearbyBitmap)
        }
        
        // 策略3：后台提取精确帧（如果位置变化）
        if (positionSec != lastRequestedPosition) {
            lastRequestedPosition = positionSec
            
            loadJob?.cancel()
            loadJob = scope.launch {
                delay(debounceDelay)
                
                val bitmap = thumbnailManager.extractThumbnailRealtime(positionSec)
                bitmap?.let {
                    // 只有当用户还在这个位置时才更新
                    if (positionSec == lastRequestedPosition) {
                        thumbnailImageView?.setImageBitmap(it)
                    }
                }
            }
        }
    }
    
    /**
     * 查找附近的缓存缩略图（降级显示）
     */
    private fun findNearbyThumbnail(positionSec: Long, maxDistance: Long): Bitmap? {
        for (offset in 1..maxDistance) {
            thumbnailManager.getThumbnailAt(positionSec + offset)?.let { return it }
            thumbnailManager.getThumbnailAt(positionSec - offset)?.let { return it }
        }
        return null
    }
    
    /**
     * 开始预加载（用户开始拖动时触发）
     */
    private fun startPreload(centerSec: Long) {
        preloadJob?.cancel()
        preloadJob = scope.launch {
            // 立即预加载当前位置周围的缩略图
            thumbnailManager.preloadAroundPosition(centerSec)
        }
    }
    
    /**
     * 停止预加载
     */
    private fun stopPreload() {
        preloadJob?.cancel()
        preloadJob = null
    }
    
    /**
     * 创建弹窗
     */
    private fun createPopupWindow() {
        val context = contextRef.get() ?: return
        
        val contentView = LayoutInflater.from(context).inflate(
            R.layout.popup_seekbar_thumbnail,
            null
        )
        
        thumbnailImageView = contentView.findViewById(R.id.ivThumbnail)
        thumbnailTimeText = contentView.findViewById(R.id.tvThumbnailTime)
        
        val screenWidth = containerView.width
        val screenHeight = containerView.height
        val x = (screenWidth - POPUP_WIDTH) / 2
        val y = (screenHeight - POPUP_HEIGHT) / 2
        
        popupWindow = PopupWindow(
            contentView,
            POPUP_WIDTH,
            POPUP_HEIGHT,
            false
        ).apply {
            elevation = 8f
            setBackgroundDrawable(null)
        }
        
        popupWindow?.showAtLocation(containerView, Gravity.NO_GRAVITY, x, y)
    }
    
    /**
     * 隐藏缩略图
     */
    private fun hideThumbnail() {
        loadJob?.cancel()
        preloadJob?.cancel()
        popupWindow?.dismiss()
        popupWindow = null
        thumbnailImageView = null
        thumbnailTimeText = null
        isShowing = false
        lastRequestedPosition = -1
    }
    
    /**
     * 更新视频时长
     */
    fun updateDuration(duration: Double) {
        videoDuration = duration
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(seconds: Double): String {
        val totalSec = seconds.toInt()
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val secs = totalSec % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        hideThumbnail()
        scope.cancel()
    }
}
