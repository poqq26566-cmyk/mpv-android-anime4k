package com.fam4k007.videoplayer

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Button
import android.widget.ImageView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.fam4k007.videoplayer.domain.player.Anime4KManager
import com.fam4k007.videoplayer.domain.player.FilePickerManager
import com.fam4k007.videoplayer.domain.player.GestureHandler
import com.fam4k007.videoplayer.domain.player.PlaybackEngine
import com.fam4k007.videoplayer.domain.player.PlayerControlsManager
import com.fam4k007.videoplayer.domain.player.PlayerDialogManager
import com.fam4k007.videoplayer.ui.player.PlayerControls
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import com.fam4k007.videoplayer.ui.theme.rememberThemeController
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.FormatUtils
import com.fam4k007.videoplayer.utils.Logger
import java.lang.ref.WeakReference

private const val TAG = "VideoPlayerActivity"
private const val SEEK_DEBUG = "SEEK_DEBUG"

/**
 * 阶段2.1：设置Compose测试层
 */
internal fun VideoPlayerActivity.setupComposeTestLayer() {
    try {
        // 创建ComposeView并添加到根布局
        val composeView = ComposeView(this).apply {
            // 设置组合策略：当Activity销毁时自动释放
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            // 设置Compose内容
            setContent {
                val revision = themeRevision
                val context = androidx.compose.ui.platform.LocalContext.current
                val themeController = rememberThemeController(context)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    // 监听文件加载完成事件，重置切换标志，确保自动连播可以继续
                    LaunchedEffect(Unit) {
                        viewModel.fileLoadCompleteEvent.collect {
                            isSwitchingVideo = false
                            Logger.d("VideoPlayerActivity", "File loaded, isSwitchingVideo reset to false")
                        }
                    }
                    // 监听音量变更事件 — 参考 mpvEx：直接调节系统音量，增强时系统满音量+MPV接管
                    LaunchedEffect(Unit) {
                        viewModel.volumeChangeEvent.collect { change ->
                            try {
                                val audioManager = this@setupComposeTestLayer.getSystemService(
                                    android.content.Context.AUDIO_SERVICE
                                ) as android.media.AudioManager
                                val maxSysVol = audioManager.getStreamMaxVolume(
                                    android.media.AudioManager.STREAM_MUSIC
                                )
                                if (change.volumeBoostEnabled && change.volume > 100) {
                                    // 增强模式且超过 100%：系统音量设最大，MPV 接管
                                    audioManager.setStreamVolume(
                                        android.media.AudioManager.STREAM_MUSIC, maxSysVol, 0
                                    )
                                    `is`.xyz.mpv.MPVLib.setPropertyInt("volume", change.volume)
                                } else {
                                    // 普通模式（含增强模式 0-100%）：直接调系统音量
                                    val sysVol = (change.volume.toFloat() / 100f * maxSysVol).toInt()
                                        .coerceIn(0, maxSysVol)
                                    audioManager.setStreamVolume(
                                        android.media.AudioManager.STREAM_MUSIC, sysVol, 0
                                    )
                                    `is`.xyz.mpv.MPVLib.setPropertyInt("volume", 100)
                                }
                            } catch (e: Exception) {
                                Logger.e("VideoPlayerActivity", "Failed to apply volume", e)
                            }
                        }
                    }
                    // 监听亮度变更事件 — 仅调节当前窗口亮度，不修改系统亮度设置
                    LaunchedEffect(Unit) {
                        viewModel.brightnessChangeEvent.collect { brightness ->
                            try {
                                this@setupComposeTestLayer.window.attributes = 
                                    this@setupComposeTestLayer.window.attributes.apply {
                                        screenBrightness = brightness
                                    }
                            } catch (e: Exception) {
                                Logger.e("VideoPlayerActivity", "Failed to set window brightness", e)
                            }
                        }
                    }
                    PlayerControls(
                        viewModel = viewModel,
                        onBackPress = {
                            handleBackNavigation()
                        },
                        onAnime4KClick = { x, y, w, h ->
                            dialogManager.setLastAnchor(x, y, w, h)
                            dialogManager.showAnime4KModeDialog(anime4KMode)
                        },
                        onDanmakuToggle = {
                            val hasLoadedDanmaku = danmakuManager.getCurrentDanmakuPath() != null
                            if (!hasLoadedDanmaku) {
                                com.fam4k007.videoplayer.utils.DialogUtils.showToastShort(
                                    this@setupComposeTestLayer, "请先加载弹幕文件"
                                )
                            } else {
                                val currentVisible = danmakuManager.isVisible()
                                val newSelected = !currentVisible
                                danmakuManager.setTrackSelected(newSelected)
                                if (newSelected && danmakuManager.isPrepared()) {
                                    val currentPos = (playbackEngine.currentPosition * 1000).toLong()
                                    danmakuManager.seekTo(currentPos)
                                    if (isPlaying) danmakuManager.resume()
                                }
                                // 同步到 ViewModel 状态（驱动按钮图标更新）
                                viewModel.setDanmakuVisible(newSelected)
                            }
                        },
                        onSubtitleClick = { anchorX, anchorY, anchorW, anchorH ->
                            dialogManager.setLastAnchor(anchorX, anchorY, anchorW, anchorH)
                            dialogManager.showSubtitleDialog()
                        },
                        onDanmakuClick = { anchorX, anchorY, anchorW, anchorH ->
                            dialogManager.setLastAnchor(anchorX, anchorY, anchorW, anchorH)
                            dialogManager.showDanmakuDialog()
                        },
                        onAspectRatioClick = { anchorX, anchorY, anchorW, anchorH ->
                            dialogManager.setLastAnchor(anchorX, anchorY, anchorW, anchorH)
                            dialogManager.showAspectRatioDialog(currentVideoAspect)
                        },
                        onMoreClick = { anchorX, anchorY, anchorW, anchorH ->
                            dialogManager.setLastAnchor(anchorX, anchorY, anchorW, anchorH)
                            dialogManager.showMoreOptionsDialog()
                        },
                        onVideoTitleClick = {
                            showVideoListDrawer()
                        },
                        onRestartFromBeginning = {
                            playbackEngine?.seekTo(0)
                            videoUri?.let { uri ->
                                Thread {
                                    preferencesManager.clearPlaybackPosition(uri.toString())
                                }.start()
                            }
                        },
                        onRotateClick = {
                            onTogglePortraitUi()
                        },
                        onSpeedClick = { anchorX, anchorY, anchorW, anchorH ->
                            dialogManager.setLastAnchor(anchorX, anchorY, anchorW, anchorH)
                            dialogManager.showSpeedDialog(viewModel.speed.value.toDouble())
                        }
                    )
                }
            }
        }

        // 添加到根布局，覆盖在所有视图之上
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(composeView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        Logger.d(TAG, "Compose test layer added successfully")
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to setup Compose test layer: ${e.message}", e)
    }
}

