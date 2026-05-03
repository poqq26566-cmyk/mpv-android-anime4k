package com.fam4k007.videoplayer.compose

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.VideoFileParcelable
import com.fam4k007.videoplayer.mediainfo.MediaInfoActivity
import com.fam4k007.videoplayer.utils.ThumbnailCacheManager
import com.fam4k007.videoplayer.utils.FileOperationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoListScreen(
    folderName: String,
    initialVideos: List<VideoFileParcelable>,
    onNavigateBack: () -> Unit,
    onOpenVideo: (VideoFileParcelable, Int, List<VideoFileParcelable>) -> Unit,
    onRescanFolder: ((List<VideoFileParcelable>) -> Unit) -> Unit,
    preferencesManager: com.fam4k007.videoplayer.manager.PreferencesManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var videos by remember { mutableStateOf(initialVideos) }
    var filteredVideos by remember { mutableStateOf(initialVideos) }
    var sortType by remember { mutableStateOf(preferencesManager.getVideoSortType()) }
    var sortOrder by remember { mutableStateOf(preferencesManager.getVideoSortOrder()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoFileParcelable?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // 文件操作菜单相关状态
    var selectedVideoForOperation by remember { mutableStateOf<VideoFileParcelable?>(null) }
    var showOperationMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    
    // 多选编辑模式
    var isEditMode by remember { mutableStateOf(false) }
    var selectedVideos by remember { mutableStateOf<Set<VideoFileParcelable>>(emptySet()) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    
    fun refreshVideos() {
        isRefreshing = true
        onRescanFolder { newVideos ->
            val sorted = sortVideos(newVideos, sortType, sortOrder)
            videos = sorted
            filteredVideos = filterVideos(sorted, searchQuery)
            isRefreshing = false
        }
    }

    LaunchedEffect(sortType, sortOrder) {
        videos = sortVideos(videos, sortType, sortOrder)
        filteredVideos = filterVideos(videos, searchQuery)
    }

    LaunchedEffect(searchQuery) {
        filteredVideos = filterVideos(videos, searchQuery)
    }

    Scaffold(
        topBar = {
            if (showSearch) {
                SearchTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onCloseSearch = {
                        showSearch = false
                        searchQuery = ""
                    }
                )
            } else {
                ImmersiveTopAppBar(
                    title = {
                        Text(
                            folderName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            isEditMode = !isEditMode
                            if (!isEditMode) {
                                selectedVideos = emptySet()
                            }
                        }) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = if (isEditMode) "退出编辑" else "编辑",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "排序",
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (filteredVideos.isEmpty()) {
                EmptyState(if (searchQuery.isEmpty()) "此文件夹中没有视频" else "未找到匹配的视频")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        if (isRefreshing) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    itemsIndexed(filteredVideos) { index, video ->
                        VideoItem(
                            video = video,
                            isSelected = video == selectedVideoForOperation,
                            isEditMode = isEditMode,
                            isChecked = selectedVideos.contains(video),
                            onClick = { 
                                if (isEditMode) {
                                    // 编辑模式下切换选中状态
                                    selectedVideos = if (selectedVideos.contains(video)) {
                                        selectedVideos - video
                                    } else {
                                        selectedVideos + video
                                    }
                                } else {
                                    // 正常模式下打开视频
                                    onOpenVideo(video, index, filteredVideos)
                                }
                            },
                            onMoreClick = { 
                                if (!isEditMode) selectedVideo = video 
                            },
                            onLongClick = {
                                if (!isEditMode) {
                                    selectedVideoForOperation = video
                                    showOperationMenu = true
                                }
                            }
                        )
                    }
                }
                
                // 文件操作菜单（全屏显示）
                FileOperationMenu(
                    visible = showOperationMenu && selectedVideoForOperation != null && !isEditMode,
                    fileName = selectedVideoForOperation?.name ?: "",
                    onDismiss = { 
                        showOperationMenu = false
                        selectedVideoForOperation = null
                    },
                    onRename = { 
                        showRenameDialog = true
                        showOperationMenu = false  // 关闭菜单
                    },
                    onDelete = { 
                        showDeleteDialog = true
                        showOperationMenu = false  // 关闭菜单
                    },
                    onCopy = { 
                        showCopyDialog = true
                        showOperationMenu = false  // 关闭菜单
                    }
                )
                
                // 多选操作栏
                MultiSelectActionBar(
                    visible = isEditMode && selectedVideos.isNotEmpty(),
                    selectedCount = selectedVideos.size,
                    totalCount = filteredVideos.size,
                    onSelectAll = {
                        if (selectedVideos.size == filteredVideos.size) {
                            // 取消全选
                            selectedVideos = emptySet()
                        } else {
                            // 全选
                            selectedVideos = filteredVideos.toSet()
                        }
                    },
                    onRename = {
                        if (selectedVideos.size == 1) {
                            selectedVideoForOperation = selectedVideos.first()
                            showRenameDialog = true
                        }
                    },
                    onDelete = {
                        showBatchDeleteDialog = true
                    },
                    onCopy = {
                        showCopyDialog = true
                    },
                    onCancel = {
                        isEditMode = false
                        selectedVideos = emptySet()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                )
                
                // 刷新按钮（编辑模式时隐藏）
                AnimatedVisibility(
                    visible = !isEditMode,
                    enter = scaleIn(
                        initialScale = 0.3f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = scaleOut(
                        targetScale = 0.3f,
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(200)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { refreshVideos() }
                    ) {
                        Icon(Icons.Default.Refresh, "刷新", tint = Color.White)
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        VideoSortDialog(
            currentSortType = sortType,
            currentSortOrder = sortOrder,
            onDismiss = { showSortDialog = false },
            onSortSelected = { newType, newOrder ->
                sortType = newType
                sortOrder = newOrder
                preferencesManager.setVideoSortType(newType)
                preferencesManager.setVideoSortOrder(newOrder)
                showSortDialog = false
            }
        )
    }

    // 监听selectedVideo变化，启动MediaInfoActivity
    LaunchedEffect(selectedVideo) {
        selectedVideo?.let { video ->
            MediaInfoActivity.start(context, video.uri, video.name)
            selectedVideo = null // 重置状态
        }
    }
    
    // 重命名对话框
    if (showRenameDialog && selectedVideoForOperation != null) {
        RenameDialog(
            visible = true,
            currentName = selectedVideoForOperation!!.name,
            onDismiss = { 
                showRenameDialog = false
                selectedVideoForOperation = null
            },
            onConfirm = { newName ->
                lifecycleOwner.lifecycleScope.launch {
                    val videoPath = selectedVideoForOperation!!.path
                    val newPath = FileOperationManager.rename(context, videoPath, newName)
                    if (newPath != null) {
                        // 刷新列表
                        refreshVideos()
                    }
                    showRenameDialog = false
                    selectedVideoForOperation = null
                    // 退出编辑模式
                    if (isEditMode) {
                        selectedVideos = emptySet()
                        isEditMode = false
                    }
                }
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog && selectedVideoForOperation != null) {
        DeleteConfirmDialog(
            visible = true,
            fileName = selectedVideoForOperation!!.name,
            isFolder = false,
            onDismiss = { 
                showDeleteDialog = false
                selectedVideoForOperation = null
            },
            onConfirm = {
                lifecycleOwner.lifecycleScope.launch {
                    val videoPath = selectedVideoForOperation!!.path
                    val success = FileOperationManager.delete(
                        context,
                        videoPath,
                        isFolder = false
                    )
                    if (success) {
                        // 刷新列表
                        refreshVideos()
                    }
                    showDeleteDialog = false
                    selectedVideoForOperation = null
                }
            }
        )
    }
    
    // 复制对话框
    if (showCopyDialog && selectedVideoForOperation != null) {
        CopyDestinationDialog(
            visible = true,
            fileName = selectedVideoForOperation!!.name,
            onDismiss = { 
                showCopyDialog = false
                selectedVideoForOperation = null
            },
            onConfirm = { destPath ->
                lifecycleOwner.lifecycleScope.launch {
                    val sourcePath = selectedVideoForOperation!!.path
                    val sourceFile = File(sourcePath)
                    val fullDestPath = File(destPath, sourceFile.name).absolutePath
                    val success = FileOperationManager.copy(
                        context,
                        sourcePath,
                        fullDestPath,
                        isFolder = false
                    )
                    if (success) {
                        // 刷新列表
                        refreshVideos()
                    }
                    showCopyDialog = false
                    selectedVideoForOperation = null
                }
            }
        )
    }
    
    // 批量删除确认对话框
    if (showBatchDeleteDialog && selectedVideos.isNotEmpty()) {
        BatchDeleteConfirmDialog(
            visible = true,
            count = selectedVideos.size,
            isFolder = false,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = {
                lifecycleOwner.lifecycleScope.launch {
                    selectedVideos.forEach { video ->
                        FileOperationManager.delete(
                            context,
                            video.path,
                            isFolder = false
                        )
                    }
                    // 刷新列表
                    refreshVideos()
                    selectedVideos = emptySet()
                    isEditMode = false
                    showBatchDeleteDialog = false
                }
            }
        )
    }
    
    // 多选复制对话框
    if (showCopyDialog && (selectedVideos.isNotEmpty() || selectedVideoForOperation != null)) {
        val fileName = when {
            selectedVideos.size == 1 -> selectedVideos.first().name
            selectedVideos.size > 1 -> "${selectedVideos.size} 个文件"
            else -> selectedVideoForOperation?.name ?: ""
        }
        
        CopyDestinationDialog(
            visible = true,
            fileName = fileName,
            onDismiss = { 
                showCopyDialog = false
                selectedVideoForOperation = null
            },
            onConfirm = { destPath ->
                lifecycleOwner.lifecycleScope.launch {
                    val videosToCode = if (selectedVideos.isNotEmpty()) {
                        selectedVideos.toList()
                    } else {
                        listOfNotNull(selectedVideoForOperation)
                    }
                    
                    videosToCode.forEach { video ->
                        val sourcePath = video.path
                        val sourceFile = File(sourcePath)
                        val fullDestPath = File(destPath, sourceFile.name).absolutePath
                        FileOperationManager.copy(
                            context,
                            sourcePath,
                            fullDestPath,
                            isFolder = false
                        )
                    }
                    
                    // 刷新列表
                    refreshVideos()
                    showCopyDialog = false
                    selectedVideoForOperation = null
                    if (isEditMode) {
                        selectedVideos = emptySet()
                        isEditMode = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoItem(
    video: VideoFileParcelable,
    isSelected: Boolean,
    isEditMode: Boolean,
    isChecked: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var thumbnailBitmap by remember(video.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(video.uri) {
        thumbnailBitmap = null // 立即清空旧缩略图
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cacheManager = ThumbnailCacheManager.getInstance(context)
                val bitmap = cacheManager.getThumbnail(context, Uri.parse(video.uri), video.duration)
                withContext(Dispatchers.Main) {
                    thumbnailBitmap = bitmap
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && !isEditMode) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
            else 
                Color.White
        ),
        border = if (isSelected && !isEditMode) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 复选框（编辑模式）
            AnimatedVisibility(
                visible = isEditMode,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = null,  // 点击整行触发
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // 缩略图
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    AsyncImage(
                        model = thumbnailBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 视频信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 标题（顶部对齐）
                Text(
                    text = video.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF212121),
                    lineHeight = 16.sp
                )

                // 时长和大小标签（底部对齐）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = formatFileSize(video.size),
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }

            // 更多按钮（非编辑模式）
            if (!isEditMode) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多信息",
                        tint = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    ImmersiveTopAppBar(
        title = {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = Color.White
                ),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "搜索视频...",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                },
                singleLine = true
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
        },
        actions = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = Color.White
                    )
                }
            }
        }
    )
}

@Composable
private fun VideoSortDialog(
    currentSortType: String,
    currentSortOrder: String,
    onDismiss: () -> Unit,
    onSortSelected: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("排序方式", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                SortOption(
                    text = "名称 (升序)",
                    isSelected = currentSortType == "NAME" && currentSortOrder == "ASCENDING",
                    onClick = { onSortSelected("NAME", "ASCENDING") }
                )
                SortOption(
                    text = "名称 (降序)",
                    isSelected = currentSortType == "NAME" && currentSortOrder == "DESCENDING",
                    onClick = { onSortSelected("NAME", "DESCENDING") }
                )
                SortOption(
                    text = "日期 (升序)",
                    isSelected = currentSortType == "DATE" && currentSortOrder == "ASCENDING",
                    onClick = { onSortSelected("DATE", "ASCENDING") }
                )
                SortOption(
                    text = "日期 (降序)",
                    isSelected = currentSortType == "DATE" && currentSortOrder == "DESCENDING",
                    onClick = { onSortSelected("DATE", "DESCENDING") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SortOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF212121)
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFBDBDBD)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

private fun sortVideos(
    videos: List<VideoFileParcelable>,
    sortType: String,
    sortOrder: String
): List<VideoFileParcelable> {
    return when (sortType) {
        "NAME" -> {
            if (sortOrder == "ASCENDING") {
                videos.sortedWith(naturalComparator { it.name })
            } else {
                videos.sortedWith(naturalComparator<VideoFileParcelable> { it.name }.reversed())
            }
        }
        "DATE" -> {
            if (sortOrder == "ASCENDING") {
                videos.sortedBy { it.dateAdded }
            } else {
                videos.sortedByDescending { it.dateAdded }
            }
        }
        else -> videos
    }
}

/**
 * 自然排序比较器 - 支持字符串中数字的正确排序
 * 例如：1, 2, 3, 10, 11, 12 而不是 1, 10, 11, 12, 2, 3
 */
private fun <T> naturalComparator(selector: (T) -> String): Comparator<T> {
    return Comparator { a, b ->
        compareNatural(selector(a), selector(b))
    }
}

/**
 * 自然排序字符串比较
 */
private fun compareNatural(str1: String, str2: String): Int {
    val s1 = str1.lowercase()
    val s2 = str2.lowercase()
    
    var i1 = 0
    var i2 = 0
    
    while (i1 < s1.length && i2 < s2.length) {
        val c1 = s1[i1]
        val c2 = s2[i2]
        
        // 如果两个字符都是数字，则提取完整的数字进行比较
        if (c1.isDigit() && c2.isDigit()) {
            // 提取第一个数字
            var num1 = 0
            while (i1 < s1.length && s1[i1].isDigit()) {
                num1 = num1 * 10 + (s1[i1] - '0')
                i1++
            }
            
            // 提取第二个数字
            var num2 = 0
            while (i2 < s2.length && s2[i2].isDigit()) {
                num2 = num2 * 10 + (s2[i2] - '0')
                i2++
            }
            
            // 比较数字大小
            if (num1 != num2) {
                return num1 - num2
            }
        } else {
            // 普通字符比较
            if (c1 != c2) {
                return c1 - c2
            }
            i1++
            i2++
        }
    }
    
    // 如果一个字符串是另一个的前缀，则较短的排在前面
    return s1.length - s2.length
}

private fun filterVideos(
    videos: List<VideoFileParcelable>,
    query: String
): List<VideoFileParcelable> {
    if (query.isEmpty()) return videos
    val lowerQuery = query.lowercase()
    return videos.filter { it.name.lowercase().contains(lowerQuery) }
}

private fun formatDuration(milliseconds: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
