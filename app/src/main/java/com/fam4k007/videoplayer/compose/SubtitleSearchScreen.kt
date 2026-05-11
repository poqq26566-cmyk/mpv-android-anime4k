package com.fam4k007.videoplayer.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fam4k007.videoplayer.subtitle.SubtitleInfo
import com.fam4k007.videoplayer.subtitle.SubtitleLanguages
import com.fam4k007.videoplayer.subtitle.SubtitleSources
import com.fam4k007.videoplayer.subtitle.SubtitleFormats
import com.fam4k007.videoplayer.subtitle.SearchOptions
import com.fam4k007.videoplayer.subtitle.TmdbMediaResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSearchScreen(
    savedFolderUri: Uri?,
    mediaResults: List<TmdbMediaResult>,
    searchResults: List<SubtitleInfo>,
    isSearchingMedia: Boolean,
    isSearching: Boolean,
    searchOptions: SearchOptions,
    selectedMedia: TmdbMediaResult?,
    onBack: () -> Unit,
    onFolderSelected: (Uri) -> Unit,
    onSearchOptionsChanged: (SearchOptions) -> Unit,
    onSearchMedia: (String) -> Unit,
    onSelectMedia: (TmdbMediaResult) -> Unit,
    onDownload: (SubtitleInfo) -> Unit,
    onClearSelection: () -> Unit
) {
    var currentFolderUri by remember { mutableStateOf(savedFolderUri) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

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
            TopAppBar(
                title = { Text("字幕搜索下载", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
        // 顶部搜索区域
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // 主搜索卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                    // 搜索字幕 - 独占一行
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentFolderUri != null) primaryColor else primaryColor.copy(alpha = 0.3f))
                                .clickable(enabled = currentFolderUri != null) {
                                    showSearchDialog = true
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "搜索字幕",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = if (currentFolderUri != null) 
                                        "点击输入影片名称" 
                                    else 
                                        "请先设置保存文件夹",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 附属功能按钮行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                    // 保存文件夹按钮
                            OutlinedButton(
                                onClick = { folderPickerLauncher.launch(currentFolderUri) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = primaryColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentFolderUri != null) "已设置" else "设置文件夹",
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 搜索选项按钮
                            OutlinedButton(
                                onClick = { showOptionsDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = primaryColor
                                ),
                                enabled = currentFolderUri != null
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "搜索选项",
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    // 媒体搜索中
                    isSearchingMedia -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = primaryColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("搜索影片中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // 显示媒体选择列表
                    mediaResults.isNotEmpty() && selectedMedia == null -> {
                        MediaResultList(
                            mediaList = mediaResults,
                            onSelectMedia = onSelectMedia
                        )
                    }
                    // 已选择媒体，显示字幕搜索结果
                    selectedMedia != null -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                        // 已选择的媒体信息
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = primaryColor.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "已选择",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = selectedMedia.displayTitle,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                    }
                                    IconButton(onClick = onClearSelection) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "取消选择",
                                            tint = primaryColor
                                        )
                                    }
                                }
                            }

                            // 字幕搜索结果
                            if (isSearching) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = primaryColor)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("搜索字幕中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else if (searchResults.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "未找到字幕",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp
                                    )
                                }
                            } else {
                                SubtitleResultList(
                                    subtitles = searchResults,
                                    onDownload = onDownload
                                )
                            }
                        }
                    }
                    // 空状态
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "请开始搜索",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

// 搜索对话框
    if (showSearchDialog) {
        SubtitleSearchDialog(
            onDismiss = { showSearchDialog = false },
            onSearch = { query ->
                showSearchDialog = false
                onSearchMedia(query)
            }
        )
    }

// 搜索选项对话框
    if (showOptionsDialog) {
        SearchOptionsDialog(
            currentOptions = searchOptions,
            onDismiss = { showOptionsDialog = false },
            onConfirm = { newOptions ->
                showOptionsDialog = false
                onSearchOptionsChanged(newOptions)
            }
        )
    }
}

@Composable
private fun MediaResultList(
    mediaList: List<TmdbMediaResult>,
    onSelectMedia: (TmdbMediaResult) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Text(
                text = "选择影片 (${mediaList.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(mediaList) { media ->
            MediaItem(
                media = media,
                onClick = { onSelectMedia(media) }
            )
        }
    }
}

