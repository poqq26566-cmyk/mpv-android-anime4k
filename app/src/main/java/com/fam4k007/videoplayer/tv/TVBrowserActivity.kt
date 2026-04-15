package com.fam4k007.videoplayer.tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.compose.ImmersiveTopAppBar
import com.fam4k007.videoplayer.remote.RemotePlaybackLauncher
import com.fam4k007.videoplayer.sniffer.DetectedVideo
import com.fam4k007.videoplayer.sniffer.VideoSnifferManager
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * TV浏览器 - 带视频嗅探功能的WebView
 * 使用Compose架构
 */
class TVBrowserActivity : ComponentActivity() {
    companion object {
        private const val TAG = "TVBrowserActivity"
        const val EXTRA_URL = "extra_url"
        
        fun start(context: Context, url: String = "") {
            val intent = Intent(context, TVBrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            context.startActivity(intent)
        }
    }
    
    private var webView: WebView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableEdgeToEdge()
        
        val initialUrl = intent.getStringExtra(EXTRA_URL) ?: ""
        
        setContent {
            val themeColors = com.fam4k007.videoplayer.ui.theme.getThemeColors(
                ThemeManager.getCurrentTheme(this).themeName
            )
            
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = themeColors.onBackground,
                    surface = themeColors.surface,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = themeColors.onSurface
                )
            ) {
                TVBrowserScreen(
                    initialUrl = initialUrl,
                    onWebViewCreated = { webView = it },
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 从播放器返回时恢复WebView
        webView?.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        // 进入播放器时暂停WebView，但不销毁
        webView?.onPause()
    }
    
    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVBrowserScreen(
    initialUrl: String,
    onWebViewCreated: (WebView) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val detectedVideos by VideoSnifferManager.detectedVideos.collectAsStateWithLifecycle()
    
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var currentTitle by remember { mutableStateOf("TV浏览器") }
    var isLoading by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(initialUrl) }
    var showUrlBar by remember { mutableStateOf(initialUrl.isEmpty()) }
    var webViewInstance: WebView? by remember { mutableStateOf(null) }
    var urlToLoad by remember { mutableStateOf(initialUrl) }  // 用于触发URL加载
    
    // 保存当前页面信息，用于视频嗅探（避免在后台线程访问WebView）
    var currentPageUrl by remember { mutableStateOf(initialUrl) }
    var currentPageTitle by remember { mutableStateOf("TV浏览器") }
    
    // 在页面首次加载时清空之前的嗅探结果
    DisposableEffect(Unit) {
        VideoSnifferManager.clear()
        Log.d("TVBrowser", "Cleared previous detected videos on entry")
        onDispose { }
    }
    
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(Color.White)
            ) {
                ImmersiveTopAppBar(
                    title = { 
                        Text(
                            text = if (showUrlBar) "输入网址" else currentTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                        }
                    },
                    actions = {
                        // 地址栏切换按钮
                        IconButton(onClick = { showUrlBar = !showUrlBar }) {
                            Icon(
                                if (showUrlBar) Icons.Default.Close else Icons.Default.Search,
                                if (showUrlBar) "关闭地址栏" else "打开地址栏",
                                tint = Color.White
                            )
                        }
                        // 播放按钮 - 智能选择最佳视频
                        Box {
                            IconButton(
                                onClick = { 
                                    if (detectedVideos.isNotEmpty()) {
                                        // 智能选择最佳视频
                                        val bestVideo = selectBestVideo(detectedVideos)
                                        playVideo(context, bestVideo)
                                    }
                                },
                                enabled = detectedVideos.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    "播放视频",
                                    tint = if (detectedVideos.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f)
                                )
                            }
                            // 右上角红点提示
                            if (detectedVideos.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-8).dp, y = 8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                    }
                )
                
                // 地址栏 - 卡片式设计
                AnimatedVisibility(
                    visible = showUrlBar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // 卡片式地址栏
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                OutlinedTextField(
                                    value = urlInput,
                                    onValueChange = { urlInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("输入网址，例如：bilibili.com") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.Language, "网址", tint = Color(0xFF666666))
                                    },
                                    trailingIcon = {
                                        if (urlInput.isNotEmpty()) {
                                            IconButton(onClick = { urlInput = "" }) {
                                                Icon(Icons.Default.Clear, "清除", tint = Color(0xFF666666))
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedTextColor = Color.Black,
                                        focusedTextColor = Color.Black
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 前往按钮
                            Button(
                                onClick = {
                                    if (urlInput.isNotEmpty()) {
                                        var url = urlInput.trim()
                                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                            url = "https://$url"
                                        }
                                        urlToLoad = url
                                        showUrlBar = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("前往", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // 移除悬浮播放按钮，因为现在自动播放
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            if (urlToLoad.isNotEmpty()) {
                // 有URL时显示WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setupWebView(
                                onUrlChanged = { url -> 
                                    currentUrl = url
                                    urlInput = url  // 同步到地址栏
                                },
                                onTitleChanged = { title -> currentTitle = title },
                                onLoadingChanged = { loading -> isLoading = loading },
                                onPageUrlChanged = { pageUrl, pageTitle ->
                                    // 页面切换时清空之前的视频，并更新当前页面信息
                                    currentPageUrl = pageUrl
                                    currentPageTitle = pageTitle
                                    VideoSnifferManager.startNewPage()
                                },
                                getCurrentPageUrl = { currentPageUrl },
                                getCurrentPageTitle = { currentPageTitle }
                            )
                            loadUrl(urlToLoad)
                            webViewInstance = this
                            onWebViewCreated(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { webView ->
                        // 当urlToLoad变化时，加载新URL
                        if (webView.url != urlToLoad && urlToLoad.isNotEmpty()) {
                            webView.loadUrl(urlToLoad)
                        }
                    }
                )
            } else {
                // 没有URL时显示空白
                Box(modifier = Modifier.fillMaxSize())
            }
            
            // 加载指示器
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * 配置WebView
 */
@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView(
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onPageUrlChanged: (String, String) -> Unit,
    getCurrentPageUrl: () -> String,
    getCurrentPageTitle: () -> String
) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        
        // 设置User-Agent
        userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
    
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { 
                onUrlChanged(it)
                onLoadingChanged(true)
                Log.d("TVBrowser", "Page started: $it")
                
                // 页面开始加载，准备检测新页面
                val title = view?.title ?: "TV浏览器"
                onPageUrlChanged(it, title)
            }
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            view?.title?.let { onTitleChanged(it) }
            onLoadingChanged(false)
            Log.d("TVBrowser", "Page finished: $url, title: ${view?.title}")
        }
        
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            request?.let { req ->
                val url = req.url.toString()
                val headers = req.requestHeaders
                // 使用回调获取页面信息，避免在后台线程访问WebView
                val pageUrl = getCurrentPageUrl()
                val pageTitle = getCurrentPageTitle()
                
                // 处理请求，检测视频
                VideoSnifferManager.processRequest(url, headers, pageUrl, pageTitle)
            }
            
            return super.shouldInterceptRequest(view, request)
        }
    }
}

/**
 * 智能选择最佳视频
 * 优先级：
 * 1. .m3u8 / .mpd 流媒体格式（通常是清晰度最高的）
 * 2. .mp4 格式
 * 3. 其他视频格式
 * 在相同格式内，按质量评分选择最佳
 */
private fun selectBestVideo(videos: List<DetectedVideo>): DetectedVideo {
    if (videos.isEmpty()) return videos.first()
    if (videos.size == 1) return videos.first()

    // 优先选择流媒体清单（HLS / DASH）
    val manifestVideos = videos.filter {
        it.url.contains(".m3u8", ignoreCase = true) || it.url.contains(".mpd", ignoreCase = true)
    }
    if (manifestVideos.isNotEmpty()) {
        return selectBestInGroup(manifestVideos)
    }

    // 其次选择mp4
    val mp4Videos = videos.filter { it.url.contains(".mp4", ignoreCase = true) }
    if (mp4Videos.isNotEmpty()) {
        return selectBestInGroup(mp4Videos)
    }

    // 最后选择其他格式
    return selectBestInGroup(videos)
}

/**
 * 在视频组内选择最佳的（按质量评分）
 */
private fun selectBestInGroup(videos: List<DetectedVideo>): DetectedVideo {
    if (videos.size == 1) return videos.first()

    // 计算每个视频的质量评分
    val scoredVideos = videos.map { video ->
        val score = calculateQualityScore(video.url)
        Pair(video, score)
    }

    // 按评分降序，评分相同时按URL长度降序
    return scoredVideos.sortedWith(compareByDescending<Pair<DetectedVideo, Int>> { it.second }.thenByDescending { it.first.url.length })
        .first().first
}

/**
 * 计算视频URL的质量评分
 * 基于URL中的质量关键词
 */
private fun calculateQualityScore(url: String): Int {
    val lowerUrl = url.lowercase()

    // 高质量关键词（高分）
    val highQualityKeywords = listOf(
        "1080p", "1080", "4k", "2160p", "2160", "uhd",
        "hd", "high", "best", "master", "premium", "vip"
    )

    // 中等质量关键词
    val mediumQualityKeywords = listOf(
        "720p", "720", "fhd", "fullhd"
    )

    // 低质量关键词（低分）
    val lowQualityKeywords = listOf(
        "360p", "360", "480p", "480", "sd", "low", "mobile"
    )

    var score = 0

    // 检查高质量关键词
    for (keyword in highQualityKeywords) {
        if (lowerUrl.contains(keyword)) {
            score += 10
        }
    }

    // 检查中等质量关键词
    for (keyword in mediumQualityKeywords) {
        if (lowerUrl.contains(keyword)) {
            score += 5
        }
    }

    // 检查低质量关键词（降低分数）
    for (keyword in lowQualityKeywords) {
        if (lowerUrl.contains(keyword)) {
            score -= 5
        }
    }

    // 降低重定向/包装URL的分数
    if (lowerUrl.contains("php?") || lowerUrl.contains("?url=") || lowerUrl.contains("redirect")) {
        score -= 10  // 大幅降低重定向URL分数
    }

    // 如果URL包含数字分辨率，尝试解析
    val resolutionPattern = Regex("(\\d{3,4})p")
    val match = resolutionPattern.find(lowerUrl)
    if (match != null) {
        val res = match.groupValues[1].toIntOrNull()
        if (res != null) {
            // 按分辨率给分：1080p=10, 720p=7, 480p=4, 360p=3
            score += when {
                res >= 1080 -> 10
                res >= 720 -> 7
                res >= 480 -> 4
                res >= 360 -> 3
                else -> 1
            }
        }
    }

    // 如果没有质量标识，给予基础分数（避免0分）
    if (score == 0) {
        score = 1
    }

    return score
}

/**
 * 播放视频
 */
private fun playVideo(context: Context, video: DetectedVideo) {
    Log.d("TVBrowser", "Playing video: ${video.url}")
    Log.d("TVBrowser", "With headers: ${video.headers}")

    val request = video.toRemotePlaybackRequest().copy(
        title = video.title.ifEmpty { "在线视频" }
    )
    RemotePlaybackLauncher.start(context, request)
}
