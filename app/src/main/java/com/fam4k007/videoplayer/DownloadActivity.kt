package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam4k007.videoplayer.download.BilibiliDownloadViewModel
import com.fam4k007.videoplayer.download.DownloadItem
import com.fam4k007.videoplayer.download.MediaParseResult
import com.fam4k007.videoplayer.download.MediaType
import com.fam4k007.videoplayer.download.EpisodeInfo
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import kotlinx.coroutines.launch
import android.util.Log
import org.koin.androidx.compose.KoinAndroidContext

class DownloadActivity : ComponentActivity() {
    private var themeRevision by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@DownloadActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    DownloadScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        themeRevision++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: BilibiliDownloadViewModel = viewModel()) {
    val downloadItems by viewModel.downloadItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadPath by viewModel.downloadPath.collectAsState()
    val downloadPathDisplay by viewModel.downloadPathDisplay.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var videoUrl by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var parseResult by remember { mutableStateOf<MediaParseResult?>(null) }
    var episodeList by remember { mutableStateOf<List<EpisodeInfo>>(emptyList()) }
    var selectedEpisodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isEpisodeListExpanded by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // 文件夹选择器 - 使用旧版本更简单
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // 授予持久化URI权限
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                // 使用DocumentFile API处理
                val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, it)
                val displayPath = docFile?.name ?: "自定义路径"
                
                viewModel.setDownloadPath(it.toString(), displayPath)
                Log.d("DownloadActivity", "选择的文件夹: $it, 显示名称: $displayPath")
            } catch (e: Exception) {
                Log.e("DownloadActivity", "选择文件夹失败", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "哔哩哔哩视频下载",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        activity?.finish()
                        activity?.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 输入区域
            item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "添加下载任务",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 免责声明
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⚠️",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    "免责声明",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    "下载内容仅供个人学习使用，请勿传播。\n使用本功能产生的法律责任由用户自行承担。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("B站视频/番剧链接") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                // 提取文本中的URL（支持带文本的链接）
                                val extractedUrl = videoUrl.trim().let { text ->
                                    // 尝试匹配 b23.tv 短链
                                    val shortUrlRegex = """https?://b23\.tv/\w+""".toRegex()
                                    val shortUrlMatch = shortUrlRegex.find(text)
                                    if (shortUrlMatch != null) {
                                        return@let shortUrlMatch.value
                                    }
                                    
                                    // 尝试匹配 bilibili.com 链接
                                    val biliUrlRegex = """https?://(?:www\.)?bilibili\.com/[^\s\u3011\]\)]+""".toRegex()
                                    val biliUrlMatch = biliUrlRegex.find(text)
                                    if (biliUrlMatch != null) {
                                        return@let biliUrlMatch.value
                                    }
                                    
                                    // 如果没有匹配到URL，返回原文本
                                    text
                                }
                                
                                if (extractedUrl.isNotBlank()) {
                                    isParsing = true
                                    parseError = null
                                    parseResult = null
                                    episodeList = emptyList()
                                    selectedEpisodes = emptySet()
                                    
                                    scope.launch {
                                        try {
                                            val result = viewModel.parseMediaUrlSync(extractedUrl)
                                            parseResult = result
                                            
                                            // 如果是番剧，获取所有集数
                                            if (result.type == MediaType.Bangumi) {
                                                // 优先使用seasonId，否则使用epId
                                                val queryId = result.seasonId ?: result.epId
                                                if (queryId != null) {
                                                    Log.d("DownloadActivity", "获取番剧集数，ID: $queryId")
                                                    val episodesResult = viewModel.getBangumiEpisodesSync(queryId)
                                                    if (episodesResult.isSuccess) {
                                                        episodeList = episodesResult.getOrNull() ?: emptyList()
                                                        Log.d("DownloadActivity", "获取到${episodeList.size}集")
                                                    } else {
                                                        parseError = "获取集数失败: ${episodesResult.exceptionOrNull()?.message}"
                                                        Log.e("DownloadActivity", "获取集数失败", episodesResult.exceptionOrNull())
                                                    }
                                                } else {
                                                    parseError = "番剧ID解析失败"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            parseError = e.message ?: "解析失败"
                                            Log.e("DownloadActivity", "解析失败", e)
                                        } finally {
                                            isParsing = false
                                        }
                                    }
                                }
                            },
                            enabled = !isParsing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isParsing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (parseResult == null) "解析链接" else "重新解析")
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                parseResult?.let { result ->
                                    if (episodeList.isNotEmpty() && selectedEpisodes.isNotEmpty()) {
                                        // 下载选中的集数
                                        episodeList.filter { selectedEpisodes.contains(it.episodeId) }
                                            .forEach { episode ->
                                                viewModel.addDownloadByEpisode(episode, result.seasonId ?: "")
                                            }
                                        // 只清空选择，保留解析结果
                                        selectedEpisodes = emptySet()
                                    } else if (episodeList.isEmpty()) {
                                        // 普通视频，直接下载
                                        viewModel.addDownloadByMediaParse(result)
                                    }
                                }
                            },
                            enabled = parseResult != null && (episodeList.isEmpty() || selectedEpisodes.isNotEmpty()),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("开始下载")
                        }
                    }

                    // 显示错误信息
                    parseError?.let { error ->
                        Text(
                            text = "错误: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // 显示存储路径
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "存储路径: $downloadPathDisplay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        TextButton(onClick = { 
                            folderPickerLauncher.launch(null)
                        }) {
                            Text("更改路径")
                        }
                    }

                    // 显示解析结果
                    parseResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ ${result.title}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // 显示集数列表（折叠式）
                    if (episodeList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEpisodeListExpanded = !isEpisodeListExpanded }
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "选择集数（共${episodeList.size}集）",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isEpisodeListExpanded) "收起" else "展开",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    if (isEpisodeListExpanded) "▼" else "▶",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        AnimatedVisibility(
                            visible = isEpisodeListExpanded,
                            enter = expandVertically(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                ) {
                                    items(episodeList) { episode ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedEpisodes = if (selectedEpisodes.contains(episode.episodeId)) {
                                                        selectedEpisodes - episode.episodeId
                                                    } else {
                                                        selectedEpisodes + episode.episodeId
                                                    }
                                                }
                                                .padding(vertical = 8.dp)
                                                .padding(horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectedEpisodes.contains(episode.episodeId),
                                                onCheckedChange = null // 使用Row的clickable代替
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "第${episode.index}集 ${episode.longTitle}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (episode.badge.isNotEmpty()) {
                                                    Text(
                                                        text = episode.badge,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (episode.badgeType == 1) {
                                                            MaterialTheme.colorScheme.error
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            // 间距
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        
            // 下载历史标题
            if (downloadItems.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "下载历史",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { viewModel.clearCompletedDownloads() }) {
                            Text("清除已完成")
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 加载中或下载列表
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(downloadItems) { item ->
                    DownloadItemCard(item, viewModel)
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(item: DownloadItem, viewModel: BilibiliDownloadViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = when (item.status) {
                    "pending" -> "等待中"
                    "downloading" -> "下载中"
                    "paused" -> "已暂停"
                    "completed" -> "已完成"
                    "failed" -> "失败"
                    "merging" -> "合并中"
                    else -> item.status
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when (item.status) {
                    "completed" -> MaterialTheme.colorScheme.primary
                    "failed" -> MaterialTheme.colorScheme.error
                    "downloading" -> MaterialTheme.colorScheme.tertiary
                    "merging" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            item.errorMessage?.let {
                Text(
                    text = "错误: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (item.progress > 0 && item.status != "completed") {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = item.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${item.progress}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (item.status !in listOf("completed", "cancelled")) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (item.status == "downloading" || item.status == "merging") {
                        Button(
                            onClick = { viewModel.pauseDownload(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("暂停")
                        }
                    } else if (item.status == "paused") {
                        Button(
                            onClick = { viewModel.resumeDownload(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("恢复")
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { viewModel.cancelDownload(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("取消")
                    }
                }
            }
        }
    }
}