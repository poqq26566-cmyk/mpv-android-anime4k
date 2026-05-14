package com.fam4k007.videoplayer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.content.pm.ActivityInfo
import android.view.MotionEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.fam4k007.videoplayer.player.CustomMPVView
import com.fam4k007.videoplayer.player.VideoAspect
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.domain.player.Anime4KManager
import com.fam4k007.videoplayer.preferences.PreferencesManager
import com.fam4k007.videoplayer.domain.subtitle.SubtitleManager
import com.fam4k007.videoplayer.remote.RemotePlaybackHeaders
import com.fam4k007.videoplayer.remote.RemotePlaybackLauncher
import com.fam4k007.videoplayer.remote.RemotePlaybackRequest
import com.fam4k007.videoplayer.remote.RemotePlaybackResolver
import com.fam4k007.videoplayer.remote.RemoteUrlParser
import com.fam4k007.videoplayer.domain.player.GestureHandler
import com.fam4k007.videoplayer.domain.player.PlaybackEngine
import com.fam4k007.videoplayer.domain.player.PlayerControlsManager
import com.fam4k007.videoplayer.domain.player.SeriesManager
import com.fam4k007.videoplayer.domain.player.PlayerDialogManager
import com.fam4k007.videoplayer.domain.player.FilePickerManager
import com.fam4k007.videoplayer.domain.player.SubtitleDialogCallback
import com.fam4k007.videoplayer.domain.player.DanmakuDialogCallback
import com.fam4k007.videoplayer.domain.player.MoreOptionsCallback
import com.fam4k007.videoplayer.domain.player.VideoAspectCallback
import com.fam4k007.videoplayer.domain.player.VideoUriProvider
import com.fam4k007.videoplayer.utils.FormatUtils
import com.fam4k007.videoplayer.utils.UriUtils.resolveUri
import com.fam4k007.videoplayer.utils.UriUtils.getFolderName
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.ThemeManager
import com.fam4k007.videoplayer.utils.getThemeAttrColor
import com.fam4k007.videoplayer.utils.Logger
import com.fam4k007.videoplayer.presentation.PlayerViewModel
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.fam4k007.videoplayer.ui.player.PlayerControls
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme

/**
 * 视频播放器 Activity (重构版)
 * 使用管理器模式进行职责分离，防止内存泄漏
 */