/**
 * 初始化所有管理器
 */
internal fun VideoPlayerActivity.initializeManagers() {
    playbackEngine = PlaybackEngine(
        mpvView,
        WeakReference(this),
        object : PlaybackEngine.PlaybackEventCallback {
            override fun onPlaybackStateChanged(isPlaying: Boolean) {
                // 【已迁移到ViewModel】播放状态现由setupViewModelObservers()自动处理
                // 此回调保留为空，由ViewModel StateFlow驱动UI更新
            }

            override fun onProgressUpdate(position: Double, duration: Double) {
                // 【已迁移到ViewModel】进度/UI更新现由setupViewModelObservers()自动处理
                // 保留缓冲检测逻辑（非UI状态，仍需即时处理）
                val currentTime = System.currentTimeMillis()
                if (position != lastPositionForBuffering) {
                    // 位置在前进，隐藏停顿缓冲
                    if (isStalledBuffering) {
                        isStalledBuffering = false
                        viewModel.setLoading(false)
                        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Playback resumed, hide stalled buffering indicator")
                    }
                    lastPositionForBuffering = position
                    lastPositionUpdateTime = currentTime
                } else if (isPlaying && currentTime - lastPositionUpdateTime > 200 && !isStalledBuffering) {
                    // 位置停顿超过0.2秒，且正在播放，显示停顿缓冲（仅在线视频）
                    isStalledBuffering = true
                    if (isOnlineVideo) {
                        viewModel.setLoading(true)
                        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Playback stalled, show buffering indicator (online video)")
                    }
                }

                // 初始播放后隐藏加载动画（防止MPV缓冲状态延迟）
                if (position > 1.0 && isPlaying && !isStalledBuffering) {
                    viewModel.setLoading(false)
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Initial playback started, hide loading indicator")
                }

                // 处理片头片尾跳过
                skipIntroOutroManager.handleSkipIntroOutro(
                    folderPath = viewModel.currentFolderPath.value,
                    position = position,
                    duration = duration,
                    getChapters = { playbackEngine.getChapters() },
                    seekTo = { playbackEngine.seekTo(it) },
                    onOutroReached = {
                        // 统一通过 ViewModel 切换，保证 _currentIndex 与播放状态始终同步
                        val hadNext = viewModel.hasNext.value
                        if (hadNext) {
                            viewModel.nextVideo()
                        }
                        hadNext
                    }
                )
            }

            override fun onFileLoaded() {
                isPlaying = true

                // 重置切换标志，允许后续的 END_FILE 事件正常处理
                isSwitchingVideo = false

                // 不在这里隐藏加载动画，让 onBufferingStateChanged 来控制
                // 因为文件加载后可能还在缓冲

                // 缩略图初始化已移动到 onProgressUpdate，确保 duration 已正确设置

                // 重置片头片尾跳过标记
                skipIntroOutroManager.resetFlags()

                // 延迟标记视频准备好，确保视频真正开始播放
                Handler(Looper.getMainLooper()).postDelayed({
                    skipIntroOutroManager.markVideoReady()
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video marked as ready for skip detection")
                }, 500)  // 延迟500ms

                // 不在这里启动弹幕，弹幕的启动由 onPlaybackStateChanged 统一管理
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video file loaded")
            }

            override fun onEndOfFile() {
                videoUri?.let { uri ->
                    preferencesManager.clearPlaybackPosition(uri.toString())
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video ended, position reset to 0")
                }

                // 百分百复用 mpvEx handleEndOfFile 算法
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video ended, handleEndOfFile")
                handleEndOfFile()
            }

            override fun onError(message: String) {
                DialogUtils.showToastLong(this@initializeManagers, message)
            }

            override fun onBufferingStateChanged(isBuffering: Boolean) {
                // 根据缓冲状态显示或隐藏加载动画（仅在线视频）
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Buffering state changed: $isBuffering, isOnlineVideo: $isOnlineVideo")

                viewModel.setLoading(isBuffering && isOnlineVideo)
            }

            override fun onSurfaceReady() {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Surface ready callback received")
            }
        }
    )

    if (!playbackEngine.initialize()) {
        DialogUtils.showToastLong(this, "播放器初始化失败")
        finish()
        return
    }

    // 应用用户保存的解码器设置
    val savedHardwareDecoder = preferencesManager.getHardwareDecoder()
    playbackEngine.setHardwareDecoding(savedHardwareDecoder)
    android.util.Log.d(TAG, "Applied saved hardware decoder setting: $savedHardwareDecoder")

    gestureHandler = GestureHandler(
        WeakReference(this),
        WeakReference(window),
        object : GestureHandler.GestureCallback {
            override fun onGestureStart() {
                // 通用手势开始（亮度/音量调节）
            }

            override fun onGestureEnd() {
                seekHint?.let {
                    if (it.visibility == View.VISIBLE) {
                        it.animate().alpha(0f).setDuration(300)
                            .withEndAction { it.visibility = View.GONE }.start()
                    }
                }
                speedHint?.let {
                    if (it.visibility == View.VISIBLE) {
                        it.animate().alpha(0f).setDuration(300)
                            .withEndAction { it.visibility = View.GONE }.start()
                    }
                }
            }

            override fun onLongPressRelease() {
                // 恢复到长按前的速度
                viewModel.restoreSpeedAfterLongPress()
                playbackEngine?.setSpeed(viewModel.speedBeforeLongPress.value)
                danmakuManager.setSpeed(viewModel.speedBeforeLongPress.value.toFloat())

                // 隐藏速度提示
                speedHint?.animate()?.alpha(0f)?.setDuration(200)
                    ?.withEndAction { speedHint?.visibility = View.GONE }?.start()
            }

            override fun onSingleTap() {
                // 锁定状态下单击屏幕触发解锁按钮重新显示
                if (controlsManager?.isLocked != true) {
                    controlsManager?.toggleControls()
                } else {
                    viewModel.triggerUnlockButtons()
                }
            }

            override fun onDoubleTap() {
                playbackEngine?.togglePlayPause()
            }

            override fun onLongPress() {
                val longPressSpeed = preferencesManager.getLongPressSpeed()

                // 记录当前速度，用于松开后恢复
                viewModel.saveSpeedBeforeLongPress(viewModel.speed.value.toDouble())

                // 设置为长按速度
                viewModel.setSpeed(longPressSpeed.toDouble())
                playbackEngine?.setSpeed(longPressSpeed.toDouble())
                danmakuManager.setSpeed(longPressSpeed)

                // 显示速度提示
                speedHintText?.text = "正在${String.format("%.1f", longPressSpeed)}倍速播放"
                speedHint?.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    animate().alpha(1f).setDuration(200).start()
                }
            }

            override fun onSeekGesture(seekSeconds: Int, isRelativeSeek: Boolean) {
                // 仅用于双击快进/快退
                if (duration > 0) {
                    val basePosition = pendingSeekPosition ?: currentPosition.toInt()
                    val newPos = (basePosition + seekSeconds).coerceIn(0, duration.toInt())
                    pendingSeekPosition = newPos

                    val usePrecise = gestureHandler.isPreciseSeekingEnabled()
                    playbackEngine?.seekTo(newPos, usePrecise)
                    danmakuManager.seekTo((newPos * 1000).toLong())

                    val currentTime = FormatUtils.formatProgressTime(newPos.toDouble())
                    val sign = if (seekSeconds >= 0) "+" else ""
                    val seekTime = FormatUtils.formatProgressTime(seekSeconds.toDouble())
                    seekHint?.text = "$currentTime\n[$sign$seekTime]"
                    if (seekHint?.visibility != View.VISIBLE) {
                        seekHint?.visibility = View.VISIBLE
                        seekHint?.animate()?.alpha(1f)?.setDuration(200)?.start()
                    } else {
                        seekHint?.alpha = 1f
                    }
                }
            }

            override fun onSeekStart(initialPosition: Int) {
                // 滑动seek开始，记录初始位置
                viewModel.setGestureStartPosition(initialPosition)
                android.util.Log.d(TAG, "Seek started from position: $initialPosition")
            }

            override fun onSeekUpdate(targetPosition: Int, deltaSeconds: Int) {
                // 滑动中，实时seek到目标位置（参考 mpvEx）
                if (duration > 0) {
                    val clampedPosition = targetPosition.coerceIn(0, duration.toInt())

                    // 实时调用 seekTo，让视频跟着手指移动
                    val usePrecise = gestureHandler.isPreciseSeekingEnabled()
                    playbackEngine?.seekTo(clampedPosition, usePrecise)
                    danmakuManager.seekTo((clampedPosition * 1000).toLong())

                    // 更新提示文字
                    val currentTime = FormatUtils.formatProgressTime(clampedPosition.toDouble())
                    val sign = if (deltaSeconds >= 0) "+" else ""
                    val seekTime = FormatUtils.formatProgressTime(kotlin.math.abs(deltaSeconds).toDouble())
                    seekHint?.text = "$currentTime\n[$sign$seekTime]"
                    if (seekHint?.visibility != View.VISIBLE) {
                        seekHint?.visibility = View.VISIBLE
                        seekHint?.alpha = 1f
                    } else {
                        seekHint?.alpha = 1f
                    }
                }
            }

            override fun onSeekEnd() {
                // 滑动seek结束，延迟隐藏提示
                seekHint?.postDelayed({
                    seekHint?.animate()?.alpha(0f)?.setDuration(300)
                        ?.withEndAction { seekHint?.visibility = View.GONE }?.start()
                }, 300)
            }

            override fun getCurrentPosition(): Int {
                return currentPosition.toInt()
            }
        }
    )

    controlsManager = PlayerControlsManager(
        WeakReference(this),
        object : PlayerControlsManager.ControlsCallback {
            override fun onPlayPauseClick() {
                playbackEngine.togglePlayPause()
            }

            override fun onPreviousClick() {
                playPreviousVideo()
            }

            override fun onNextClick() {
                playNextVideo()
            }

            override fun onRewindClick() {
                android.util.Log.d(SEEK_DEBUG, "onRewindClick: seekTimeSeconds = ${viewModel.seekTimeSeconds.value}, currentPosition = $currentPosition, seekBy = -${viewModel.seekTimeSeconds.value}")
                playbackEngine.seekBy(-viewModel.seekTimeSeconds.value)
                val newPos = (currentPosition - viewModel.seekTimeSeconds.value).coerceAtLeast(0.0)
                danmakuManager.seekTo((newPos * 1000).toLong())
            }

            override fun onForwardClick() {
                android.util.Log.d(SEEK_DEBUG, "onForwardClick: seekTimeSeconds = ${viewModel.seekTimeSeconds.value}, currentPosition = $currentPosition, seekBy = ${viewModel.seekTimeSeconds.value}")
                playbackEngine.seekBy(viewModel.seekTimeSeconds.value)
                val newPos = (currentPosition + viewModel.seekTimeSeconds.value).coerceAtMost(duration)
                danmakuManager.seekTo((newPos * 1000).toLong())
            }

            override fun onSubtitleClick() {
                dialogManager.showSubtitleDialog()
            }

            override fun onAspectRatioClick() {
                dialogManager.showAspectRatioDialog(currentVideoAspect)
            }

            override fun onLockClick() {
                // 【阶段1.2改进】使用ViewModel的action方法
                viewModel.toggleLock()
                // Manager仍需调用以更新UI（未来可由Compose自动处理）
                controlsManager.toggleLock()
            }

            override fun onAudioTrackClick() {
                dialogManager.showAudioTrackDialog()
            }

            override fun onDecoderClick() {
                dialogManager.showDecoderDialog()
            }

            override fun onAnime4KClick() {
                dialogManager.showAnime4KModeDialog(anime4KMode)
            }

            override fun onMoreClick() {
                dialogManager.showMoreOptionsDialog()
            }

            override fun onSpeedClick() {
                dialogManager.showSpeedDialog(viewModel.speed.value.toDouble())
            }

            override fun onSeekBarChange(position: Double) {
                val usePrecise = gestureHandler.isPreciseSeekingEnabled()
                playbackEngine.seekTo(position.toInt(), usePrecise)
                danmakuManager.seekTo((position * 1000).toLong())
            }

            override fun onBackClick() {
                handleBackNavigation()
            }

            override fun onControlsVisibilityChanged(visible: Boolean) {
            }

            override fun onVideoTitleClick() {
                showVideoListDrawer()
            }

            override fun onControlsShown() {
                // Compose PauseIndicator 会在控制栏显示时自动隐藏（通过 ViewModel 状态）
            }
        },
        WeakReference(gestureHandler)  // 传入GestureHandler引用
    )

    // 百分百复用 mpvEx 播放列表初始化算法
    // 只在本地视频时处理播放列表
    if (!isOnlineVideo) {
        val videoListParcelable = intent.getParcelableArrayListExtra<VideoFileParcelable>("video_list")

        com.fam4k007.videoplayer.utils.Logger.d(TAG, "=== Playlist Initialization ===")
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "videoListParcelable: ${videoListParcelable?.size ?: "null"}")

        if (videoListParcelable != null && videoListParcelable.isNotEmpty()) {
            // 从列表传入：直接设置播放列表
            val currentIndex = videoListParcelable.indexOfFirst { Uri.parse(it.uri) == videoUri }.takeIf { it >= 0 } ?: 0
            viewModel.setVideoList(videoListParcelable, currentIndex)

            // 同步到 Activity 的 playlist
            playlist = videoListParcelable.map { Uri.parse(it.uri) }
            playlistIndex = currentIndex

            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Playlist from intent: ${playlist.size} videos, currentIndex: $playlistIndex")
        } else {
            // 没有列表传入：从文件夹生成播放列表（百分百复用 mpvEx 算法）
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "No video_list in intent, generating playlist from folder")
            videoUri?.let { uri ->
                val path = parsePathFromUri(uri)
                if (path != null) {
                    generatePlaylistFromFolder(path)
                }
            }
        }

        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Playlist size: ${playlist.size}, currentIndex: $playlistIndex")
    } else {
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Online video - skipping playlist generation")
    }

    anime4KManager = Anime4KManager(this)
    if (anime4KManager.initialize()) {
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Anime4K initialized successfully")
    } else {
        com.fam4k007.videoplayer.utils.Logger.w(TAG, "Anime4K initialization failed")
    }

    // 初始化Compose管理器（必须在dialogManager之前）
    composeOverlayManager = com.fanchen.fam4k007.manager.compose.ComposeOverlayManager(
        context = this,
        lifecycleOwner = this,
        rootView = findViewById(android.R.id.content)
    )

    // 设置Compose弹窗状态回调
    composeOverlayManager.onPopupVisibilityChanged = { visible ->
        if (isControlsManagerInitialized) {
            controlsManager.setPopupVisible(visible)
        }
    }

    dialogManager = PlayerDialogManager(
        WeakReference(this),
        playbackEngine,
        danmakuManager,
        anime4KManager,
        preferencesManager,
        composeOverlayManager,
        WeakReference(controlsManager),
        WeakReference(viewModel)  // 传入ViewModel
    )
    dialogManager.setCallback(object : PlayerDialogManager.DialogCallback {
        override fun onSpeedChanged(speed: Double) {
            viewModel.setSpeed(speed)
            playbackEngine.setSpeed(speed)
            danmakuManager.setSpeed(speed.toFloat())
            preferencesManager.setLastPlaybackSpeed(speed.toFloat())
        }

        override fun onAnime4KChanged(enabled: Boolean, mode: Anime4KManager.Mode, quality: Anime4KManager.Quality) {
            viewModel.setAnime4K(enabled, mode, quality)
            anime4KMode = mode
            applyAnime4K()

            // 如果启用了记忆功能，保存当前模式
            if (preferencesManager.isAnime4KMemoryEnabled()) {
                preferencesManager.setLastAnime4KMode(mode.name)
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Anime4K mode saved to memory: ${mode.name}")
            }
        }
    })

    // 初始化文件选择器管理器
    filePickerManager = FilePickerManager(
        WeakReference(this),
        subtitleManager,
        danmakuManager,
        historyManager,
        WeakReference(playbackEngine),
        preferencesManager
    )
    filePickerManager.initialize()
    // 设置ComposeOverlayManager供文件选择器使用
    filePickerManager.setComposeOverlayManager(composeOverlayManager)

    // 初始化截图管理器
    screenshotManager = com.fam4k007.videoplayer.manager.ScreenshotManager(this)

    // 初始化缩略图管理器
    thumbnailManager = com.fam4k007.videoplayer.manager.VideoThumbnailManager(this)

    // 初始化片头片尾管理器
    skipIntroOutroManager = com.fanchen.fam4k007.manager.SkipIntroOutroManager(
        this,
        preferencesManager,
        composeOverlayManager
    )

    bindViewsToManagers()
}

/**
 * 绑定所有View到对应的管理器
 */
internal fun VideoPlayerActivity.bindViewsToManagers() {
    controlsManager.initialize()

    videoUri?.let { uri ->
        val fileName = getFileNameFromUri(uri)
        viewModel.setVideoTitle(fileName)
    }

    gestureHandler.initialize()

    // 亮度/音量指示器已移至 Compose 层，通过 ViewModel 驱动

    // 设置controlsManager引用到gestureHandler，用于检查锁定状态
    gestureHandler.setControlsManager(controlsManager)

    clickArea.setOnTouchListener { v: View, event: MotionEvent ->
        gestureHandler.onTouchEvent(event)
    }

    // 设置当前视频 URI 给文件选择器管理器
    videoUri?.let { uri ->
        filePickerManager.setCurrentVideoUri(uri)
    }
}
