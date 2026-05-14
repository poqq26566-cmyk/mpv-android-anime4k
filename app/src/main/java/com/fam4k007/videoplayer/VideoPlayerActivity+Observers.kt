package com.fam4k007.videoplayer

import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.fam4k007.videoplayer.utils.Logger

private const val TAG = "VideoPlayerActivity"

/**
 * 设置ViewModel状态监听，自动更新UI
 */
internal fun VideoPlayerActivity.setupViewModelObservers() {
    // 监听播放/暂停状态
    lifecycleScope.launch {
        viewModel.paused.collect { paused ->
            val isPlaying = paused != true
            this@setupViewModelObservers.isPlaying = isPlaying

            // 弹幕同步（保持原有逻辑）
            if (danmakuManager.isPrepared()) {
                if (isPlaying) {
                    danmakuManager.resume()
                } else {
                    danmakuManager.pause()
                }
            }
        }
    }

    // 监听播放进度和时长
    lifecycleScope.launch {
        viewModel.position.collect { position ->
            val duration = viewModel.duration.value
            if (duration > 0) {
                this@setupViewModelObservers.currentPosition = position.toDouble()
                this@setupViewModelObservers.duration = duration.toDouble()

                // 初始化缩略图（仅一次）
                if (!viewModel.isThumbnailInitialized.value) {
                    videoUri?.let { uri ->
                        val isWebDav = intent.getBooleanExtra("is_webdav", false)
                        thumbnailManager.initializeVideo(uri, (duration * 1000L), isWebDav)
                        viewModel.setThumbnailInitialized(true)
                    }
                }
            }
        }
    }

    // 监听播放速度变化
    lifecycleScope.launch {
        var previousSpeed = 1.0f
        viewModel.speed.collect { speed ->
            com.fam4k007.videoplayer.utils.Logger.v(TAG, "【ViewModel】Speed changed to: ${speed}x")

            // 速度变化时显示提示（排除初始值；恢复到1.0x时不显示，避免长按松手后误触发）
            if (previousSpeed != 1.0f && speed != previousSpeed && speed != 1.0f) {
                showSpeedChangeHint(speed)
            }
            previousSpeed = speed
        }
    }

    // 监听字幕轨道变化（可选：用于调试）
    lifecycleScope.launch {
        viewModel.subtitleTracks.collect { tracks ->
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "【ViewModel】Subtitle tracks updated: ${tracks.size} tracks")
        }
    }

    // 监听音频轨道变化（可选：用于调试）
    lifecycleScope.launch {
        viewModel.audioTracks.collect { tracks ->
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "【ViewModel】Audio tracks updated: ${tracks.size} tracks")
        }
    }

    // ==================== 阶段1.2：UI状态监听示例 ====================

    // 监听控制栏显示/隐藏状态 — 同步到 controlsManager，使顶部面板与 Compose 底部面板一起显示/隐藏
    lifecycleScope.launch {
        viewModel.controlsShown.collect { shown ->
            if (isControlsManagerInitialized) {
                if (shown) controlsManager.showControls() else controlsManager.hideControls()
            }
            com.fam4k007.videoplayer.utils.Logger.v(TAG, "【ViewModel】Controls shown: $shown")
        }
    }

    // 监听控制栏锁定状态 — 同步到 controlsManager 使 XML GestureHandler 能正确判断
    lifecycleScope.launch {
        viewModel.areControlsLocked.collect { locked ->
            controlsManager.setLocked(locked)
            com.fam4k007.videoplayer.utils.Logger.v(TAG, "【ViewModel】Controls locked: $locked")
        }
    }

    // 监听视频比例变化（示例）
    lifecycleScope.launch {
        viewModel.videoAspect.collect { aspect ->
            // 【示例】视频比例变化时更新Activity状态
            currentVideoAspect = aspect
            com.fam4k007.videoplayer.utils.Logger.v(TAG, "【ViewModel】Video aspect: ${aspect.displayName}")
        }
    }

    // 监听弹幕显示状态（示例）
    lifecycleScope.launch {
        viewModel.danmakuVisible.collect { visible ->
            // 【示例】弹幕显示状态同步到Manager
            com.fam4k007.videoplayer.utils.Logger.v(TAG, "【ViewModel】Danmaku visible: $visible")
        }
    }

    // 监听硬件解码状态（示例）
    lifecycleScope.launch {
        viewModel.isHardwareDecoding.collect { hwdec ->
            isHardwareDecoding = hwdec
            com.fam4k007.videoplayer.utils.Logger.v(TAG, "【ViewModel】Hardware decoding: $hwdec")
        }
    }

    // ==================== 阶段1.2：ViewModel状态同步到Activity变量 ====================

    // 同步播放/暂停状态（ViewModel是唯一数据源）
    lifecycleScope.launch {
        viewModel.paused.collect { paused ->
            isPlaying = (paused == false)
        }
    }

    // 同步视频比例（ViewModel是唯一数据源）
    lifecycleScope.launch {
        viewModel.videoAspect.collect { aspect ->
            currentVideoAspect = aspect
        }
    }

    // 同步视频列表（ViewModel是唯一数据源）
    lifecycleScope.launch {
        viewModel.videoList.collect { list ->
            currentVideoList = list
            Logger.v(TAG, "【ViewModel】Video list synced: ${list.size} videos")
        }
    }

    // 同步当前视频索引
    lifecycleScope.launch {
        viewModel.currentIndex.collect { index ->
            currentVideoIndex = index
            Logger.v(TAG, "【ViewModel】Current video index: $index")
        }
    }

    // 同步当前视频URI
    lifecycleScope.launch {
        viewModel.currentVideoUri.collect { uri ->
            // videoUri在某些旧代码中仍需要使用，保持同步
            if (uri != null && uri != videoUri) {
                Logger.v(TAG, "【ViewModel】Current video URI synced")
            }
        }
    }

    // 同步在线视频标识
    lifecycleScope.launch {
        viewModel.isOnlineVideo.collect { online ->
            isOnlineVideo = online
            Logger.v(TAG, "【ViewModel】Is online video: $online")
        }
    }

    // 同步保存的播放位置
    lifecycleScope.launch {
        viewModel.savedPosition.collect { position ->
            savedPosition = position
            Logger.v(TAG, "【ViewModel】Saved position: ${position}s")
        }
    }

    // 同步最后播放位置
    lifecycleScope.launch {
        viewModel.lastPlaybackPosition.collect { position ->
            lastPlaybackPosition = position
            Logger.v(TAG, "【ViewModel】Last playback position: ${position}ms")
        }
    }

    // 同步缓冲检测相关
    lifecycleScope.launch {
        viewModel.lastPositionForBuffering.collect { position ->
            lastPositionForBuffering = position
        }
    }

    lifecycleScope.launch {
        viewModel.lastPositionUpdateTime.collect { time ->
            lastPositionUpdateTime = time
        }
    }

    lifecycleScope.launch {
        viewModel.isStalledBuffering.collect { isStalled ->
            isStalledBuffering = isStalled
        }
    }

    // 同步系列播放列表
    lifecycleScope.launch {
        viewModel.currentSeries.collect { series ->
            currentSeries = series
            Logger.v(TAG, "【ViewModel】Current series synced: ${series.size} videos")
        }
    }

    // 同步Anime4K模式
    lifecycleScope.launch {
        viewModel.anime4KMode.collect { mode ->
            anime4KMode = mode
            Logger.v(TAG, "【ViewModel】Anime4K mode: $mode")
        }
    }

    // ==================== 视频切换事件监听 ====================

    // 监听上下集切换事件（从Compose UI触发）
    lifecycleScope.launch {
        viewModel.switchVideoEvent.collect { uri ->
            Logger.d(TAG, "【ViewModel】Switching to video: $uri")
            playVideo(uri)
        }
    }

    // ==================== 手势相关监听 ====================

    // 监听亮度变化（手势调节）
    lifecycleScope.launch {
        viewModel.currentBrightness.collect { brightness ->
            // 设置窗口亮度
            window.attributes = window.attributes.apply {
                screenBrightness = brightness
            }
            Logger.v(TAG, "【ViewModel】Brightness: $brightness")
        }
    }
}