@Composable
private fun MediaItem(
    media: TmdbMediaResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标
            Icon(
                imageVector = if (media.mediaType == "tv") Icons.Default.Tv else Icons.Default.Movie,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.displayTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                media.overview?.let { overview ->
                    Text(
                        text = overview,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubtitleResultList(
    subtitles: List<SubtitleInfo>,
    onDownload: (SubtitleInfo) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Text(
                text = "字幕结果 (${subtitles.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(subtitles) { subtitle ->
            SubtitleItem(
                subtitle = subtitle,
                onDownload = { onDownload(subtitle) }
            )
        }
    }
}

@Composable
private fun SubtitleItem(
    subtitle: SubtitleInfo,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDownload),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subtitle.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { 
                            Text(
                                subtitle.displayLanguage, 
                                fontSize = 12.sp, 
                                color = androidx.compose.ui.graphics.Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        modifier = Modifier
                            .height(28.dp)
                            .weight(1f, fill = false)
                    )
                    subtitle.format?.let {
                        AssistChip(
                            onClick = {},
                            label = { 
                                Text(
                                    it.uppercase(), 
                                    fontSize = 12.sp, 
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                ) 
                            },
                            modifier = Modifier
                                .height(28.dp)
                                .weight(1f, fill = false)
                        )
                    }
                    subtitle.source?.let {
                        AssistChip(
                            onClick = {},
                            label = { 
                                Text(
                                    it, 
                                    fontSize = 12.sp, 
                                    color = androidx.compose.ui.graphics.Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                ) 
                            },
                            modifier = Modifier
                                .height(28.dp)
                                .weight(1f, fill = false)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "下载",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SubtitleSearchDialog(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                "搜索字幕",
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "请输入影片名称",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("影片名称") },
                    placeholder =  { Text("例如：疾速追杀") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        focusedTextColor = androidx.compose.ui.graphics.Color.Black,
                        unfocusedTextColor = androidx.compose.ui.graphics.Color.Black
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        onSearch(searchQuery.trim())
                    }
                },
                enabled = searchQuery.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {
                Text("搜索")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = primaryColor
                )
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SearchOptionsDialog(
    currentOptions: SearchOptions,
    onDismiss: () -> Unit,
    onConfirm: (SearchOptions) -> Unit
) {
    var selectedLanguages by remember { mutableStateOf(currentOptions.languages) }
    var selectedSources by remember { mutableStateOf(currentOptions.sources) }
    var selectedFormats by remember { mutableStateOf(currentOptions.formats) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "搜索选项",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 语言选项
                    item {
                        Text(
                            "字幕语言",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SubtitleLanguages.ALL.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLanguages = if (code == "all") {
                                            setOf("all")
                                        } else {
                                            val newSet = selectedLanguages.toMutableSet()
                                            newSet.remove("all")
                                            if (code in newSet) newSet.remove(code) else newSet.add(code)
                                            if (newSet.isEmpty()) setOf("all") else newSet
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = code in selectedLanguages,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = primaryColor
                                    )
                                )
                                Text(
                                    name,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 来源选项
                    item {
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "字幕来源",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SubtitleSources.ALL.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSources = if (code == "all") {
                                            setOf("all")
                                        } else {
                                            val newSet = selectedSources.toMutableSet()
                                            newSet.remove("all")
                                            if (code in newSet) newSet.remove(code) else newSet.add(code)
                                            if (newSet.isEmpty()) setOf("all") else newSet
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = code in selectedSources,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = primaryColor
                                    )
                                )
                                Text(
                                    name,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 格式选项
                    item {
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "字幕格式",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SubtitleFormats.ALL.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = selectedFormats.toMutableSet()
                                        if (code in newSet) newSet.remove(code) else newSet.add(code)
                                        if (newSet.isNotEmpty()) {
                                            selectedFormats = newSet
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = code in selectedFormats,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = primaryColor
                                    )
                                )
                                Text(
                                    name,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = primaryColor
                        )
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                SearchOptions(
                                    languages = selectedLanguages,
                                    sources = selectedSources,
                                    formats = selectedFormats
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
