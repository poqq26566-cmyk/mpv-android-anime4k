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
import com.fam4k007.videoplayer.Anime4KManager
import com.fam4k007.videoplayer.manager.PreferencesManager
import com.fam4k007.videoplayer.manager.SubtitleManager
import com.fam4k007.videoplayer.remote.RemotePlaybackHeaders
import com.fam4k007.videoplayer.remote.RemotePlaybackLauncher
import com.fam4k007.videoplayer.remote.RemotePlaybackRequest
import com.fam4k007.videoplayer.remote.RemotePlaybackResolver
import com.fam4k007.videoplayer.remote.RemoteUrlParser
import com.fam4k007.videoplayer.player.GestureHandler
import com.fam4k007.videoplayer.player.PlaybackEngine
import com.fam4k007.videoplayer.player.PlayerControlsManager
import com.fam4k007.videoplayer.player.SeriesManager
import com.fam4k007.videoplayer.utils.FormatUtils
import com.fam4k007.videoplayer.utils.UriUtils.resolveUri
import com.fam4k007.videoplayer.utils.UriUtils.getFolderName
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.ThemeManager
import com.fam4k007.videoplayer.utils.getThemeAttrColor
import com.fam4k007.videoplayer.utils.Logger
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

/**
 * 视频播放器 Activity (重构版)
 * 使用管理器模式进行职责分离，防止内存泄漏
 */
