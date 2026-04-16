package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import com.fam4k007.videoplayer.BaseComponentActivity
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.download.BilibiliDownloadViewModel
import com.fam4k007.videoplayer.download.DownloadItem
import com.fam4k007.videoplayer.download.MediaParseResult
import com.fam4k007.videoplayer.download.MediaType
import com.fam4k007.videoplayer.download.EpisodeInfo
import com.fam4k007.videoplayer.compose.ImmersiveTopAppBar
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager
import kotlinx.coroutines.launch
import android.util.Log

class DownloadActivity : BaseComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableEdgeToEdge()
        
        setContent {
            val themeColors = getThemeColors(this@DownloadActivity, ThemeManager.getCurrentTheme(this@DownloadActivity).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = androidx.compose.ui.graphics.Color.White, // 固定白色背景
                    onBackground = themeColors.onBackground,
                    surface = themeColors.primary, // 使用主题主色调
                    surfaceVariant = themeColors.primary, // 使用主题主色调
                    onSurface = themeColors.onSurface
                )
            ) {
                DownloadScreen()
            }
        }
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
    val activity = context as? BaseComponentActivity

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
                val displayPath = docFile?.name ?: context.getString(R.string.download_custom_path)
                
                viewModel.setDownloadPath(it.toString(), displayPath)
                Log.d("DownloadActivity", "选择的文件夹: $it, 显示名称: $displayPath")
            } catch (e: Exception) {
                Log.e("DownloadActivity", "选择文件夹失败", e)
            }
        }
    }

    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.download_title),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        activity?.finish()
                        activity?.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
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
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.download_add_task),
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 免责声明
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFFF3E0)
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
                                    stringResource(R.string.download_disclaimer),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color(0xFFFF6F00),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.download_disclaimer_text),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color(0xFFE65100),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text(stringResource(R.string.download_bilibili_link)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = androidx.compose.ui.graphics.Color.Black,
                            unfocusedTextColor = androidx.compose.ui.graphics.Color.Black
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
                                                        parseError = context.getString(R.string.download_get_episode_failed, episodesResult.exceptionOrNull()?.message ?: "")
                                                        Log.e("DownloadActivity", "获取集数失败", episodesResult.exceptionOrNull())
                                                    }
                                                } else {
                                                    parseError = context.getString(R.string.download_parse_failed)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            parseError = e.message ?: context.getString(R.string.download_episode_parse_error)
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
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            } else {
                                Text(if (parseResult == null) stringResource(R.string.download_parse_link) else stringResource(R.string.download_reparse))
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
                            Text(stringResource(R.string.download_start))
                        }
                    }

                    // 显示错误信息
                    parseError?.let { error ->
                        Text(
                            text = stringResource(R.string.download_error_prefix, error),
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
                            text = stringResource(R.string.download_storage_path, downloadPathDisplay),
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.Gray,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        TextButton(onClick = { 
                            folderPickerLauncher.launch(null)
                        }) {
                            Text(stringResource(R.string.download_change_path))
                        }
                    }

                    // 显示解析结果
                    parseResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ ${result.title}",
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
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
                                    androidx.compose.ui.graphics.Color(0xFFE3F2FD),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.download_episode_selection, episodeList.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = androidx.compose.ui.graphics.Color(0xFF1976D2),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isEpisodeListExpanded) stringResource(R.string.download_collapse) else stringResource(R.string.download_expand),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color(0xFF1976D2),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    if (isEpisodeListExpanded) "▼" else "▶",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = androidx.compose.ui.graphics.Color(0xFF1976D2)
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
                                                    text = stringResource(R.string.download_episode_format, episode.index, episode.longTitle),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = androidx.compose.ui.graphics.Color.Black
                                                )
                                                if (episode.badge.isNotEmpty()) {
                                                    Text(
                                                        text = episode.badge,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (episode.badgeType == 1) {
                                                            androidx.compose.ui.graphics.Color(0xFFFB7299)
                                                        } else {
                                                            androidx.compose.ui.graphics.Color.Gray
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
                            stringResource(R.string.download_history),
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.Black
                        )
                        TextButton(onClick = { viewModel.clearCompletedDownloads() }) {
                            Text(stringResource(R.string.download_clear_completed))
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
            containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.ui.graphics.Color.Black
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = when (item.status) {
                    "pending" -> stringResource(R.string.download_status_pending)
                    "downloading" -> stringResource(R.string.download_status_downloading)
                    "paused" -> stringResource(R.string.download_status_paused)
                    "completed" -> stringResource(R.string.download_status_completed)
                    "failed" -> stringResource(R.string.download_status_failed)
                    "merging" -> stringResource(R.string.download_status_merging)
                    else -> item.status
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when (item.status) {
                    "completed" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    "failed" -> androidx.compose.ui.graphics.Color(0xFFF44336)
                    "downloading" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
                    "merging" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                    else -> androidx.compose.ui.graphics.Color.Gray
                }
            )
            
            item.errorMessage?.let {
                Text(
                    text = stringResource(R.string.download_error_prefix, it),
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
                    color = androidx.compose.ui.graphics.Color.Black
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
                                containerColor = androidx.compose.ui.graphics.Color(0xFFFF9800)
                            )
                        ) {
                            Text(stringResource(R.string.download_pause))
                        }
                    } else if (item.status == "paused") {
                        Button(
                            onClick = { viewModel.resumeDownload(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            )
                        ) {
                            Text(stringResource(R.string.download_resume))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { viewModel.cancelDownload(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFF44336)
                        )
                    ) {
                        Text(stringResource(R.string.download_cancel))
                    }
                }
            }
        }
    }
}