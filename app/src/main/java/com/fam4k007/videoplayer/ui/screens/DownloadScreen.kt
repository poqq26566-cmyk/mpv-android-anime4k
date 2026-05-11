package com.fam4k007.videoplayer.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.download.BilibiliDownloadViewModel
import com.fam4k007.videoplayer.download.DownloadItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: BilibiliDownloadViewModel = viewModel()) {
    val downloadItems by viewModel.downloadItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadPath by viewModel.downloadPath.collectAsState()
    val downloadPathDisplay by viewModel.downloadPathDisplay.collectAsState()
    
    // 解析相关状态 - 现在从 ViewModel 获取
    val isParsing by viewModel.isParsing.collectAsState()
    val parseError by viewModel.parseError.collectAsState()
    val parseResult by viewModel.parseResult.collectAsState()
    val episodeList by viewModel.episodeList.collectAsState()
    val selectedEpisodes by viewModel.selectedEpisodes.collectAsState()
    val isEpisodeListExpanded by viewModel.isEpisodeListExpanded.collectAsState()
    
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var videoUrl by remember { mutableStateOf("") }

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
            // 免责声明
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚠️",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            "下载内容仅供个人学习使用，请勿传播",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 链接输入框卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        placeholder = { 
                            Text(
                                "粘贴 B站 视频或番剧链接...",
                                fontSize = 14.sp
                            ) 
                        },
                        singleLine = false,
                        maxLines = 3,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row {
                                if (videoUrl.isNotEmpty()) {
                                    IconButton(onClick = { videoUrl = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "清除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (videoUrl.isNotBlank()) {
                                            viewModel.parseVideoUrl(videoUrl)
                                        }
                                    },
                                    enabled = videoUrl.isNotBlank() && !isParsing
                                ) {
                                    if (isParsing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = "解析",
                                            tint = if (videoUrl.isNotBlank()) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                }
                
                // 显示错误信息
                parseError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠ $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 功能按钮行
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 设置保存路径按钮
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { folderPickerLauncher.launch(null) },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (downloadPath.isNotEmpty()) "已设置路径" else "设置路径",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    
                    // 开始下载按钮
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                enabled = parseResult != null && (episodeList.isEmpty() || selectedEpisodes.isNotEmpty())
                            ) {
                                viewModel.startSelectedDownloads()
                            },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (parseResult != null && (episodeList.isEmpty() || selectedEpisodes.isNotEmpty()))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (parseResult != null && (episodeList.isEmpty() || selectedEpisodes.isNotEmpty()))
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "开始下载",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (parseResult != null && (episodeList.isEmpty() || selectedEpisodes.isNotEmpty()))
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 解析结果卡片
            parseResult?.let { result ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = result.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            if (episodeList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "共 ${episodeList.size} 集，已选 ${selectedEpisodes.size} 集",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // 集数选择列表
            if (episodeList.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 标题栏
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleEpisodeListExpanded() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "选择集数",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.toggleAllEpisodes() }
                                ) {
                                    Text(
                                        if (selectedEpisodes.size == episodeList.size) "取消全选" else "全选",
                                        fontSize = 13.sp
                                    )
                                }
                                Icon(
                                    if (isEpisodeListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // 集数列表
                            AnimatedVisibility(
                                visible = isEpisodeListExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
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
                                                        viewModel.toggleEpisodeSelection(episode.episodeId)
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = selectedEpisodes.contains(episode.episodeId),
                                                    onCheckedChange = null
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "第${episode.index}集 ${episode.longTitle}",
                                                        style = MaterialTheme.typography.bodyMedium
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 下载历史卡片
            if (downloadItems.isNotEmpty() || isLoading) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 标题栏
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "下载历史",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (downloadItems.isNotEmpty()) {
                                    TextButton(onClick = { viewModel.clearCompletedDownloads() }) {
                                        Text("清除已完成")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 加载中或下载列表
                            if (isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    downloadItems.forEach { item ->
                                        DownloadItemCardContent(item, viewModel)
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

@Composable
fun DownloadItemCardContent(item: DownloadItem, viewModel: BilibiliDownloadViewModel) {
    Column {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
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
                style = MaterialTheme.typography.bodySmall,
                color = when (item.status) {
                    "completed" -> MaterialTheme.colorScheme.primary
                    "failed" -> MaterialTheme.colorScheme.error
                    "downloading" -> MaterialTheme.colorScheme.tertiary
                    "merging" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp
            )
            
            item.errorMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "错误: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp
                )
            }
            
            if (item.progress > 0 && item.status != "completed") {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = item.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.progress}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (item.status !in listOf("completed", "cancelled")) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (item.status == "downloading" || item.status == "merging") {
                        TextButton(
                            onClick = { viewModel.pauseDownload(item) }
                        ) {
                            Text("暂停", fontSize = 13.sp)
                        }
                    } else if (item.status == "paused") {
                        TextButton(
                            onClick = { viewModel.resumeDownload(item) }
                        ) {
                            Text("恢复", fontSize = 13.sp)
                        }
                    }
                    
                    TextButton(
                        onClick = { viewModel.cancelDownload(item) }
                    ) {
                        Text("取消", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}
