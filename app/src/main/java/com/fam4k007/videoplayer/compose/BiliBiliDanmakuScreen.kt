package com.fam4k007.videoplayer.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.compose.SettingsColors as SettingsPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliBiliDanmakuScreen(
    savedFolderUri: Uri?,
    downloadProgress: DownloadProgress,
    isDownloading: Boolean,
    onBack: () -> Unit,
    onFolderSelected: (Uri) -> Unit,
    onDownloadDanmaku: (String, Boolean) -> Unit
) {
    var currentFolderUri by remember { mutableStateOf(savedFolderUri) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // 当开始下载时显示进度弹窗
    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            showProgressDialog = true
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            onFolderSelected(it)
            currentFolderUri = it
        }
    }

    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = { Text(stringResource(R.string.bilibili_danmaku_title), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 设置保存文件夹
            SettingCard(
                icon = Icons.Default.Folder,
                title = stringResource(R.string.bilibili_danmaku_set_folder),
                subtitle = currentFolderUri?.let {
                    it.lastPathSegment?.substringAfter(':') ?: stringResource(R.string.bilibili_danmaku_folder_set) 
                } ?: stringResource(R.string.bilibili_danmaku_click_select),
                onClick = { folderPickerLauncher.launch(currentFolderUri) }
            )

            // 下载弹幕
            SettingCard(
                icon = Icons.Default.CloudDownload,
                title = stringResource(R.string.bilibili_danmaku_download),
                subtitle = if (currentFolderUri != null) stringResource(R.string.bilibili_danmaku_click_input) else stringResource(R.string.bilibili_danmaku_set_folder_first),
                onClick = {
                    if (currentFolderUri != null) {
                        showDownloadDialog = true
                    }
                },
                enabled = currentFolderUri != null
            )

            // 使用说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SettingsPalette.CardBackground
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bilibili_danmaku_instructions),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsPalette.PrimaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = """
                            1. 先设置弹幕保存文件夹
                            2. 输入B站视频或番剧链接
                            3. 选择下载模式（单集或整季）
                            4. 点击下载，等待完成
                            
                            支持的链接格式：
                            • https://www.bilibili.com/video/BVxxx
                            • https://www.bilibili.com/bangumi/play/epxxx
                        """.trimIndent(),
                        fontSize = 14.sp,
                        color = SettingsPalette.SecondaryText,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    // 下载对话框
    if (showDownloadDialog) {
        DownloadDialog(
            onDismiss = { showDownloadDialog = false },
            onDownload = { url, downloadWholeSeason ->
                showDownloadDialog = false
                onDownloadDanmaku(url, downloadWholeSeason)
            }
        )
    }
    
    // 下载进度弹窗
    if (showProgressDialog) {
        DownloadProgressDialog(
            progress = downloadProgress,
            isDownloading = isDownloading,
            onDismiss = { 
                if (!isDownloading) {
                    showProgressDialog = false
                }
            }
        )
    }
}

// 下载进度数据类
data class DownloadProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentTitle: String = "",
    val successCount: Int = 0,
    val failedCount: Int = 0
)

@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SettingsPalette.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SettingsPalette.IconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) SettingsPalette.PrimaryText
                           else SettingsPalette.DisabledText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = if (enabled) SettingsPalette.SecondaryText 
                           else SettingsPalette.DisabledText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SettingsPalette.TertiaryText
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadDialog(
    onDismiss: () -> Unit,
    onDownload: (String, Boolean) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var downloadWholeSeason by remember { mutableStateOf(true) }
    val accentColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bilibili_danmaku_download_title), color = SettingsPalette.PrimaryText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.bilibili_danmaku_link_label)) },
                    placeholder = { Text(stringResource(R.string.bilibili_danmaku_link_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SettingsPalette.AccentText,
                        unfocusedBorderColor = SettingsPalette.Divider,
                        cursorColor = SettingsPalette.AccentText,
                        focusedLabelColor = SettingsPalette.SecondaryText,
                        unfocusedLabelColor = SettingsPalette.SecondaryText,
                        focusedTextColor = SettingsPalette.PrimaryText,
                        unfocusedTextColor = SettingsPalette.PrimaryText,
                        focusedPlaceholderColor = SettingsPalette.TertiaryText,
                        unfocusedPlaceholderColor = SettingsPalette.TertiaryText,
                        focusedContainerColor = SettingsPalette.DialogSurface,
                        unfocusedContainerColor = SettingsPalette.DialogSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.bilibili_danmaku_download_mode), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SettingsPalette.PrimaryText)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { downloadWholeSeason = true }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = downloadWholeSeason,
                        onClick = { downloadWholeSeason = true },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                    Text(stringResource(R.string.bilibili_danmaku_season_download), modifier = Modifier.padding(start = 8.dp), color = SettingsPalette.PrimaryText)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { downloadWholeSeason = false }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = !downloadWholeSeason,
                        onClick = { downloadWholeSeason = false },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                    Text(stringResource(R.string.bilibili_danmaku_single_download), modifier = Modifier.padding(start = 8.dp), color = SettingsPalette.PrimaryText)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onDownload(url.trim(), downloadWholeSeason)
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Text(stringResource(R.string.bilibili_danmaku_download_btn), color = SettingsPalette.AccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = SettingsPalette.SecondaryText)
            }
        },
        containerColor = SettingsPalette.DialogSurface
    )
}

@Composable
private fun DownloadProgressDialog(
    progress: DownloadProgress,
    isDownloading: Boolean,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isCompleted = !isDownloading && progress.total > 0 && progress.current >= progress.total
    
    AlertDialog(
        onDismissRequest = { 
            if (!isDownloading) {
                onDismiss()
            }
        }, // 下载过程中不允许手动关闭，完成后可以关闭
        title = { 
            Text(
                stringResource(R.string.bilibili_danmaku_downloading), 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Bold,
                color = SettingsPalette.PrimaryText
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // 当前进度
                if (progress.total > 0) {
                    Text(
                        text = stringResource(R.string.bilibili_danmaku_progress, progress.current, progress.total),
                        fontSize = 14.sp,
                        color = SettingsPalette.SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 进度条
                    LinearProgressIndicator(
                        progress = progress.current.toFloat() / progress.total.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = primaryColor,
                        trackColor = SettingsPalette.Divider,
                    )
                } else {
                    // 准备中
                    Text(
                        text = stringResource(R.string.bilibili_danmaku_preparing),
                        fontSize = 14.sp,
                        color = SettingsPalette.SecondaryText
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = primaryColor,
                        trackColor = SettingsPalette.Divider,
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 当前下载的文件
                if (progress.currentTitle.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.bilibili_danmaku_current, progress.currentTitle),
                        fontSize = 13.sp,
                        color = SettingsPalette.SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 成功和失败数量
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.bilibili_danmaku_success_count, progress.successCount),
                            fontSize = 13.sp,
                            color = SettingsPalette.SecondaryText
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.bilibili_danmaku_failed_count, progress.failedCount),
                            fontSize = 13.sp,
                            color = SettingsPalette.SecondaryText
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isCompleted) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.bilibili_danmaku_done), color = SettingsPalette.AccentText, fontSize = 14.sp)
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SettingsPalette.DialogSurface
    )
}