class VideoPlayerActivity : AppCompatActivity(),
    com.fam4k007.videoplayer.player.SubtitleDialogCallback,
    com.fam4k007.videoplayer.player.DanmakuDialogCallback,
    com.fam4k007.videoplayer.player.MoreOptionsCallback,
    com.fam4k007.videoplayer.player.VideoAspectCallback,
    com.fam4k007.videoplayer.player.VideoUriProvider {

    companion object {
        private const val TAG = "VideoPlayerActivity"
        private const val SEEK_DEBUG = "SEEK_DEBUG"  // 快进调试专用日志标签
        private val REMOTE_URI_SCHEMES = setOf("http", "https", "rtsp", "rtmp", "rtmps")
        private const val EXTRA_PORTRAIT_UI = "portrait_ui"
        private const val EXTRA_AUTO_ROTATE = "auto_rotate"
    }

    private lateinit var playbackEngine: PlaybackEngine
    private lateinit var controlsManager: PlayerControlsManager
    private lateinit var gestureHandler: GestureHandler
    private lateinit var seriesManager: SeriesManager
    private lateinit var anime4KManager: Anime4KManager
    private lateinit var danmakuManager: com.fam4k007.videoplayer.danmaku.DanmakuManager
    private lateinit var dialogManager: com.fam4k007.videoplayer.player.PlayerDialogManager
    private lateinit var filePickerManager: com.fam4k007.videoplayer.player.FilePickerManager
    private lateinit var composeOverlayManager: com.fanchen.fam4k007.manager.compose.ComposeOverlayManager
    private lateinit var screenshotManager: com.fam4k007.videoplayer.manager.ScreenshotManager
    private lateinit var skipIntroOutroManager: com.fanchen.fam4k007.manager.SkipIntroOutroManager
    private lateinit var thumbnailManager: com.fam4k007.videoplayer.manager.VideoThumbnailManager
    private var seekBarThumbnailHelper: com.fam4k007.videoplayer.player.SeekBarThumbnailHelper? = null

    private lateinit var mpvView: CustomMPVView
    private lateinit var danmakuView: com.fam4k007.videoplayer.danmaku.DanmakuPlayerView
    private lateinit var clickArea: View
    private lateinit var loadingIndicator: android.widget.ProgressBar
    private lateinit var pauseIndicator: android.widget.ImageView
    private val pauseIndicatorHandler = Handler(Looper.getMainLooper())
    private var pauseIndicatorHideRunnable: Runnable? = null
    
    private var resumeProgressPrompt: LinearLayout? = null
    private var btnResumePromptConfirm: TextView? = null
    private var btnResumePromptClose: TextView? = null
    private val resumePromptHandler = Handler(Looper.getMainLooper())

    private var videoUri: Uri? = null
    private var remotePlaybackRequest: RemotePlaybackRequest? = null
    private var remoteResolveJob: Job? = null
    private var remoteResolveSequence = 0L
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var historyManager: PlaybackHistoryManager
    private val subtitleManager = SubtitleManager()
    private var savedPosition = 0.0
    private var hasRestoredPosition = false
    private var hasShownPrompt = false
    private var lastPlaybackPosition = 0L  // 从列表传入的播放位置
    
    private var currentPosition = 0.0
    private var duration = 0.0
    private var isPlaying = false
    private var currentSpeed = 1.0
    private var speedBeforeLongPress = 1.0  // 记录长按前的速度，用于松开后恢复
    private var isHardwareDecoding = true
    private var pendingSeekPosition: Int? = null  // 待处理的seek位置，用于解决连续双击问题
    private var gestureStartPosition = 0  // 手势开始时的视频位置（用于滑动跳转）
    
    private var currentVideoAspect = VideoAspect.FIT  // 当前画面比例模式
    
    // 缓冲检测相关变量
    private var lastPositionForBuffering = 0.0
    private var lastPositionUpdateTime = 0L
    private var isStalledBuffering = false
    
    // 播放状态跟踪
    private var previousIsPlaying = false
    private var isThumbnailInitialized = false  // 记录是否已初始化缩略图
    
    private var seekTimeSeconds = 5
    
    private var currentSeries: List<Uri> = emptyList()
    private var currentVideoIndex = -1
    
    // 当前文件夹的视频列表
    private var currentVideoList: List<VideoFileParcelable> = emptyList()

    private var anime4KDialog: android.app.Dialog? = null
    private var anime4KEnabled = false
    private var anime4KMode = Anime4KManager.Mode.OFF
    
    // 标记本次播放是否已经自动加载过字幕（防止重复加载）
    private var hasAutoLoadedSubtitle = false
    private var anime4KQuality = Anime4KManager.Quality.BALANCED
    
    // 当前视频所在文件夹路径
    private var currentFolderPath: String? = null
    
    // 是否为在线视频
    private var isOnlineVideo = false
    
    // 是否从主页继续播放进入（需要返回到视频列表而不是直接finish）
    private var isFromHomeContinue = false
    
    private lateinit var seekHint: TextView
    private lateinit var speedHint: LinearLayout
    private lateinit var speedHintText: TextView
    
    private lateinit var subtitlePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    private lateinit var danmakuPickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    private var wasPlayingBeforeSubtitlePicker = false
    
    private var wasPlayingBeforeDanmakuPicker = false
    
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

        preferencesManager = PreferencesManager.getInstance(this)
        
        historyManager = PlaybackHistoryManager(this)
        
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
        isOnlineVideo = remotePlaybackRequest != null ||
            intent.getBooleanExtra("is_online", false) ||
            intent.getBooleanExtra("is_online_video", false) ||
            isRemotePlaybackUri(videoUri)

        if (isOnlineVideo && remotePlaybackRequest == null) {
            remotePlaybackRequest = buildLegacyRemotePlaybackRequest(videoUri!!)
        }
        
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Is online video: $isOnlineVideo")
        
        // 检查是否从主页继续播放进入（有 folder_path 参数）
        isFromHomeContinue = intent.hasExtra("folder_path")
        if (isFromHomeContinue) {
            currentFolderPath = intent.getStringExtra("folder_path")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "From home continue play, folder: $currentFolderPath")
        } else if (isOnlineVideo) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Playing online video")
            // 在线视频不需要获取文件夹路径
            currentFolderPath = null
        } else {
            // 获取当前视频所在文件夹路径
            videoUri?.let { uri ->
                currentFolderPath = uri.getFolderName()
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Folder path: $currentFolderPath")
            }
        }

        savedPosition = preferencesManager.getPlaybackPosition(videoUri.toString())
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Saved position: $savedPosition seconds")

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
        
        resumeProgressPrompt = findViewById(R.id.resumeProgressPrompt)
        btnResumePromptConfirm = findViewById(R.id.btnResumePromptConfirm)
        btnResumePromptClose = findViewById(R.id.btnResumePromptClose)
        
        btnResumePromptConfirm?.setOnClickListener {
            onResumePromptConfirm()
        }
        
        btnResumePromptClose?.setOnClickListener {
            hideResumeProgressPrompt()
        }
        
        seekHint = findViewById(R.id.seekHint)
        speedHint = findViewById(R.id.speedHint)
        speedHintText = findViewById(R.id.speedHintText)
        
        danmakuManager = com.fam4k007.videoplayer.danmaku.DanmakuManager(this, danmakuView)
        danmakuManager.initialize()
        
        initializeManagers()
        
        // 【优化】设置播放引擎，让弹幕可以自动同步播放位置（参考 DanDanPlay 的 ControlWrapper）
        danmakuManager.setPlaybackEngine(playbackEngine)
        
        handleVideoListIntent()

        // Apply portrait UI last: by this point all views are inflated and bound.
        applyPortraitUiEnabled(portraitUi)
        
    }

    /**
     * 初始化所有管理器
     */
    private fun initializeManagers() {
        playbackEngine = PlaybackEngine(
            mpvView,
            WeakReference(this),
            object : PlaybackEngine.PlaybackEventCallback {
                override fun onPlaybackStateChanged(isPlaying: Boolean) {
                    this@VideoPlayerActivity.isPlaying = isPlaying
                    controlsManager?.updatePlayPauseButton(isPlaying)
                    
                    // 按照DanDanPlay的逻辑：只有弹幕prepared并且track被选中时才控制弹幕播放
                    if (danmakuManager.isPrepared()) {
                        if (isPlaying) {
                            danmakuManager.resume()
                        } else {
                            danmakuManager.pause()
                        }
                    }
                }
                
                override fun onProgressUpdate(position: Double, duration: Double) {
                    this@VideoPlayerActivity.currentPosition = position
                    this@VideoPlayerActivity.duration = duration
                    
                    // 清除pending seek位置(当位置更新后，说明seek已完成)
                    pendingSeekPosition = null
                    
                    controlsManager?.updateProgress(position, duration)
                    
                    // 只在第一次获取有效duration时初始化缩略图
                    if (duration > 0 && !isThumbnailInitialized) {
                        videoUri?.let { uri ->
                            val isWebDav = intent.getBooleanExtra("is_webdav", false)
                            thumbnailManager.initializeVideo(uri, (duration * 1000).toLong(), isWebDav)
                            seekBarThumbnailHelper?.updateDuration(duration)
                            isThumbnailInitialized = true
                        }
                    }
                    
                    // 检测播放状态变化，显示/隐藏暂停指示器
                    if (isPlaying != previousIsPlaying) {
                        if (!isPlaying) {
                            // 暂停，显示暂停指示器
                            showPauseIndicator()
                        } else {
                            // 播放，隐藏暂停指示器
                            hidePauseIndicator()
                        }
                        previousIsPlaying = isPlaying
                    }
                    
                    // 检测播放停顿缓冲
                    val currentTime = System.currentTimeMillis()
                    if (position != lastPositionForBuffering) {
                        // 位置在前进，隐藏停顿缓冲
                        if (isStalledBuffering) {
                            isStalledBuffering = false
                            loadingIndicator.visibility = View.GONE
                            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Playback resumed, hide stalled buffering indicator")
                        }
                        lastPositionForBuffering = position
                        lastPositionUpdateTime = currentTime
                    } else if (isPlaying && currentTime - lastPositionUpdateTime > 200 && !isStalledBuffering) {
                        // 位置停顿超过0.2秒，且正在播放，显示停顿缓冲（仅在线视频）
                        isStalledBuffering = true
                        if (isOnlineVideo) {
                            loadingIndicator.visibility = View.VISIBLE
                            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Playback stalled, show buffering indicator (online video)")
                        }
                    }
                    
                    // 初始播放后隐藏加载动画（防止MPV缓冲状态延迟）
                    if (position > 1.0 && isPlaying && loadingIndicator.visibility == View.VISIBLE && !isStalledBuffering) {
                        loadingIndicator.visibility = View.GONE
                        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Initial playback started, hide loading indicator")
                    }
                    
                    // 处理片头片尾跳过
                    skipIntroOutroManager.handleSkipIntroOutro(
                        folderPath = currentFolderPath,
                        position = position,
                        duration = duration,
                        getChapters = { playbackEngine.getChapters() },
                        seekTo = { playbackEngine.seekTo(it) },
                        onOutroReached = {
                            // 使用seriesManager判断是否有下一集
                            val hadNext = seriesManager.hasNext
                            if (hadNext) {
                                playNextVideo()
                            }
                            hadNext
                        }
                    )
                }
                
                override fun onFileLoaded() {
                    isPlaying = true
                    controlsManager?.updatePlayPauseButton(true)
                    
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
                    
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video playback ended, auto-play disabled")
                }
                
                override fun onError(message: String) {
                    DialogUtils.showToastLong(this@VideoPlayerActivity, message)
                }
                
                override fun onBufferingStateChanged(isBuffering: Boolean) {
                    // 根据缓冲状态显示或隐藏加载动画（仅在线视频）
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Buffering state changed: $isBuffering, isOnlineVideo: $isOnlineVideo")
                    
                    if (isBuffering && isOnlineVideo) {
                        // 显示加载动画（仅在线视频）
                        loadingIndicator.visibility = View.VISIBLE
                        loadingIndicator.alpha = 0f
                        loadingIndicator.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    } else {
                        // 缓冲完成时立即隐藏加载动画，不使用动画延迟
                        loadingIndicator.visibility = View.GONE
                    }
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
        Log.d(TAG, "Applied saved hardware decoder setting: $savedHardwareDecoder")
        
        gestureHandler = GestureHandler(
            WeakReference(this),
            WeakReference(window),
            object : GestureHandler.GestureCallback {
                override fun onGestureStart() {
                    // 通用手势开始（亮度/音量调节）
                }
                
                override fun onGestureEnd() {
                    if (seekHint.visibility == View.VISIBLE) {
                        seekHint.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction { seekHint.visibility = View.GONE }
                            .start()
                    }
                    
                    if (speedHint.visibility == View.VISIBLE) {
                        speedHint.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction { speedHint.visibility = View.GONE }
                            .start()
                    }
                }
                
                override fun onLongPressRelease() {
                    // 恢复到长按前的速度
                    currentSpeed = speedBeforeLongPress
                    playbackEngine?.setSpeed(speedBeforeLongPress)
                    danmakuManager.setSpeed(speedBeforeLongPress.toFloat())
                    
                    // 隐藏速度提示
                    speedHint.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            speedHint.visibility = View.GONE
                        }
                        .start()
                }
                
                override fun onSingleTap() {
                    // 如果处于锁定状态，切换解锁按钮显示
                    if (controlsManager?.isLocked == true) {
                        controlsManager?.toggleUnlockButtonVisibility()
                    } else {
                        controlsManager?.toggleControls()
                    }
                }
                
                override fun onDoubleTap() {
                    playbackEngine?.togglePlayPause()
                }
                
                override fun onLongPress() {
                    val longPressSpeed = preferencesManager.getLongPressSpeed()
                    
                    // 记录当前速度，用于松开后恢复
                    speedBeforeLongPress = currentSpeed
                    
                    // 设置为长按速度
                    currentSpeed = longPressSpeed.toDouble()
                    playbackEngine?.setSpeed(longPressSpeed.toDouble())
                    danmakuManager.setSpeed(longPressSpeed)
                    
                    // 显示速度提示
                    speedHintText.text = "正在${String.format("%.1f", longPressSpeed)}倍速播放"
                    speedHint.visibility = View.VISIBLE
                    speedHint.alpha = 0f
                    speedHint.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
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
                        seekHint.text = "$currentTime\n[$sign$seekTime]"
                        
                        if (seekHint.visibility != View.VISIBLE) {
                            seekHint.visibility = View.VISIBLE
                            seekHint.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .start()
                        } else {
                            seekHint.alpha = 1f
                        }
                    }
                }
                
                override fun onSeekStart(initialPosition: Int) {
                    // 滑动seek开始，记录初始位置
                    gestureStartPosition = initialPosition
                    Log.d(TAG, "Seek started from position: $initialPosition")
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
                        seekHint.text = "$currentTime\n[$sign$seekTime]"
                        
                        if (seekHint.visibility != View.VISIBLE) {
                            seekHint.visibility = View.VISIBLE
                            seekHint.alpha = 1f
                        } else {
                            seekHint.alpha = 1f
                        }
                    }
                }
                
                override fun onSeekEnd() {
                    // 滑动seek结束，延迟隐藏提示
                    seekHint.postDelayed({
                        seekHint.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction { seekHint.visibility = View.GONE }
                            .start()
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
                    Log.d(SEEK_DEBUG, "onRewindClick: seekTimeSeconds = $seekTimeSeconds, currentPosition = $currentPosition, seekBy = -$seekTimeSeconds")
                    playbackEngine.seekBy(-seekTimeSeconds)
                    val newPos = (currentPosition - seekTimeSeconds).coerceAtLeast(0.0)
                    danmakuManager.seekTo((newPos * 1000).toLong())
                }
                
                override fun onForwardClick() {
                    Log.d(SEEK_DEBUG, "onForwardClick: seekTimeSeconds = $seekTimeSeconds, currentPosition = $currentPosition, seekBy = $seekTimeSeconds")
                    playbackEngine.seekBy(seekTimeSeconds)
                    val newPos = (currentPosition + seekTimeSeconds).coerceAtMost(duration)
                    danmakuManager.seekTo((newPos * 1000).toLong())
                }
                
                override fun onSubtitleClick() {
                    dialogManager.showSubtitleDialog()
                }
                
                override fun onAspectRatioClick() {
                    dialogManager.showAspectRatioDialog(currentVideoAspect)
                }
                
                override fun onLockClick() {
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
                    dialogManager.showSpeedDialog(currentSpeed)
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
                    updatePortraitFloatingButtonsVisibility(visible)
                }
                
                override fun onVideoTitleClick() {
                    showVideoListDrawer()
                }
                
                override fun onControlsShown() {
                    // 显示控制栏时立即隐藏暂停指示器
                    hidePauseIndicator()
                }
            },
            WeakReference(gestureHandler)  // 传入GestureHandler引用
        )
        
        seriesManager = SeriesManager()
        
        // 只在本地视频时处理系列
        if (!isOnlineVideo) {
            val videoListParcelable = intent.getParcelableArrayListExtra<VideoFileParcelable>("video_list")
            
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "=== Video List Processing ===")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "videoListParcelable: ${videoListParcelable?.size ?: "null"}")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "isFromHomeContinue: $isFromHomeContinue")
            
            if (videoListParcelable != null && videoListParcelable.isNotEmpty()) {
                // 保存视频列表用于显示
                currentVideoList = videoListParcelable
                
                val uriList = videoListParcelable.map { Uri.parse(it.uri) }
                videoUri?.let { uri ->
                    seriesManager.setVideoList(uriList, uri)
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video list from intent: ${uriList.size} videos, currentIndex: ${seriesManager.currentIndex}")
                }
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "currentVideoList size: ${currentVideoList.size}")
            } else {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "No video_list in intent, calling identifySeries")
                videoUri?.let { uri ->
                    seriesManager.identifySeries(this, uri) { videoUri ->
                        getFileNameFromUri(videoUri)
                    }
                }
            }
            
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Series list size: ${seriesManager.getVideoList().size}, currentIndex: ${seriesManager.currentIndex}")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "hasPrevious: ${seriesManager.hasPrevious}, hasNext: ${seriesManager.hasNext}")
        } else {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Online video - skipping series detection")
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
            if (::controlsManager.isInitialized) {
                controlsManager.setPopupVisible(visible)
            }
        }
        
        dialogManager = com.fam4k007.videoplayer.player.PlayerDialogManager(
            WeakReference(this),
            playbackEngine,
            danmakuManager,
            anime4KManager,
            preferencesManager,
            composeOverlayManager,
            WeakReference(controlsManager)
        )
        dialogManager.setCallback(object : com.fam4k007.videoplayer.player.PlayerDialogManager.DialogCallback {
            override fun onSpeedChanged(speed: Double) {
                currentSpeed = speed
                playbackEngine.setSpeed(speed)
                danmakuManager.setSpeed(speed.toFloat())
            }
            
            override fun onAnime4KChanged(enabled: Boolean, mode: Anime4KManager.Mode, quality: Anime4KManager.Quality) {
                anime4KEnabled = enabled
                anime4KMode = mode
                anime4KQuality = quality
                applyAnime4K()
                
                // 如果启用了记忆功能，保存当前模式
                if (preferencesManager.isAnime4KMemoryEnabled()) {
                    preferencesManager.setLastAnime4KMode(mode.name)
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Anime4K mode saved to memory: ${mode.name}")
                }
            }
        })
        
        // 初始化文件选择器管理器
        filePickerManager = com.fam4k007.videoplayer.player.FilePickerManager(
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
    private fun bindViewsToManagers() {
        controlsManager.bindViews(
            topInfoPanel = findViewById(R.id.topInfoPanel),
            controlPanel = findViewById(R.id.controlPanel),
            topGradientBackground = findViewById(R.id.topGradientBackground),  // 顶部渐变背景层
            bottomGradientBackground = findViewById(R.id.bottomGradientBackground),  // 底部渐变背景层
            tvFileName = findViewById(R.id.tvFileName),
            titleClickArea = findViewById(R.id.titleClickArea),  // 标题点击区域
            tvBattery = findViewById(R.id.tvBattery),
            tvTime = findViewById(R.id.tvTime),
            tvTimeInfo = findViewById(R.id.tvTimeInfo),
            btnPlayPause = findViewById(R.id.btnPlayPause),
            btnPrevious = findViewById(R.id.btnPrevious),
            btnNext = findViewById(R.id.btnNext),
            btnRewind = findViewById(R.id.btnRewind),
            btnForward = findViewById(R.id.btnForward),
            btnBack = findViewById(R.id.btnBack),
            btnSubtitle = findViewById(R.id.btnSubtitle),  // 新增字幕按钮
            btnAspectRatio = findViewById(R.id.btnAspectRatio),  // 新增画面比例按钮
            btnLock = findViewById(R.id.btnLock),  // 新增锁定按钮
            btnUnlock = findViewById(R.id.btnUnlock),  // 新增解锁按钮（左侧）
            btnUnlockRight = findViewById(R.id.btnUnlockRight),  // 新增解锁按钮（右侧）
            btnMore = findViewById(R.id.btnMore),
            btnSpeed = findViewById(R.id.btnSpeed),
            btnAnime4K = findViewById(R.id.btnAnime4K),
            seekBar = findViewById(R.id.seekBar),
            resumePlaybackPrompt = findViewById(R.id.resumePlaybackPrompt),
            tvResumeConfirm = findViewById(R.id.tvResumeConfirm)
        )
        
        val btnDanmaku = findViewById<ImageView>(R.id.btnDanmaku)
        btnDanmaku.setOnClickListener {
            dialogManager.showDanmakuDialog()
        }

        // Portrait-only floating buttons (exist in both layouts; only visible in portrait mode).
        findViewById<Button>(R.id.btnAnime4KFloat)?.setOnClickListener {
            dialogManager.showAnime4KModeDialog(anime4KMode)
            controlsManager.resetAutoHideTimer()
        }
        findViewById<Button>(R.id.btnRotateFloat)?.setOnClickListener {
            onTogglePortraitUi()
            controlsManager.resetAutoHideTimer()
        }
        findViewById<Button>(R.id.btnRotateCorner)?.setOnClickListener {
            onTogglePortraitUi()
            controlsManager.resetAutoHideTimer()
        }
          
        // 弹幕显示/隐藏按钮
        val btnDanmakuToggle = findViewById<ImageView>(R.id.btnDanmakuToggle)
        btnDanmakuToggle.setOnClickListener {
            val hasLoadedDanmaku = danmakuManager.getCurrentDanmakuPath() != null
            if (!hasLoadedDanmaku) {
                DialogUtils.showToastShort(this, "请先加载弹幕文件")
            } else {
                // 切换trackSelected状态(参考DanDanPlay的selectTrack/deselectTrack)
                val currentVisible = danmakuManager.isVisible()
                val newSelected = !currentVisible
                
                // 设置轨道选中状态
                danmakuManager.setTrackSelected(newSelected)
                
                // 如果选中且弹幕已准备好
                if (newSelected && danmakuManager.isPrepared()) {
                    // 先同步弹幕到当前播放位置（修复隐藏期间弹幕不同步的问题）
                    val currentPosition = (playbackEngine.currentPosition * 1000).toLong()
                    danmakuManager.seekTo(currentPosition)
                    Logger.d(TAG, "Danmaku synced to current position: $currentPosition ms")
                    
                    // 如果视频正在播放，启动弹幕
                    if (isPlaying) {
                        danmakuManager.resume()
                    }
                }
                
                // 更新按钮图标
                btnDanmakuToggle.setImageResource(
                    if (newSelected) R.drawable.ic_danmaku_visible else R.drawable.ic_danmaku_hidden
                )
                
                Logger.d(TAG, "Danmaku track selected: $newSelected")
            }
        }
        
        controlsManager.initialize()
        
        // 初始化进度条缩略图助手（在controlsManager.initialize()之后）
        // 获取controlsManager设置的监听器，然后用代理模式包装它
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        val container = findViewById<ViewGroup>(android.R.id.content)
        val originalListener = controlsManager.getSeekBarListener()
        seekBarThumbnailHelper = com.fam4k007.videoplayer.player.SeekBarThumbnailHelper(
            this,
            seekBar,
            container,
            thumbnailManager,
            originalListener  // 传入原监听器
        )
        
        videoUri?.let { uri ->
            val fileName = getFileNameFromUri(uri)
            controlsManager.setFileName(fileName)
        }
        
        gestureHandler.initialize()
        
        gestureHandler.bindIndicatorViews(
            brightnessIndicator = findViewById(R.id.brightnessIndicator),
            volumeIndicator = findViewById(R.id.volumeIndicator),
            brightnessBar = findViewById(R.id.brightnessBar),
            volumeBar = findViewById(R.id.volumeBar),
            brightnessText = findViewById(R.id.brightnessText),
            volumeText = findViewById(R.id.volumeText)
        )
        
        // 绑定双击跳转指示器
        gestureHandler.bindDoubleTapSeekIndicators(
            left = findViewById(R.id.doubleTapSeekLeft),
            right = findViewById(R.id.doubleTapSeekRight)
        )
        
        // 设置controlsManager引用到gestureHandler，用于检查锁定状态
        gestureHandler.setControlsManager(controlsManager)
        
        clickArea.setOnTouchListener { v: View, event: MotionEvent ->
            gestureHandler.onTouchEvent(event)
        }
        
        // 设置当前视频 URI 给文件选择器管理器
        videoUri?.let { uri ->
            filePickerManager.setCurrentVideoUri(uri)
        }
        
        updateEpisodeButtons()
    }

    override fun onTogglePortraitUi() {
        intent.putExtra(EXTRA_AUTO_ROTATE, false)
        val currentPortrait = intent.getBooleanExtra(EXTRA_PORTRAIT_UI, false)
        val nextPortrait = !currentPortrait
        intent.putExtra(EXTRA_PORTRAIT_UI, nextPortrait)
        applyPortraitUiEnabled(nextPortrait)
        requestedOrientation = if (nextPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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

    private fun applyPortraitUiEnabled(enabled: Boolean) {
        if (!intent.getBooleanExtra(EXTRA_AUTO_ROTATE, false)) {
            requestedOrientation = if (enabled) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        findViewById<View>(R.id.resumeProgressPrompt)?.let { v ->
            val lp = v.layoutParams as? android.widget.RelativeLayout.LayoutParams ?: return@let
            if (enabled) {
                lp.addRule(android.widget.RelativeLayout.ABOVE, R.id.controlPanel)
                lp.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM)
                lp.bottomMargin = (10f * resources.displayMetrics.density).toInt()
            } else {
                lp.removeRule(android.widget.RelativeLayout.ABOVE)
                lp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM)
                lp.bottomMargin = (130f * resources.displayMetrics.density).toInt()
            }
            v.layoutParams = lp
        }

        applyPortraitSizing(enabled)
    }

    private fun syncPortraitUiWithConfiguration(configuration: Configuration) {
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        intent.putExtra(EXTRA_PORTRAIT_UI, isPortrait)
        applyPortraitUiEnabled(isPortrait)
    }

    private fun applyPortraitSizing(enabled: Boolean) {
        // Anime4K: use a floating button in portrait to avoid overlapping the bottom control row.
        findViewById<View>(R.id.btnAnime4K)?.visibility = if (enabled) View.GONE else View.VISIBLE

        findViewById<View>(R.id.topStatusContainer)?.visibility = View.VISIBLE

        // In portrait, prioritize the right-side status and five-button cluster. The title only
        // gets a small fixed slot and must ellipsize instead of competing for more width.
        findViewById<View>(R.id.titleClickArea)?.let { v ->
            val lp = v.layoutParams as? LinearLayout.LayoutParams ?: return@let
            if (enabled) {
                lp.width = 84.dpToPx()
                lp.weight = 0f
            } else {
                lp.width = 0
                lp.weight = 1f
            }
            v.layoutParams = lp
        }
        findViewById<View>(R.id.topRightPanel)?.let { v ->
            val lp = v.layoutParams as? LinearLayout.LayoutParams ?: return@let
            lp.width = 0
            lp.weight = if (enabled) 2f else 1f
            lp.marginEnd = (if (enabled) 8 else 60).dpToPx()
            v.layoutParams = lp
        }
        findViewById<TextView>(R.id.tvFileName)?.let { tv ->
            tv.maxLines = if (enabled) 1 else 2
            tv.ellipsize = TextUtils.TruncateAt.END
            tv.setSingleLine(enabled)
            tv.textSize = if (enabled) 12f else 13f
        }

        // 1) Top-right icons: portrait uses a tighter five-button cluster while keeping time visible.
        val topIconSize = if (enabled) 28 else 32
        val topIconPadding = if (enabled) 4 else 6
        val portraitIconMargin = 2.dpToPx()
        val landscapeIconMargin = 4.dpToPx()
        val portraitWideIconMargin = 4.dpToPx()
        val landscapeWideIconMargin = 6.dpToPx()
        listOf(
            R.id.btnSubtitle,
            R.id.btnDanmaku,
            R.id.btnAspectRatio,
            R.id.btnLock,
            R.id.btnMore
        ).forEach { id ->
            findViewById<ImageView>(id)?.let { icon ->
                val lp = icon.layoutParams as? ViewGroup.MarginLayoutParams ?: return@let
                lp.width = topIconSize.dpToPx()
                lp.height = topIconSize.dpToPx()
                lp.marginStart = when {
                    enabled && (id == R.id.btnLock || id == R.id.btnMore) -> portraitWideIconMargin
                    enabled -> portraitIconMargin
                    id == R.id.btnLock || id == R.id.btnMore -> landscapeWideIconMargin
                    else -> landscapeIconMargin
                }
                icon.setPadding(
                    topIconPadding.dpToPx(),
                    topIconPadding.dpToPx(),
                    topIconPadding.dpToPx(),
                    topIconPadding.dpToPx()
                )
                icon.layoutParams = lp
            }
        }

        // 2) Bottom main controls: reduce icon size/margins in portrait so they don't feel cramped.
        val normalIconSize = if (enabled) 40 else 44
        val playIconSize = if (enabled) 44 else 48
        val normalPadding = if (enabled) 6 else 8
        val compactMargin = if (enabled) 8 else 12

        fun adjustBottomIcon(
            id: Int,
            sizeDp: Int,
            paddingDp: Int,
            marginStartDp: Int? = null,
            marginEndDp: Int? = null
        ) {
            val icon = findViewById<ImageView>(id) ?: return
            val lp = icon.layoutParams as? LinearLayout.LayoutParams ?: return
            lp.width = sizeDp.dpToPx()
            lp.height = sizeDp.dpToPx()
            marginStartDp?.let { lp.marginStart = it.dpToPx() }
            marginEndDp?.let { lp.marginEnd = it.dpToPx() }
            icon.setPadding(
                paddingDp.dpToPx(),
                paddingDp.dpToPx(),
                paddingDp.dpToPx(),
                paddingDp.dpToPx()
            )
            icon.layoutParams = lp
        }

        adjustBottomIcon(R.id.btnDanmakuToggle, normalIconSize, 6)
        adjustBottomIcon(R.id.btnPrevious, normalIconSize, normalPadding, marginStartDp = compactMargin, marginEndDp = 0)
        adjustBottomIcon(R.id.btnRewind, normalIconSize, normalPadding, marginStartDp = compactMargin, marginEndDp = 0)
        adjustBottomIcon(R.id.btnPlayPause, playIconSize, 6, marginStartDp = compactMargin, marginEndDp = compactMargin)
        adjustBottomIcon(R.id.btnForward, normalIconSize, normalPadding, marginStartDp = 0, marginEndDp = compactMargin)
        adjustBottomIcon(R.id.btnNext, normalIconSize, normalPadding, marginStartDp = 0, marginEndDp = compactMargin)
        adjustBottomIcon(R.id.btnSpeed, normalIconSize, normalPadding)

        updatePortraitFloatingButtonsVisibility(
            controlsManager.isVisible && !controlsManager.isControlsLocked()
        )
    }

    private fun updatePortraitFloatingButtonsVisibility(controlsVisible: Boolean) {
        val portraitEnabled = intent.getBooleanExtra(EXTRA_PORTRAIT_UI, false)
        val shouldShowPortraitButtons = portraitEnabled && controlsVisible
        val shouldShowLandscapeRotate = !portraitEnabled && controlsVisible

        findViewById<View>(R.id.btnAnime4KFloat)?.visibility =
            if (shouldShowPortraitButtons) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnRotateFloat)?.visibility =
            if (shouldShowPortraitButtons) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnRotateCorner)?.visibility =
            if (shouldShowLandscapeRotate) View.VISIBLE else View.GONE
    }

    private fun refreshVideoLayoutAfterOrientationToggle() {
        mpvView.post {
            playbackEngine.changeVideoAspect(currentVideoAspect)
            listOf(
                R.id.surfaceView,
                R.id.danmakuView,
                R.id.clickArea,
                R.id.loadingIndicator
            ).forEach { id ->
                findViewById<View>(id)?.apply {
                    requestLayout()
                    invalidate()
                }
            }

            if (!isPlaying) {
                val pausedPosition = currentPosition.toInt().coerceAtLeast(0)
                mpvView.postDelayed({
                    playbackEngine.seekTo(pausedPosition, precise = true)
                    playbackEngine.pause()
                    controlsManager.updatePlayPauseButton(false)
                }, 120)
            }
        }
    }
    
    /**
     * 加载视频
     */
    private fun loadVideo() {
        videoUri?.let { uri ->
            // 重置自动加载字幕标志（新视频开始播放）
            hasAutoLoadedSubtitle = false
            
            val position = if (duration > 0 && duration < 30) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Short video detected, starting from 0")
                0.0
            } else {
                savedPosition
            }
            
            if (position > 5.0) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Saved position detected: $position seconds - showing resume prompt")
                showResumeProgressPrompt()
            }
            
            // 对于在线视频,直接使用URI字符串;对于本地文件,使用URI对象
            if (isOnlineVideo) {
                val request = remotePlaybackRequest ?: buildLegacyRemotePlaybackRequest(uri)
                remotePlaybackRequest = request
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Loading online video with request: ${request.url}")
                loadResolvedRemoteVideo(request, position)
            } else {
                // 本地视频:使用URI对象
                playbackEngine?.loadVideo(uri, position)
            }
            
            loadDanmakuForVideo(uri)
            
            // 只在position > 0时同步弹幕(延迟更长,等待在线视频加载)
            if (position > 0) {
                lifecycleScope.launch {
                    // 在线视频需要更长延迟
                    val delayTime = if (isOnlineVideo) 3000L else 800L
                    delay(delayTime)
                    danmakuManager.seekTo((position * 1000).toLong())
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Synced danmaku to position: $position seconds")
                }
            }
            
            // 使用协程延迟恢复字幕设置和Anime4K效果
            lifecycleScope.launch {
                delay(500)
                
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Post-load coroutine started, isOnlineVideo=$isOnlineVideo")
                
                // 先尝试自动加载同名字幕（本地视频，包括content://）
                if (!isOnlineVideo) {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Calling autoLoadSubtitleIfExists")
                    autoLoadSubtitleIfExists(uri)
                } else {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Skipping subtitle auto-load for online video")
                }
                
                // 再恢复用户的字幕偏好设置（会覆盖自动加载的）
                restoreSubtitlePreferences(uri)
                
                // 如果记忆的Anime4K模式已启用，在视频加载后应用shader
                if (anime4KEnabled && anime4KMode != Anime4KManager.Mode.OFF) {
                    delay(200) // 额外延迟确保MPV完全初始化
                    applyAnime4K()
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Applied remembered Anime4K mode: $anime4KMode")
                }
            }
        }
    }
    
    /**
     * 自动加载同文件夹下的同名字幕文件
     * 例如：movie.mp4 -> 自动查找 movie.srt, movie.ass 等
     * 需要MANAGE_EXTERNAL_STORAGE权限才能访问所有目录
     */
    private fun autoLoadSubtitleIfExists(videoUri: android.net.Uri) {
        try {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load subtitle start =====")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video URI: $videoUri")
            
            // 检查PreferencesManager中是否已有字幕路径，如果有则跳过自动加载
            val savedSubtitlePath = preferencesManager.getExternalSubtitle(videoUri.toString())
            if (savedSubtitlePath != null && File(savedSubtitlePath).exists()) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Subtitle already exists in preferences: $savedSubtitlePath, skipping auto-load")
                return
            }
            
            // 获取视频文件真实路径
            val videoPath = getRealPathFromUri(videoUri)
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Real video path: $videoPath")
            
            if (videoPath == null) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Cannot get real path from URI")
                return
            }
            
            val videoFile = File(videoPath)
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video file exists: ${videoFile.exists()}, isFile: ${videoFile.isFile}")
            
            if (!videoFile.exists() || !videoFile.isFile) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video file not accessible")
                return
            }
            
            // 获取视频文件名（不含扩展名）
            val videoNameWithoutExt = videoFile.nameWithoutExtension
            val videoDir = videoFile.parentFile
            
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video name without ext: $videoNameWithoutExt")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video directory: ${videoDir?.absolutePath}")
            
            if (videoDir == null) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video directory is null")
                return
            }
            
            // 按优先级排序：ass > srt > 其他
            val priorityExtensions = listOf("ass", "srt", "ssa", "vtt", "sub", "sbv", "json")
            
            // 模糊匹配：查找所有以视频文件名开头的字幕文件
            val allSubtitles = videoDir.listFiles()?.filter { file ->
                if (!file.isFile) return@filter false
                val fileName = file.name.lowercase()
                val videoName = videoNameWithoutExt.lowercase()
                
                // 文件名必须以视频名开头
                if (!fileName.startsWith(videoName)) return@filter false
                
                // 扩展名必须在支持列表中
                val ext = file.extension.lowercase()
                priorityExtensions.contains(ext)
            } ?: emptyList()
            
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Found ${allSubtitles.size} potential subtitle files")
            
            // 如果没有找到任何字幕，直接返回
            if (allSubtitles.isEmpty()) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "No matching subtitle file found")
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load subtitle end =====")
                return
            }
            
            // 获取系统语言偏好
            val systemLanguage = resources.configuration.locales[0].toString().lowercase()
            val isSimplifiedChinese = systemLanguage.contains("zh_cn") || systemLanguage.contains("zh-cn")
            val isTraditionalChinese = systemLanguage.contains("zh_tw") || systemLanguage.contains("zh_hk") || 
                                       systemLanguage.contains("zh-tw") || systemLanguage.contains("zh-hk")
            
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "System language: $systemLanguage, SC: $isSimplifiedChinese, TC: $isTraditionalChinese")
            
            // 按优先级排序字幕文件
            val sortedSubtitles = allSubtitles.sortedWith(compareBy(
                // 1. 扩展名优先级（数字越小优先级越高）
                { file -> 
                    val ext = file.extension.lowercase()
                    priorityExtensions.indexOf(ext).let { if (it == -1) 999 else it }
                },
                // 2. 完全匹配优先（123.ass > 123.sc.ass）
                { file -> 
                    val nameWithoutExt = file.nameWithoutExtension
                    if (nameWithoutExt.equals(videoNameWithoutExt, ignoreCase = true)) 0 else 1
                },
                // 3. 语言标记优先级（根据系统语言）
                { file ->
                    val nameLower = file.nameWithoutExtension.lowercase()
                    val afterVideoName = nameLower.removePrefix(videoNameWithoutExt.lowercase())
                    
                    when {
                        // 简体中文系统优先简体标记
                        isSimplifiedChinese && (afterVideoName.contains("sc") || afterVideoName.contains("chs") || 
                                                afterVideoName.contains("简") || afterVideoName.contains("zh-cn")) -> 0
                        // 繁体中文系统优先繁体标记
                        isTraditionalChinese && (afterVideoName.contains("tc") || afterVideoName.contains("cht") || 
                                                 afterVideoName.contains("繁") || afterVideoName.contains("zh-tw")) -> 0
                        else -> 1
                    }
                },
                // 4. 文件名长度（越短越优先，假设越接近原始名称）
                { file -> file.nameWithoutExtension.length },
                // 5. 字母顺序
                { file -> file.name.lowercase() }
            ))
            
            // 打印所有找到的字幕（调试用）
            sortedSubtitles.forEachIndexed { index, file ->
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Subtitle [$index]: ${file.name}")
            }
            
            // 选择优先级最高的字幕文件
            val foundSubtitle = sortedSubtitles.first()
            
            val subtitlePath = foundSubtitle.absolutePath
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Auto-loading best match subtitle: $subtitlePath")
            
            try {
                MPVLib.command("sub-add", subtitlePath, "select")
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Successfully auto-loaded subtitle: ${foundSubtitle.name}")
                
                // 保存字幕路径到记忆中，下次进入视频时不会重复自动加载
                preferencesManager.setExternalSubtitle(videoUri.toString(), subtitlePath)
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Saved auto-loaded subtitle path to preferences")
                
                // 设置标志，防止 restoreSubtitlePreferences 重复加载
                hasAutoLoadedSubtitle = true
                
                DialogUtils.showToastShort(this, "已自动加载字幕: ${foundSubtitle.name}")
            } catch (e: Exception) {
                com.fam4k007.videoplayer.utils.Logger.w(TAG, "Failed to auto-load subtitle", e)
                DialogUtils.showToastShort(this, "字幕加载失败: ${e.message}")
            }
            
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load subtitle end =====")
        } catch (e: Exception) {
            Log.e(TAG, "Error in autoLoadSubtitleIfExists", e)
        }
    }
    
    /**
     * 从content:// URI获取真实文件路径
     * 支持媒体库URI和文件URI
     */
    private fun getRealPathFromUri(uri: android.net.Uri): String? {
        return when (uri.scheme) {
            "file" -> uri.path
            "content" -> {
                try {
                    val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                            cursor.getString(columnIndex)
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    com.fam4k007.videoplayer.utils.Logger.w(TAG, "Failed to get real path from content URI", e)
                    null
                }
            }
            else -> null
        }
    }
    
    /**
     * 根据文件路径查找对应的content:// URI（从MediaStore）
     * 用于解决某些目录的权限问题
     */
    private fun getContentUriForFile(file: File): android.net.Uri? {
        return try {
            val projection = arrayOf(android.provider.MediaStore.Files.FileColumns._ID)
            val selection = "${android.provider.MediaStore.Files.FileColumns.DATA}=?"
            val selectionArgs = arrayOf(file.absolutePath)
            
            contentResolver.query(
                android.provider.MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns._ID))
                    android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Files.getContentUri("external"),
                        id
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            com.fam4k007.videoplayer.utils.Logger.w(TAG, "Failed to get content URI for file: ${file.absolutePath}", e)
            null
        }
    }
    
    /**
     * 加载视频对应的弹幕文件
     * 参考 DanDanPlay 的弹幕加载逻辑：优先使用历史记录，其次自动查找
     */
    private fun loadDanmakuForVideo(videoUri: android.net.Uri) {
        try {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Loading danmaku for video: $videoUri")
            
            val history = historyManager.getHistoryForUri(videoUri)
            
            // 如果历史记录中有弹幕路径且文件存在，恢复弹幕
            if (history?.danmuPath != null && File(history.danmuPath).exists()) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Restoring danmaku from history: ${history.danmuPath}")
                // 恢复用户上次的弹幕可见性设置(这会设置trackSelected状态)
                val autoShow = history.danmuVisible
                val loaded = danmakuManager.loadDanmakuFile(
                    history.danmuPath,
                    autoShow = autoShow,
                    isPlaying = isPlaying
                )
                
                if (loaded) {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Danmaku restored: path=${history.danmuPath}, trackSelected=$autoShow")
                    
                    // 同步UI按钮状态
                    val btnDanmakuToggle = findViewById<ImageView>(R.id.btnDanmakuToggle)
                    btnDanmakuToggle?.setImageResource(
                        if (autoShow) R.drawable.ic_danmaku_visible else R.drawable.ic_danmaku_hidden
                    )
                } else {
                    com.fam4k007.videoplayer.utils.Logger.w(TAG, "Failed to restore danmaku")
                }
            } 
            // 只有当历史记录不存在时，才执行自动查找
            // 如果历史记录存在但 danmuPath 为 null，说明之前尝试过但没找到，不要重复尝试
            else if (history == null) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "No history found, trying auto-find...")
                autoFindAndLoadDanmaku(videoUri)
            } else {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "History exists but no danmaku path, skipping auto-find")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading danmaku", e)
            DialogUtils.showToastShort(this, "弹幕加载失败: ${e.message}")
        }
    }
    
    /**
     * 自动查找并加载同名弹幕文件
     */
    private fun autoFindAndLoadDanmaku(videoUri: android.net.Uri) {
        try {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load danmaku start =====")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video URI: $videoUri")
            
            // 【修复】使用 getRealPathFromUri 获取真实文件路径（而不是 fd:// 路径）
            val videoPath = getRealPathFromUri(videoUri)
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Real video path: $videoPath")
            
            if (videoPath != null) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Auto-finding danmaku for: $videoPath")
                
                // 自动加载弹幕，默认不显示（autoShow = false），由用户手动控制
                val loaded = danmakuManager.loadDanmakuForVideo(
                    videoPath, 
                    autoShow = false,  // 加载但不自动显示
                    isPlaying = isPlaying
                )
                
                if (loaded) {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "✓ Danmaku auto-loaded successfully")
                    
                    // 在主线程显示提示和更新UI
                    runOnUiThread {
                        // 获取实际加载的弹幕文件名
                        val danmakuPath = danmakuManager.getCurrentDanmakuPath()
                        val fileName = danmakuPath?.substringAfterLast("/") ?: "弹幕文件"
                        
                        // 显示加载成功提示，提醒用户需要手动显示
                        DialogUtils.showToastShort(this, "已加载弹幕: $fileName\n点击弹幕按钮显示")
                        
                        // 根据实际的 trackSelected 状态更新按钮
                        val btnDanmakuToggle = findViewById<ImageView>(R.id.btnDanmakuToggle)
                        val isTrackSelected = danmakuManager.getTrackSelected()
                        btnDanmakuToggle?.setImageResource(
                            if (isTrackSelected) R.drawable.ic_danmaku_visible else R.drawable.ic_danmaku_hidden
                        )
                        
                        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Button state updated: trackSelected=$isTrackSelected")
                        
                        // 【修复】保存弹幕信息到历史记录，避免下次重复自动加载
                        if (danmakuPath != null) {
                            historyManager.updateDanmu(
                                uri = videoUri,
                                danmuPath = danmakuPath,
                                danmuVisible = isTrackSelected,
                                danmuOffsetTime = 0L
                            )
                            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Danmu info saved to history: path=$danmakuPath")
                        }
                    }
                } else {
                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "✗ No danmaku file found")
                }
            } else {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Cannot get real path from URI, skipping danmaku auto-load")
            }
            
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load danmaku end =====")
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-finding danmaku", e)
            // 自动查找失败不提示，避免打扰用户
        }
    }
    
    /**
     * 恢复视频的字幕偏好设置
     */
    private fun restoreSubtitlePreferences(videoUri: android.net.Uri) {
        try {
            val uriString = videoUri.toString()
            Logger.d(TAG, "Restoring subtitle preferences for: $uriString")
            
            playbackEngine?.let { engine ->
                val assOverride = preferencesManager.isAssOverrideEnabled(uriString)
                if (assOverride) {
                    lifecycleScope.launch {
                        delay(300)
                        engine.setAssOverride(assOverride)
                        Logger.d(TAG, "Restored ASS override: $assOverride")
                    }
                }
                
                val savedScale = preferencesManager.getSubtitleScale(uriString)
                if (savedScale != 1.0) {
                    engine.setSubtitleScale(savedScale)
                    Logger.d(TAG, "Restored subtitle scale: $savedScale")
                }
                
                val savedPosition = preferencesManager.getSubtitlePosition(uriString)
                if (savedPosition != 100) {
                    engine.setSubtitleVerticalPosition(savedPosition)
                    Logger.d(TAG, "Restored subtitle position: $savedPosition")
                }
                
                val savedDelay = preferencesManager.getSubtitleDelay(uriString)
                if (savedDelay != 0.0) {
                    engine.setSubtitleDelay(savedDelay)
                    Logger.d(TAG, "Restored subtitle delay: $savedDelay")
                }
                
                val savedSubtitlePath = preferencesManager.getExternalSubtitle(uriString)
                if (savedSubtitlePath != null) {
                    // 如果已经自动加载过这个字幕，就跳过（防止重复加载）
                    if (hasAutoLoadedSubtitle) {
                        Logger.d(TAG, "Subtitle already auto-loaded this session, skipping restore")
                    } else if (File(savedSubtitlePath).exists()) {
                        try {
                            MPVLib.command("sub-add", savedSubtitlePath, "select")
                            Logger.d(TAG, "Restored external subtitle from path: $savedSubtitlePath")
                        } catch (e: Exception) {
                            Logger.w(TAG, "Failed to restore external subtitle", e)
                        }
                    } else {
                        Logger.w(TAG, "Saved subtitle file not found: $savedSubtitlePath")
                    }
                }
                
                val savedTrackId = preferencesManager.getSubtitleTrackId(uriString)
                if (savedTrackId != -1) {
                    lifecycleScope.launch {
                        delay(600)
                        try {
                            engine.selectSubtitleTrack(savedTrackId)
                            Logger.d(TAG, "Restored subtitle track: $savedTrackId")
                        } catch (e: Exception) {
                            Logger.w(TAG, "Failed to restore subtitle track", e)
                        }
                    }
                }
                
                val savedTextColor = preferencesManager.getSubtitleTextColor(uriString)
                if (savedTextColor != "#FFFFFF") {
                    engine.setSubtitleTextColor(savedTextColor)
                    Logger.d(TAG, "Restored subtitle text color: $savedTextColor")
                }
                
                val savedBorderSize = preferencesManager.getSubtitleBorderSize(uriString)
                if (savedBorderSize != 3) {
                    engine.setSubtitleBorderSize(savedBorderSize)
                    Logger.d(TAG, "Restored subtitle border size: $savedBorderSize")
                }
                
                val savedBorderColor = preferencesManager.getSubtitleBorderColor(uriString)
                if (savedBorderColor != "#000000") {
                    engine.setSubtitleBorderColor(savedBorderColor)
                    Logger.d(TAG, "Restored subtitle border color: $savedBorderColor")
                }
                
                val savedBackColor = preferencesManager.getSubtitleBackColor(uriString)
                if (savedBackColor != "#00000000") {
                    engine.setSubtitleBackColor(savedBackColor)
                    Logger.d(TAG, "Restored subtitle back color: $savedBackColor")
                }
                
                // 总是应用描边样式，即使是默认值，确保MPV状态正确
                val savedBorderStyle = preferencesManager.getSubtitleBorderStyle(uriString)
                engine.setSubtitleBorderStyle(savedBorderStyle)
                Logger.d(TAG, "Restored subtitle border style: $savedBorderStyle")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring subtitle preferences", e)
            // 恢复字幕设置失败不影响播放，不提示用户
        }
    }
    
    /**
     * 显示进度恢复提示框
     */
    private fun showResumeProgressPrompt() {
        resumeProgressPrompt?.visibility = View.VISIBLE
        
        resumePromptHandler.postDelayed({
            hideResumeProgressPrompt()
        }, 5000)
    }
    
    /**
     * 加载用户设置
     */
    private fun loadUserSettings() {
        seekTimeSeconds = preferencesManager.getSeekTime()
        Log.d(SEEK_DEBUG, "loadUserSettings: seekTimeSeconds loaded = $seekTimeSeconds seconds")
        
        // 如果启用了Anime4K记忆功能，恢复上次使用的模式
        if (preferencesManager.isAnime4KMemoryEnabled()) {
            val lastMode = preferencesManager.getLastAnime4KMode()
            try {
                anime4KMode = Anime4KManager.Mode.valueOf(lastMode)
                // 只有非OFF模式才启用Anime4K
                anime4KEnabled = (anime4KMode != Anime4KManager.Mode.OFF)
                Logger.d(TAG, "Anime4K mode restored from memory: $lastMode, enabled=$anime4KEnabled")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid Anime4K mode in preferences: $lastMode", e)
                anime4KMode = Anime4KManager.Mode.OFF
                anime4KEnabled = false
            }
        } else {
            anime4KMode = Anime4KManager.Mode.OFF
            anime4KEnabled = false
        }
    }
    
    /**
     * 隐藏进度恢复提示框
     */
    private fun hideResumeProgressPrompt() {
        resumeProgressPrompt?.visibility = View.GONE
        resumePromptHandler.removeCallbacksAndMessages(null)
    }
    
    /**
     * 用户点击确定按钮，从头开始播放
     */
    private fun onResumePromptConfirm() {
        Logger.d(TAG, "User confirmed to restart from beginning")
        hideResumeProgressPrompt()
        
        playbackEngine?.seekTo(0)
        
        videoUri?.let { uri ->
            Thread {
                preferencesManager.clearPlaybackPosition(uri.toString())
            }.start()
        }
    }
    
    /**
     * 处理从列表传入的视频数据
     */
    private fun handleVideoListIntent() {
        val lastPosition = intent.getLongExtra("lastPosition", -1L)
        if (lastPosition > 0) {
            this.lastPlaybackPosition = lastPosition
        }
    }
    
    /**
     * 更新上一集下一集按钮状态
     */
    private fun updateEpisodeButtons() {
        Logger.d(TAG, "updateEpisodeButtons - hasPrevious: ${seriesManager.hasPrevious}, hasNext: ${seriesManager.hasNext}")
        controlsManager?.updateEpisodeButtons(seriesManager.hasPrevious, seriesManager.hasNext)
    }
    
    /**
     * 播放上一集
     */
    private fun playPreviousVideo() {
        Logger.d(TAG, "playPreviousVideo - hasPrevious: ${seriesManager.hasPrevious}")
        if (seriesManager.hasPrevious) {
            val previousUri = seriesManager.previous()
            if (previousUri != null) {
                Logger.d(TAG, "Playing previous video: $previousUri")
                playVideo(previousUri)
                updateEpisodeButtons()
            }
        } else {
            DialogUtils.showToastShort(this, "已经是第一集了")
        }
    }
    
    /**
     * 播放下一集
     */
    private fun playNextVideo() {
        Logger.d(TAG, "playNextVideo - hasNext: ${seriesManager.hasNext}")
        if (seriesManager.hasNext) {
            val nextUri = seriesManager.next()
            if (nextUri != null) {
                Logger.d(TAG, "Playing next video: $nextUri")
                playVideo(nextUri)
                updateEpisodeButtons()
            }
        } else {
            DialogUtils.showToastShort(this, "已经是最后一集了")
        }
    }
    
    /**
     * 播放指定视频
     */
    private fun playVideo(uri: Uri) {
        videoUri = uri
        
        // 重置自动加载字幕标志（新视频开始播放）
        hasAutoLoadedSubtitle = false
        
        // 更新在线视频标志
        isOnlineVideo = isRemotePlaybackUri(uri)
        remotePlaybackRequest = if (isOnlineVideo) {
            RemotePlaybackRequest(
                url = uri.toString(),
                title = resolveVideoTitle(uri),
                source = RemotePlaybackRequest.Source.UNKNOWN
            )
        } else {
            null
        }
        
        // 显示加载动画（仅在线视频）
        if (isOnlineVideo) {
            loadingIndicator.visibility = View.VISIBLE
            loadingIndicator.alpha = 1f
        } else {
            loadingIndicator.visibility = View.GONE
        }
        
        val fileName = getFileNameFromUri(uri)
        controlsManager?.setFileName(fileName)
        
        val position = preferencesManager.getPlaybackPosition(uri.toString())
        
        // 【重要】清除旧弹幕，避免切换视频时弹幕残留
        Logger.d(TAG, "Releasing old danmaku before playing new video")
        danmakuManager.release()
        
        if (isOnlineVideo) {
            loadResolvedRemoteVideo(remotePlaybackRequest!!, position)
        } else {
            playbackEngine?.loadVideo(uri, position)
        }
        
        // 设置当前视频 URI 给文件选择器管理器
        filePickerManager.setCurrentVideoUri(uri)
        
        // 自动加载字幕和弹幕（和初始播放一样的逻辑）
        lifecycleScope.launch {
            delay(500)
            
            // 本地视频自动加载同名字幕
            if (!isOnlineVideo) {
                Logger.d(TAG, "Auto-loading subtitle for next video")
                autoLoadSubtitleIfExists(uri)
            }
            
            // 恢复字幕偏好设置
            restoreSubtitlePreferences(uri)
            
            // 【重要】加载新视频的弹幕
            Logger.d(TAG, "Loading danmaku for new video: $uri")
            loadDanmakuForVideo(uri)
            
            // 同步弹幕到播放位置
            if (position > 0) {
                delay(300)
                danmakuManager.seekTo((position * 1000).toLong())
                Logger.d(TAG, "Synced danmaku to position: $position seconds")
            }
        }
        
        updateEpisodeButtons()
    }

    private fun loadResolvedRemoteVideo(
        request: RemotePlaybackRequest,
        position: Double
    ) {
        remoteResolveJob?.cancel()
        val sequence = ++remoteResolveSequence
        remoteResolveJob = lifecycleScope.launch {
            val result = RemotePlaybackResolver.resolve(request)
            val debugSummary = RemotePlaybackResolver.buildDebugSummary(result)
            preferencesManager.setLastRemoteDebugSummary(debugSummary)
            Logger.d(TAG, "Remote debug summary:\n$debugSummary")
            if (sequence != remoteResolveSequence) {
                Logger.d(TAG, "Discarding stale remote resolve result for: ${request.url}")
                return@launch
            }

            when (result) {
                is RemotePlaybackResolver.ResolveResult.Success -> {
                    remotePlaybackRequest = result.request
                    videoUri = Uri.parse(result.request.url)
                    Logger.d(
                        TAG,
                        "Remote request resolved via ${result.probeMethod}: ${request.url} -> ${result.request.url}"
                    )
                    playbackEngine.loadRemote(result.request, position)
                }
                is RemotePlaybackResolver.ResolveResult.Failed -> {
                    remotePlaybackRequest = result.request
                    Logger.w(TAG, "Remote resolve fallback: reason=${result.reason}, message=${result.message}", result.cause)
                    val suggestion = RemotePlaybackResolver.buildFailureSuggestion(result.reason)
                    DialogUtils.showToastLong(
                        this@VideoPlayerActivity,
                        "${result.message}，继续尝试直接播放\n$suggestion"
                    )
                    playbackEngine.loadRemote(result.request, position)
                }
            }
        }
    }
    
    override fun onBackPressed() {
        handleBackNavigation()
    }
    
    /**
     * 处理返回导航
     * 如果是从主页继续播放进入，则跳转到对应的视频列表
     * 否则正常返回
     */
    private fun handleBackNavigation() {
        gestureHandler?.restoreOriginalSettings()
        
        if (isFromHomeContinue) {
            // 从主页继续播放进入，直接返回到主页（MainActivity）
            Logger.d(TAG, "Returning to MainActivity from continue play")
            
            val intent = Intent(this, MainActivity::class.java).apply {
                // 清除任务栈，确保回到主页
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            // 正常返回
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
    
    /**
     * VideoAspectCallback 实现
     */
    override fun onVideoAspectChanged(aspect: VideoAspect) {
        currentVideoAspect = aspect
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
            if (::controlsManager.isInitialized) {
                controlsManager.updatePlayPauseButton(false)
            }
            
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
        
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 检查gestureHandler是否已初始化
        if (::gestureHandler.isInitialized) {
            gestureHandler.restoreOriginalSettings()
        }
        
        resumePromptHandler.removeCallbacksAndMessages(null)
        
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
        seekBarThumbnailHelper?.release()
    }
    
    private fun savePlaybackState() {
        val uri = videoUri ?: return
        
        try {
            // 1. 保存播放进度到 PreferencesManager
            if (duration > 0 && currentPosition > 0) {
                preferencesManager.setPlaybackPosition(uri.toString(), currentPosition)
                Logger.d(TAG, "Playback position saved: $currentPosition / $duration")
            }
            
            // 2. 添加到历史记录 - 只记录本地视频，不记录在线视频
            if (duration > 0 && !isOnlineVideo) {  // 添加 !isOnlineVideo 判断
                val fileName = getFileNameFromUri(uri)
                val folderName = uri.getFolderName()
                
                historyManager.addHistory(
                    uri = uri,
                    fileName = fileName,
                    position = (currentPosition * 1000).toLong(),
                    duration = (duration * 1000).toLong(),
                    folderName = folderName
                )
                Logger.d(TAG, "History saved: $fileName")
                
                // 3. 保存弹幕信息到历史记录(保存真实的显示状态)
                val danmakuPath = danmakuManager.getCurrentDanmakuPath()
                if (danmakuPath != null) {
                    // 只有prepared且visible时才保存为true
                    val actualVisible = danmakuManager.isVisible() && danmakuManager.isPrepared()
                    historyManager.updateDanmu(
                        uri = uri,
                        danmuPath = danmakuPath,
                        danmuVisible = actualVisible,
                        danmuOffsetTime = 0L
                    )
                    Logger.d(TAG, "Danmu info saved: path=$danmakuPath, visible=$actualVisible")
                }
            } else if (isOnlineVideo) {
                Logger.d(TAG, "Skipping history for online video: $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save playback state", e)
        }
    }
    
    
    private fun buildLegacyRemotePlaybackRequest(uri: Uri): RemotePlaybackRequest {
        val headers = linkedMapOf<String, String>()
        intent.getStringExtra("cookies")
            ?.takeIf { it.isNotBlank() }
            ?.let { headers["Cookie"] = it }
        intent.getStringExtra("referer")
            ?.takeIf { it.isNotBlank() }
            ?.let { headers["Referer"] = it }
        intent.getStringExtra("user_agent")
            ?.takeIf { it.isNotBlank() }
            ?.let { headers["User-Agent"] = it }

        val urlString = uri.toString()
        if (urlString.contains("bilivideo.com") && headers["Referer"].isNullOrBlank()) {
            headers["Referer"] = "https://www.bilibili.com"
        }

        val source = when {
            intent.hasExtra("cookies") -> RemotePlaybackRequest.Source.BILIBILI
            intent.getBooleanExtra("is_webdav", false) -> RemotePlaybackRequest.Source.WEBDAV
            else -> RemotePlaybackRequest.Source.UNKNOWN
        }

        return RemotePlaybackRequest(
            url = urlString,
            title = resolveVideoTitle(uri),
            sourcePageUrl = RemotePlaybackHeaders.deriveSourcePageUrl(headers),
            headers = RemotePlaybackHeaders.normalize(headers),
            source = source
        )
    }

    private fun isRemotePlaybackUri(uri: Uri?): Boolean {
        val scheme = uri?.scheme?.lowercase().orEmpty()
        return scheme in REMOTE_URI_SCHEMES
    }

    private fun resolveVideoTitle(uri: Uri): String {
        remotePlaybackRequest?.title
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val intentTitle = listOf("video_title", "title", "file_name", "video_name")
            .asSequence()
            .mapNotNull { key -> intent.getStringExtra(key) }
            .firstOrNull { it.isNotBlank() }
        if (!intentTitle.isNullOrBlank()) {
            return intentTitle
        }

        if (isRemotePlaybackUri(uri)) {
            val remoteName = uri.lastPathSegment
                ?.substringBefore("?")
                ?.takeIf { it.isNotBlank() }
                ?.let { Uri.decode(it) }
            return remoteName ?: uri.host ?: "在线视频"
        }

        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        it.getString(nameIndex)
                    } else {
                        uri.lastPathSegment ?: "未知文件"
                    }
                } else {
                    uri.lastPathSegment ?: "未知文件"
                }
            } ?: uri.lastPathSegment ?: "未知文件"
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to resolve file name from uri: $uri", e)
            uri.lastPathSegment ?: "未知文件"
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        return resolveVideoTitle(uri)
    }
    
    private fun applyAnime4K() {
        if (anime4KEnabled) {
            val shaderString = anime4KManager.getShaderChain(anime4KMode, anime4KQuality)
            val shaders = if (shaderString.isNotEmpty()) {
                shaderString.split(":")
            } else {
                emptyList()
            }
            playbackEngine.setShaderList(shaders)
            Logger.d(TAG, "Anime4K applied: mode=$anime4KMode, quality=$anime4KQuality")
        } else {
            playbackEngine.setShaderList(emptyList())
            Logger.d(TAG, "Anime4K disabled")
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
    
    /**
     * 加载网络弹幕
     */
    private fun loadNetworkDanmaku(episodeId: Int, animeTitle: String, episodeTitle: String) {
        lifecycleScope.launch {
            try {
                // 显示加载提示
                DialogUtils.showToastShort(this@VideoPlayerActivity, "正在加载弹幕...")
                
                // 获取弹幕
                val api = com.fam4k007.videoplayer.dandanplay.DanDanPlayApi()
                val result = api.getDanmaku(episodeId)
                
                result.fold(
                    onSuccess = { danmakuResponse ->
                        // 转换为 XML 格式
                        val xmlContent = api.convertToXml(danmakuResponse)
                        
                        // 保存到外部存储的 Android/data/包名/files/danmaku/network 目录
                        // 用户可以通过文件管理器访问
                        val danmakuDir = File(getExternalFilesDir(null), "danmaku/network")
                        if (!danmakuDir.exists()) {
                            danmakuDir.mkdirs()
                        }
                        
                        // 使用番剧名和剧集名生成文件名（先清理特殊字符，再添加.xml后缀）
                        val cleanName = "${animeTitle}_${episodeTitle}_${episodeId}"
                            .replace("[^a-zA-Z0-9_\\u4e00-\\u9fa5]".toRegex(), "_")
                        val fileName = "$cleanName.xml"
                        val danmakuFile = File(danmakuDir, fileName)
                        danmakuFile.writeText(xmlContent)
                        
                        Logger.d(TAG, "网络弹幕已保存到: ${danmakuFile.absolutePath}")
                        
                        // 加载弹幕
                        val loaded = danmakuManager.loadDanmakuFile(danmakuFile.absolutePath, autoShow = true)
                        
                        if (loaded) {
                            // 同步弹幕到当前播放位置
                            val currentPosition = (playbackEngine.currentPosition * 1000).toLong()
                            danmakuManager.seekTo(currentPosition)
                            
                            // 如果视频正在播放，立即启动弹幕
                            if (isPlaying) {
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    danmakuManager.resume()
                                    Logger.d(TAG, "Network danmaku resumed (video playing)")
                                }, 300)
                            } else {
                                Logger.d(TAG, "Network danmaku loaded but not started (video paused)")
                            }
                            
                            Logger.d(TAG, "Network danmaku loaded and synced to position: $currentPosition")
                            
                            DialogUtils.showToastShort(
                                this@VideoPlayerActivity,
                                "弹幕加载成功: $animeTitle - $episodeTitle"
                            )
                            
                            // 更新历史记录
                            videoUri?.let { uri ->
                                historyManager.updateDanmu(
                                    uri = uri,
                                    danmuPath = danmakuFile.absolutePath,
                                    danmuVisible = true,
                                    danmuOffsetTime = 0L
                                )
                                Logger.d(TAG, "Network danmaku updated in history")
                            }
                        } else {
                            DialogUtils.showToastLong(this@VideoPlayerActivity, "弹幕加载失败")
                        }
                    },
                    onFailure = { e ->
                        DialogUtils.showToastLong(
                            this@VideoPlayerActivity,
                            "弹幕加载失败: ${e.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load network danmaku", e)
                DialogUtils.showToastLong(this@VideoPlayerActivity, "弹幕加载异常: ${e.message}")
            }
        }
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
    
    /**
     * 显示匹配结果选择对话框
     */
    private fun showMatchSelectionDialog(matches: List<com.fam4k007.videoplayer.dandanplay.MatchInfo>) {
        val items = matches.map { "${it.animeTitle} - ${it.episodeTitle}" }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择匹配结果")
            .setItems(items) { dialog, which ->
                val match = matches[which]
                loadNetworkDanmaku(match.episodeId, match.animeTitle, match.episodeTitle)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示暂停指示器（方案A：缩放+透明度动画，2秒后自动隐藏）
     */
    private fun showPauseIndicator() {
        // 取消之前的自动隐藏任务
        pauseIndicatorHideRunnable?.let { pauseIndicatorHandler.removeCallbacks(it) }
        
        pauseIndicator.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            
            // 入场动画：缩放（0.8x→1.0x）+ 透明度（0→0.9）
            animate()
                .alpha(0.9f)  // 半透明，不完全不透明
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)  // 300ms动画
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        
        // 2秒后自动隐藏
        pauseIndicatorHideRunnable = Runnable {
            hidePauseIndicator()
        }
        pauseIndicatorHandler.postDelayed(pauseIndicatorHideRunnable!!, 2000)
    }
    
    /**
     * 隐藏暂停指示器（淡出动画）
     */
    private fun hidePauseIndicator() {
        // 取消自动隐藏任务
        pauseIndicatorHideRunnable?.let { pauseIndicatorHandler.removeCallbacks(it) }
        
        pauseIndicator.animate()
            .alpha(0f)
            .setDuration(300)  // 淡出300ms
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                pauseIndicator.visibility = View.GONE
                // 重置状态
                pauseIndicator.scaleX = 0.8f
                pauseIndicator.scaleY = 0.8f
            }
            .start()
    }
    
    override fun onScreenshot() {
        screenshotManager.takeScreenshot()
    }
    
    override fun onShowSkipSettings() {
        skipIntroOutroManager.showSkipSettingsDrawer(currentFolderPath)
    }
    
    /**
     * 显示视频列表抽屉
     */
    private fun showVideoListDrawer() {
        // 如果没有视频列表，提示用户
        if (currentVideoList.isEmpty()) {
            DialogUtils.showToastShort(this, "当前没有可用的视频列表")
            return
        }
        
        videoUri?.let { uri ->
            composeOverlayManager.showVideoListDrawer(
                videoList = currentVideoList,
                currentVideoUri = uri,
                onVideoSelected = { video, index ->
                    // 切换到选中的视频
                    val selectedUri = Uri.parse(video.uri)
                    Logger.d(TAG, "Video selected from list: ${video.name}, index: $index")
                    playVideo(selectedUri)
                }
            )
        }
    }
    
    override fun getVideoUri(): Uri? = videoUri
}

fun Int.dpToPx(): Int {
    return (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
}
