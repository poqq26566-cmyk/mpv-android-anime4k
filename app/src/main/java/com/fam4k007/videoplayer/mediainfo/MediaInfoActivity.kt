package com.fam4k007.videoplayer.mediainfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import com.fam4k007.videoplayer.utils.MediaInfoHelper
import org.koin.androidx.compose.KoinAndroidContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 媒体信息详情页面
 * 显示视频文件的详细信息
 */
class MediaInfoActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MediaInfoActivity"
        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_VIDEO_NAME = "extra_video_name"

        fun start(context: Context, videoUri: String, videoName: String) {
            val intent = Intent(context, MediaInfoActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URI, videoUri)
                putExtra(EXTRA_VIDEO_NAME, videoName)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示（沉浸式状态栏）
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val videoUri = intent.getStringExtra(EXTRA_VIDEO_URI) ?: ""
        val videoName = intent.getStringExtra(EXTRA_VIDEO_NAME) ?: "未知视频"

        setContent {
            KoinAndroidContext {
                val themeController = ThemeController.from(this@MediaInfoActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MediaInfoScreen(
                            videoUri = videoUri,
                            videoName = videoName,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaInfoScreen(
    videoUri: String,
    videoName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var mediaInfo by remember { mutableStateOf<MediaInfoHelper.MediaInfoData?>(null) }
    var fullTextContent by remember { mutableStateOf<String?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // 加载媒体信息
    LaunchedEffect(Unit) {
        if (videoUri.isEmpty()) {
            error = "无效的视频地址"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val uri = Uri.parse(videoUri)
            
            // 加载详细信息
            val result = MediaInfoHelper.getMediaInfo(context, uri, videoName)
            result.onSuccess { info ->
                mediaInfo = info
                
                // 生成文本内容用于复制/分享
                val textResult = MediaInfoHelper.generateTextOutput(context, uri, videoName)
                textResult.onSuccess { text ->
                    fullTextContent = text
                }
                
                isLoading = false
            }.onFailure { e ->
                error = e.message ?: "加载媒体信息失败"
                isLoading = false
                Log.e("MediaInfoActivity", "Failed to load media info", e)
            }
        } catch (e: Exception) {
            error = e.message ?: "未知错误"
            isLoading = false
            Log.e("MediaInfoActivity", "Error in loading", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "媒体信息",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = videoName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "返回",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    if (!isLoading && error == null && fullTextContent != null) {
                        Row(modifier = Modifier.padding(end = 8.dp)) {
                            // 复制按钮
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        copyToClipboard(context, fullTextContent!!, videoName)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    "复制",
                                    tint = Color.Black
                                )
                            }
                            
                            // 分享按钮
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        shareMediaInfo(context, fullTextContent!!, videoName)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    "分享",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "未知错误",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                mediaInfo != null && fullTextContent != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 标签页选择器
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text("详细信息", color = Color.Black) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                text = { Text("原始信息", color = Color.Black) }
                            )
                        }
                        
                        // 标签页内容
                        when (selectedTabIndex) {
                            0 -> MediaInfoContent(mediaInfo = mediaInfo!!)
                            1 -> RawInfoContent(fullText = fullTextContent!!)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RawInfoContent(fullText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        ) {
            SelectionContainer {
                Text(
                    text = fullText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MediaInfoContent(mediaInfo: MediaInfoHelper.MediaInfoData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 通用信息
        InfoSection(title = "通用信息") {
            val general = mediaInfo.general
            if (general.format.isNotEmpty()) InfoRow("格式", general.format)
            if (general.formatVersion.isNotEmpty()) InfoRow("格式版本", general.formatVersion)
            if (general.fileSize.isNotEmpty()) InfoRow("文件大小", general.fileSize)
            if (general.duration.isNotEmpty()) InfoRow("时长", general.duration)
            if (general.overallBitRate.isNotEmpty()) InfoRow("总比特率", general.overallBitRate)
            if (general.frameRate.isNotEmpty()) InfoRow("帧率", general.frameRate)
            if (general.title.isNotEmpty()) InfoRow("标题", general.title)
            if (general.writingApplication.isNotEmpty()) InfoRow("编码应用", general.writingApplication)
            if (general.writingLibrary.isNotEmpty()) InfoRow("编码库", general.writingLibrary)
            if (general.encodedDate.isNotEmpty()) InfoRow("编码日期", general.encodedDate)
        }

        // 视频流信息
        mediaInfo.videoStreams.forEachIndexed { index, stream ->
            InfoSection(title = "视频流 #${index + 1}") {
                if (stream.format.isNotEmpty()) InfoRow("编码", stream.format)
                if (stream.formatProfile.isNotEmpty()) InfoRow("配置", stream.formatProfile)
                if (stream.width.isNotEmpty() && stream.height.isNotEmpty()) {
                    InfoRow("分辨率", "${stream.width} × ${stream.height}")
                }
                if (stream.displayAspectRatio.isNotEmpty()) InfoRow("宽高比", stream.displayAspectRatio)
                if (stream.frameRate.isNotEmpty()) InfoRow("帧率", stream.frameRate)
                if (stream.frameRateMode.isNotEmpty()) InfoRow("帧率模式", stream.frameRateMode)
                if (stream.bitRate.isNotEmpty()) InfoRow("比特率", stream.bitRate)
                if (stream.bitDepth.isNotEmpty()) InfoRow("位深度", stream.bitDepth)
                if (stream.colorSpace.isNotEmpty()) InfoRow("色彩空间", stream.colorSpace)
                if (stream.chromaSubsampling.isNotEmpty()) InfoRow("色度子采样", stream.chromaSubsampling)
                if (stream.hdrFormat.isNotEmpty()) InfoRow("HDR格式", stream.hdrFormat)
                if (stream.maxCLL.isNotEmpty()) InfoRow("最大内容亮度", stream.maxCLL)
                if (stream.maxFALL.isNotEmpty()) InfoRow("最大帧平均亮度", stream.maxFALL)
                if (stream.encodingLibrary.isNotEmpty()) InfoRow("编码库", stream.encodingLibrary)
                if (stream.streamSize.isNotEmpty()) InfoRow("流大小", stream.streamSize)
                if (stream.duration.isNotEmpty()) InfoRow("时长", stream.duration)
            }
        }

        // 音频流信息
        mediaInfo.audioStreams.forEachIndexed { index, stream ->
            InfoSection(title = "音频流 #${index + 1}") {
                if (stream.format.isNotEmpty()) InfoRow("编码", stream.format)
                if (stream.channels.isNotEmpty()) InfoRow("声道", stream.channels)
                if (stream.channelLayout.isNotEmpty()) InfoRow("声道布局", stream.channelLayout)
                if (stream.samplingRate.isNotEmpty()) InfoRow("采样率", stream.samplingRate)
                if (stream.bitRate.isNotEmpty()) InfoRow("比特率", stream.bitRate)
                if (stream.language.isNotEmpty()) InfoRow("语言", stream.language)
                if (stream.title.isNotEmpty()) InfoRow("标题", stream.title)
                if (stream.compressionMode.isNotEmpty()) InfoRow("压缩模式", stream.compressionMode)
                if (stream.delay.isNotEmpty()) InfoRow("延迟", stream.delay)
                if (stream.streamSize.isNotEmpty()) InfoRow("流大小", stream.streamSize)
                if (stream.duration.isNotEmpty()) InfoRow("时长", stream.duration)
            }
        }

        // 字幕流信息
        mediaInfo.textStreams.forEachIndexed { index, stream ->
            InfoSection(title = "字幕流 #${index + 1}") {
                if (stream.format.isNotEmpty()) InfoRow("格式", stream.format)
                if (stream.codecId.isNotEmpty()) InfoRow("编码ID", stream.codecId)
                if (stream.language.isNotEmpty()) InfoRow("语言", stream.language)
                if (stream.title.isNotEmpty()) InfoRow("标题", stream.title)
                if (stream.muxingMode.isNotEmpty()) InfoRow("混流模式", stream.muxingMode)
                if (stream.countOfElements.isNotEmpty()) InfoRow("元素数量", stream.countOfElements)
                if (stream.streamSize.isNotEmpty()) InfoRow("流大小", stream.streamSize)
                if (stream.duration.isNotEmpty()) InfoRow("时长", stream.duration)
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = Color.Black,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private suspend fun copyToClipboard(context: Context, text: String, fileName: String) {
    withContext(Dispatchers.Main) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("媒体信息 - $fileName", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

private suspend fun shareMediaInfo(context: Context, text: String, fileName: String) {
    withContext(Dispatchers.Main) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "媒体信息 - $fileName")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "分享媒体信息"))
        } catch (e: Exception) {
            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