class VideoPlayerActivity : AppCompatActivity(),
    SubtitleDialogCallback,
    DanmakuDialogCallback,
    MoreOptionsCallback,
    VideoAspectCallback,
    VideoUriProvider {

    companion object {
        private const val TAG = "VideoPlayerActivity"
        private const val SEEK_DEBUG = "SEEK_DEBUG"  // 快进调试专用日志标签
        private val REMOTE_URI_SCHEMES = setOf("http", "https", "rtsp", "rtmp", "rtmps")
        private const val EXTRA_PORTRAIT_UI = "portrait_ui"
        private const val EXTRA_AUTO_ROTATE = "auto_rotate"
    }

    internal lateinit var playbackEngine: PlaybackEngine
    internal lateinit var controlsManager: PlayerControlsManager
    internal val isControlsManagerInitialized get() = ::controlsManager.isInitialized
    internal lateinit var gestureHandler: GestureHandler
    internal lateinit var seriesManager: SeriesManager
    internal lateinit var anime4KManager: Anime4KManager
    internal lateinit var danmakuManager: com.fam4k007.videoplayer.domain.danmaku.DanmakuManager
    internal lateinit var dialogManager: PlayerDialogManager
    internal lateinit var filePickerManager: FilePickerManager
    internal lateinit var composeOverlayManager: com.fanchen.fam4k007.manager.compose.ComposeOverlayManager
    internal lateinit var screenshotManager: com.fam4k007.videoplayer.manager.ScreenshotManager
    internal lateinit var skipIntroOutroManager: com.fanchen.fam4k007.manager.SkipIntroOutroManager
    internal lateinit var thumbnailManager: com.fam4k007.videoplayer.manager.VideoThumbnailManager

    internal lateinit var mpvView: CustomMPVView
    internal lateinit var danmakuView: com.fam4k007.videoplayer.danmaku.DanmakuPlayerView
    internal lateinit var clickArea: View
    internal lateinit var loadingIndicator: android.widget.ProgressBar
    internal lateinit var pauseIndicator: android.widget.ImageView
    internal val pauseIndicatorHandler = Handler(Looper.getMainLooper())
    internal var pauseIndicatorHideRunnable: Runnable? = null
    
    @Deprecated("Use viewModel.currentVideoUri instead", ReplaceWith("viewModel.currentVideoUri.value"))
    internal var videoUri: Uri? = null
    
    internal var remotePlaybackRequest: RemotePlaybackRequest? = null
    internal var remoteResolveJob: Job? = null
    internal var remoteResolveSequence = 0L
    internal val preferencesManager: PreferencesManager by inject()
    internal val historyManager: PlaybackHistoryManager by inject()
    internal val viewModel: PlayerViewModel by viewModel()
    internal val subtitleManager = SubtitleManager()
    
    @Deprecated("Use viewModel.savedPosition instead", ReplaceWith("viewModel.savedPosition.value"))
    internal var savedPosition = 0.0
    
    @Deprecated("Use viewModel.lastPlaybackPosition instead", ReplaceWith("viewModel.lastPlaybackPosition.value"))
    internal var lastPlaybackPosition = 0L  // 从列表传入的播放位置
    
    @Deprecated("Use viewModel.precisePosition instead", ReplaceWith("viewModel.precisePosition.value"))
    internal var currentPosition = 0.0
    
    @Deprecated("Use viewModel.duration instead", ReplaceWith("viewModel.duration.value"))
    internal var duration = 0.0
    
    // 【阶段1.2迁移】以下变量已迁移到ViewModel，Activity保留用于临时兼容
    @Deprecated("Use viewModel.paused instead", ReplaceWith("viewModel.paused.value == false"))
    internal var isPlaying = false
    
    @Deprecated("Use viewModel.isHardwareDecoding instead", ReplaceWith("viewModel.isHardwareDecoding.value"))
    internal var isHardwareDecoding = true
    
    internal var pendingSeekPosition: Int? = null  // 待处理的seek位置，用于解决连续双击问题
    
    @Deprecated("Use viewModel.videoAspect instead", ReplaceWith("viewModel.videoAspect.value"))
    internal var currentVideoAspect = VideoAspect.FIT  // 当前画面比例模式
    
    // 缓冲检测相关变量
    @Deprecated("Use viewModel.lastPositionForBuffering instead", ReplaceWith("viewModel.lastPositionForBuffering.value"))
    internal var lastPositionForBuffering = 0.0
    
    @Deprecated("Use viewModel.lastPositionUpdateTime instead", ReplaceWith("viewModel.lastPositionUpdateTime.value"))
    internal var lastPositionUpdateTime = 0L
    
    @Deprecated("Use viewModel.isStalledBuffering instead", ReplaceWith("viewModel.isStalledBuffering.value"))
    internal var isStalledBuffering = false
    
    @Deprecated("Use viewModel.currentSeries instead", ReplaceWith("viewModel.currentSeries.value"))
    internal var currentSeries: List<Uri> = emptyList()
    
    @Deprecated("Use viewModel.currentIndex instead", ReplaceWith("viewModel.currentIndex.value"))
    internal var currentVideoIndex = -1
    
    // 当前文件夹的视频列表
    @Deprecated("Use viewModel.videoList instead", ReplaceWith("viewModel.videoList.value"))
    internal var currentVideoList: List<VideoFileParcelable> = emptyList()

    internal var anime4KDialog: android.app.Dialog? = null
    
    @Deprecated("Use viewModel.anime4KMode instead", ReplaceWith("viewModel.anime4KMode.value"))
    internal var anime4KMode = Anime4KManager.Mode.OFF
    
    // 是否为在线视频
    @Deprecated("Use viewModel.isOnlineVideo instead", ReplaceWith("viewModel.isOnlineVideo.value"))
    internal var isOnlineVideo = false
    
    internal var seekHint: TextView? = null
    internal var speedHint: LinearLayout? = null
    internal var speedHintText: TextView? = null
    
    internal lateinit var subtitlePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    internal lateinit var danmakuPickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    internal var wasPlayingBeforeSubtitlePicker = false
    
    internal var wasPlayingBeforeDanmakuPicker = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)

        val portraitUi = intent.getBooleanExtra(EXTRA_PORTRAIT_UI, false)
        if (intent.getBooleanExtra(EXTRA_AUTO_ROTATE, false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        // Always use the same layout to avoid tearing down / re-initializing MPV when toggling portrait UI.
        // Portrait mode is applied by updating RelativeLayout rules at runtime.
        setContentView(R.layout.activity_video_player)
        
        // 确保内容不受系统栏影响，视频画面完全居中
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 处理刘海屏/挖孔屏，让视频延伸到刘海区域，确保横屏时完全居中
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Screen keep-on enabled")
        
        loadUserSettings()
        remotePlaybackRequest = intent.getParcelableExtra(RemotePlaybackLauncher.EXTRA_REMOTE_REQUEST)

        // 处理视频URI - 支持本地文件和在线URL
        try {
            videoUri = when {
                remotePlaybackRequest != null -> {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Remote playback request from intent")
                    Uri.parse(remotePlaybackRequest!!.url)
                }
                intent.action == android.content.Intent.ACTION_VIEW -> {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "ACTION_VIEW intent")
                    intent.data
                }
                intent.action == android.content.Intent.ACTION_SEND -> {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "ACTION_SEND intent")
                    if (intent.type?.startsWith("video/") == true || intent.type?.startsWith("audio/") == true) {
                        intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
                    } else {
                        val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                        val parsedSharedInput = sharedText?.let { RemoteUrlParser.parsePlaybackInput(it) }
                        if (parsedSharedInput != null) {
                            val sourcePageUrl = RemotePlaybackHeaders.deriveSourcePageUrl(
                                headers = parsedSharedInput.headers
                            )
                            remotePlaybackRequest = RemotePlaybackRequest(
                                url = parsedSharedInput.url,
                                title = intent.getStringExtra(android.content.Intent.EXTRA_SUBJECT).orEmpty(),
                                sourcePageUrl = sourcePageUrl,
                                headers = parsedSharedInput.headers,
                                source = RemotePlaybackRequest.Source.DIRECT_INPUT
                            )
                            Logger.d(
                                TAG,
                                "Parsed shared remote text: url=${parsedSharedInput.url}, headers=${RemotePlaybackHeaders.describeForLog(parsedSharedInput.headers)}"
                            )
                            Uri.parse(parsedSharedInput.url)
                        } else {
                            intent.data
                        }
                    }
                }
                // 支持从MainActivity传递的URL
                intent.hasExtra("uri") -> {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Has 'uri' extra")
                    val uriString = intent.getStringExtra("uri")
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "URI string from extra: $uriString")
                    if (uriString != null) {
                        Uri.parse(uriString)
                    } else {
                        intent.data
                    }
                }
                else -> {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Using intent.data")
                    intent.data
                }
            }
            
            // 同步到ViewModel
            viewModel.setCurrentVideoUri(videoUri)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing video URI", e)
            DialogUtils.showToastShort(this, "解析视频地址失败: ${e.message}")
            finish()
            return
        }
        
        if (videoUri == null) {
            Log.e(TAG, "Video URI is null")
            DialogUtils.showToastShort(this, "无效的视频路径")
            finish()
            return
        }

        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video URI: $videoUri")
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "URI scheme: ${videoUri?.scheme}")
        
        // 判断是否为在线视频
        val isOnline = remotePlaybackRequest != null ||
            intent.getBooleanExtra("is_online", false) ||
            intent.getBooleanExtra("is_online_video", false) ||
            isRemotePlaybackUri(videoUri)
        viewModel.setIsOnlineVideo(isOnline)

        if (isOnline && remotePlaybackRequest == null) {
            remotePlaybackRequest = buildLegacyRemotePlaybackRequest(videoUri!!)
        }
        
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Is online video: $isOnlineVideo")
        
        // 检查是否从主页继续播放进入（有 folder_path 参数）
        val fromHomeContinue = intent.hasExtra("folder_path")
        viewModel.setIsFromHomeContinue(fromHomeContinue)
        if (fromHomeContinue) {
            val folderPath = intent.getStringExtra("folder_path")
            viewModel.setCurrentFolderPath(folderPath)
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "From home continue play, folder: $folderPath")
        } else if (isOnlineVideo) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Playing online video")
            // 在线视频不需要获取文件夹路径
            viewModel.setCurrentFolderPath(null)
        } else {
            // 获取当前视频所在文件夹路径
            videoUri?.let { uri ->
                val folderPath = uri.getFolderName()
                viewModel.setCurrentFolderPath(folderPath)
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Folder path: $folderPath")
            }
        }

        val savedPos = preferencesManager.getPlaybackPosition(videoUri.toString())
        viewModel.setSavedPosition(savedPos)
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Saved position: $savedPos seconds")

        mpvView = findViewById(R.id.surfaceView)
        danmakuView = findViewById(R.id.danmakuView)
        clickArea = findViewById(R.id.clickArea)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        
        // 根据是否为在线视频设置加载动画的初始状态
        if (isOnlineVideo) {
            loadingIndicator.visibility = View.VISIBLE
        } else {
            loadingIndicator.visibility = View.GONE
        }
        
        // 初始化暂停指示器（方案A：屏幕中央大图标）
        pauseIndicator = ImageView(this).apply {
            setImageResource(R.drawable.media)  // 使用提供的media.png图标
            visibility = View.GONE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            setColorFilter(android.graphics.Color.WHITE)  // 设置图标为白色
        }
        // 添加到根布局正中央
        val iconSizeDp = 90  // 图标大小90dp
        val iconSizePx = (iconSizeDp * resources.displayMetrics.density).toInt()
        (findViewById(android.R.id.content) as ViewGroup)?.addView(pauseIndicator, FrameLayout.LayoutParams(
            iconSizePx,
            iconSizePx,
            Gravity.CENTER  // 屏幕正中央
        ))
        
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Initializing MPV in Activity...")
        try {
            // 总是调用 initialize，CustomMPVView 内部会处理重复初始化的保护
            mpvView.initialize(filesDir.path, cacheDir.path)
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "MPV View initialized")
            
            // 注册ViewModel的MPV观察者（阶段1.1）
            viewModel.registerMPVObservers()
            
            mpvView.postDelayed({
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Loading video after MPV init")
                loadVideo()
            }, 100) // 延迟 100ms 确保 MPV 完全就绪
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MPV", e)
            DialogUtils.showToastLong(this, "播放器初始化失败: ${e.message}\n\n如果问题持续存在，请重启应用")
            finish()
            return
        }
        
        danmakuManager = com.fam4k007.videoplayer.domain.danmaku.DanmakuManager(this, danmakuView)
        danmakuManager.initialize()
        
        initializeManagers()
        
        // 【优化】设置播放引擎，让弹幕可以自动同步播放位置（参考 DanDanPlay 的 ControlWrapper）
        danmakuManager.setPlaybackEngine(playbackEngine)
        
        handleVideoListIntent()

        // Apply portrait UI last: by this point all views are inflated and bound.
        applyPortraitUiEnabled(portraitUi)
        
        // 【阶段1.1】订阅ViewModel StateFlow，自动更新UI（方案A：零视觉影响）
        setupViewModelObservers()
        
        // 【阶段2.1】添加Compose测试层（不影响现有XML布局）
        setupComposeTestLayer()
        
        // 初始化当前系统亮度到ViewModel（用于手势调节）
        val currentBrightness = window.attributes.screenBrightness
        if (currentBrightness > 0) {
            viewModel.setInitialBrightness(currentBrightness)
        } else {
            // 如果当前使用系统默认亮度，设置为0.5
            viewModel.setInitialBrightness(0.5f)
        }
        
        // 初始化当前音量到ViewModel（用于手势调节）
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val volumePercent = ((currentVolume.toFloat() / maxVolume) * 100).toInt()
        viewModel.setInitialVolume(volumePercent)
        
    }

    override fun onTogglePortraitUi() {
        val autoRotateEnabled = intent.getBooleanExtra(EXTRA_AUTO_ROTATE, false)
        
        val currentPortrait = intent.getBooleanExtra(EXTRA_PORTRAIT_UI, false)
        val nextPortrait = !currentPortrait
        intent.putExtra(EXTRA_PORTRAIT_UI, nextPortrait)
        applyPortraitUiEnabled(nextPortrait)
        
        // 手动旋转时不改变自动旋转状态
        if (!autoRotateEnabled) {
            // 自动旋转关闭时，锁定到对应的方向
            requestedOrientation = if (nextPortrait) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        } else {
            // 自动旋转开启时，临时改变方向，但保持自动旋转开启
            requestedOrientation = if (nextPortrait) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        refreshVideoLayoutAfterOrientationToggle()
    }

    override fun onToggleAutoRotate() {
        val enableAutoRotate = !intent.getBooleanExtra(EXTRA_AUTO_ROTATE, false)
        intent.putExtra(EXTRA_AUTO_ROTATE, enableAutoRotate)

        if (enableAutoRotate) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            syncPortraitUiWithConfiguration(resources.configuration)
        } else {
            val currentPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            intent.putExtra(EXTRA_PORTRAIT_UI, currentPortrait)
            applyPortraitUiEnabled(currentPortrait)
            requestedOrientation = if (currentPortrait) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        refreshVideoLayoutAfterOrientationToggle()
    }

    override fun isAutoRotateEnabled(): Boolean {
        return intent.getBooleanExtra(EXTRA_AUTO_ROTATE, false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (intent.getBooleanExtra(EXTRA_AUTO_ROTATE, false)) {
            syncPortraitUiWithConfiguration(newConfig)
            refreshVideoLayoutAfterOrientationToggle()
        }
    }

    override fun onBackPressed() {
        handleBackNavigation()
    }

    /**
     * VideoAspectCallback 实现
     */
    override fun onVideoAspectChanged(aspect: VideoAspect) {
        viewModel.setVideoAspect(aspect)
        Logger.d(TAG, "Video aspect changed to: ${aspect.displayName}")
    }

    override fun onPause() {
        super.onPause()
        savePlaybackState()
    }
    
    override fun onStop() {
        super.onStop()
        
        // 当Activity完全不可见时（Home键、锁屏等），自动暂停视频
        // 不会影响文件选择器等操作，因为那些只触发onPause不触发onStop
        if (::playbackEngine.isInitialized && isPlaying) {
            playbackEngine.pause()
            
            // 手动同步播放状态(因为playbackEngine.pause()不会触发onPlaybackStateChanged)
            isPlaying = false
            
            // 更新UI状态
            // (播放按钮状态由Compose通过ViewModel自动更新)
            
            // 同步暂停弹幕(关键!修复问题1)
            if (::danmakuManager.isInitialized && danmakuManager.isPrepared()) {
                danmakuManager.pause()
            }
            
            // 暂停指示器会由状态监听器自动显示，不需要手动调用
            
            Logger.d(TAG, "Video and danmaku paused due to app going to background")
        }
        
        savePlaybackState()
    }
    
    override fun onResume() {
        super.onResume()
        // 不自动恢复播放，让用户手动控制
        Logger.d(TAG, "Activity resumed")
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 当窗口重新获得焦点时(例如对话框关闭后),重新隐藏系统UI
        // 这样可以确保在Android 12及以下版本中,系统栏不会一直显示
        if (hasFocus && ::controlsManager.isInitialized) {
            controlsManager.hideSystemUI()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "Activity destroyed")

        remoteResolveJob?.cancel()
        
        savePlaybackState()
        
        // 清除自动旋转设置，避免影响下次播放
        intent.removeExtra(EXTRA_AUTO_ROTATE)
        
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 检查gestureHandler是否已初始化
        if (::gestureHandler.isInitialized) {
            gestureHandler.restoreOriginalSettings()
        }
        
        // 释放弹幕资源
        if (::danmakuManager.isInitialized) {
            danmakuManager.release()
        }
        
        // 清理Dialog（防止内存泄漏）
        anime4KDialog?.dismiss()
        anime4KDialog = null
        
        // 清理对话框管理器
        if (::dialogManager.isInitialized) {
            dialogManager.cleanup()
        }
        
        // 销毁播放引擎（会自动移除MPVLib观察者）
        playbackEngine?.destroy()
        controlsManager?.cleanup()
        gestureHandler?.cleanup()
        filePickerManager?.cleanup()
        
        // 释放缩略图资源
        if (::thumbnailManager.isInitialized) {
            thumbnailManager.release()
        }
    }

    override fun onImportSubtitle() {
        filePickerManager.importSubtitle(isPlaying)
    }
    
    override fun onImportDanmaku() {
        filePickerManager.importDanmaku(isPlaying)
    }
    
    override fun onSearchNetworkDanmaku() {
        // 显示网络弹幕搜索对话框
        composeOverlayManager.showDanDanPlaySearchDialog(
            onEpisodeSelected = { episodeId, animeTitle, episodeTitle ->
                // 下载并加载弹幕
                loadNetworkDanmaku(episodeId, animeTitle, episodeTitle)
            }
        )
    }

    override fun onDanmakuVisibilityChanged(visible: Boolean) {
        // 更新历史记录中的弹幕可见性状态
        videoUri?.let { uri ->
            historyManager.updateDanmu(
                uri = uri,
                danmuPath = danmakuManager.getCurrentDanmakuPath(),
                danmuVisible = visible,
                danmuOffsetTime = 0L
            )
            Logger.d(TAG, "Danmaku visibility updated in history: $visible")
        }
    }
    
    override fun onMatchDanmaku() {
        // 自动匹配弹幕
        lifecycleScope.launch {
            try {
                // 获取当前视频文件路径
                val uri = videoUri
                if (uri == null) {
                    DialogUtils.showToastShort(this@VideoPlayerActivity, "无法获取视频URI")
                    return@launch
                }
                
                // 获取真实文件路径
                val videoPath = when (uri.scheme) {
                    "file" -> uri.path
                    "content" -> {
                        // ContentResolver 方式获取路径
                        try {
                            val cursor = contentResolver.query(uri, arrayOf(android.provider.MediaStore.Video.Media.DATA), null, null, null)
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val columnIndex = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                                    it.getString(columnIndex)
                                } else null
                            }
                        } catch (e: Exception) {
                            Logger.e(TAG, "Failed to get path from ContentResolver", e)
                            null
                        }
                    }
                    else -> uri.path
                }
                
                if (videoPath.isNullOrEmpty()) {
                    DialogUtils.showToastShort(this@VideoPlayerActivity, "无法获取视频文件路径")
                    return@launch
                }
                
                val videoFile = File(videoPath)
                if (!videoFile.exists()) {
                    DialogUtils.showToastShort(this@VideoPlayerActivity, "视频文件不存在: $videoPath")
                    return@launch
                }
                
                // 显示匹配提示
                DialogUtils.showToastShort(this@VideoPlayerActivity, "正在匹配弹幕，请稍候...")
                
                val api = com.fam4k007.videoplayer.dandanplay.DanDanPlayApi()
                
                // 获取完整文件名
                val fileName = videoFile.name
                val fileSize = videoFile.length()
                
                Logger.d(TAG, "File info - name: $fileName, size: $fileSize, path: $videoPath")
                
                // 计算文件哈希
                Logger.d(TAG, "Calculating file hash...")
                val fileHash = api.calculateFileHash(videoPath)
                Logger.d(TAG, "File hash calculated: $fileHash")
                
                // 调用匹配API
                val matchResponse = api.matchDanmaku(
                    fileName = fileName,
                    fileHash = fileHash,
                    fileSize = fileSize
                )
                
                if (!matchResponse.isMatched || matchResponse.matches.isNullOrEmpty()) {
                    DialogUtils.showToastLong(this@VideoPlayerActivity, "未找到匹配的弹幕")
                    return@launch
                }
                
                // 找到匹配，显示选择对话框
                val matches = matchResponse.matches
                if (matches.size == 1) {
                    // 只有一个匹配，直接加载
                    val match = matches[0]
                    loadNetworkDanmaku(match.episodeId, match.animeTitle, match.episodeTitle)
                } else {
                    // 多个匹配，让用户选择
                    withContext(Dispatchers.Main) {
                        showMatchSelectionDialog(matches)
                    }
                }
                
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to match danmaku", e)
                DialogUtils.showToastLong(this@VideoPlayerActivity, "匹配失败: ${e.message}")
            }
        }
    }

    override fun onScreenshot() {
        screenshotManager.takeScreenshot()
    }
    
    override fun onShowSkipSettings() {
        skipIntroOutroManager.showSkipSettingsDrawer(viewModel.currentFolderPath.value)
    }

    override fun getVideoUri(): Uri? = videoUri
}

fun Int.dpToPx(): Int {
    return (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
}
