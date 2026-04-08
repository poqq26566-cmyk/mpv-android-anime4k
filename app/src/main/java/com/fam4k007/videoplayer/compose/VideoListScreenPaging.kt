package com.fam4k007.videoplayer.compose

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.VideoFileParcelable
import com.fam4k007.videoplayer.database.VideoDatabase
import com.fam4k007.videoplayer.paging.VideoPagingSource
import com.fam4k007.videoplayer.utils.ThumbnailCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 使用Paging3的视频列表界面
 * 防止大量视频导致OOM
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreenPaging(
    folderName: String,
    folderPath: String,
    onNavigateBack: () -> Unit,
    onOpenVideo: (VideoFileParcelable, List<VideoFileParcelable>) -> Unit,
    onRescanFolder: (() -> Unit) -> Unit,
    preferencesManager: com.fam4k007.videoplayer.manager.PreferencesManager,
    coroutineScope: CoroutineScope
) {
    val context = LocalContext.current
    
    // 排序状态
    var sortType by remember { mutableStateOf(preferencesManager.getVideoSortType()) }
    var sortOrder by remember { mutableStateOf(preferencesManager.getVideoSortOrder()) }
    
    // Paging数据流 - 依赖于sortType和sortOrder
    val pager = remember(folderPath, sortType, sortOrder) {
        Pager(
            config = PagingConfig(
                pageSize = VideoPagingSource.PAGE_SIZE,
                prefetchDistance = 10,
                enablePlaceholders = false,
                initialLoadSize = VideoPagingSource.PAGE_SIZE
            ),
            pagingSourceFactory = {
                VideoPagingSource(
                    dao = VideoDatabase.getDatabase(context).videoCacheDao(),
                    folderPath = folderPath,
                    sortType = sortType,
                    sortOrder = sortOrder
                )
            }
        ).flow.cachedIn(coroutineScope)
    }
    
    val lazyPagingItems = pager.collectAsLazyPagingItems()
    
    var showSearch by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoFileParcelable?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 当需要刷新时
    fun refreshVideos() {
        isRefreshing = true
        onRescanFolder {
            lazyPagingItems.refresh()
            isRefreshing = false
        }
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
                TopAppBar(
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
                                contentDescription = stringResource(R.string.common_back),
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.common_search),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = stringResource(R.string.video_sort),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // 根据搜索关键词过滤显示
            val filteredItems = if (searchQuery.isEmpty()) {
                lazyPagingItems
            } else {
                // 搜索时需要过滤，但Paging不支持直接过滤，这里显示提示
                null
            }
            
            if (filteredItems == null || (searchQuery.isNotEmpty() && lazyPagingItems.itemCount == 0)) {
                EmptyState(context.getString(R.string.paging_search_hint))
            } else if (lazyPagingItems.itemCount == 0) {
                EmptyState(context.getString(R.string.video_list_no_videos))
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
                    
                    // 使用Paging3的items扩展
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { it.uri }
                    ) { index ->
                        val video = lazyPagingItems[index]
                        if (video != null) {
                            // 过滤搜索关键词
                            if (searchQuery.isEmpty() || video.name.contains(searchQuery, ignoreCase = true)) {
                                VideoItem(
                                    video = video,
                                    onClick = { 
                                        // 从数据库查询该文件夹的所有视频（使用当前排序方式）
                                        coroutineScope.launch {
                                            val allVideos = withContext(Dispatchers.IO) {
                                                val dao = VideoDatabase.getDatabase(context).videoCacheDao()
                                                val entities = when {
                                                    sortType == "NAME" && sortOrder == "ASCENDING" -> 
                                                        dao.getVideosByFolderSortedByNameAsc(folderPath)
                                                    sortType == "NAME" && sortOrder == "DESCENDING" -> 
                                                        dao.getVideosByFolderSortedByNameDesc(folderPath)
                                                    sortType == "DATE" && sortOrder == "ASCENDING" -> 
                                                        dao.getVideosByFolderSortedByDateAsc(folderPath)
                                                    sortType == "DATE" && sortOrder == "DESCENDING" -> 
                                                        dao.getVideosByFolderSortedByDateDesc(folderPath)
                                                    else -> 
                                                        dao.getVideosByFolderSortedByNameAsc(folderPath)
                                                }
                                                entities.map { entity ->
                                                    VideoFileParcelable(
                                                        uri = entity.uri,
                                                        name = entity.name,
                                                        path = entity.path,
                                                        size = entity.size,
                                                        duration = entity.duration,
                                                        dateAdded = entity.dateAdded
                                                    )
                                                }
                                            }
                                            onOpenVideo(video, allVideos)
                                        }
                                    },
                                    onMoreClick = { selectedVideo = video }
                                )
                            }
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { refreshVideos() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.common_refresh), tint = Color.White)
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

    AnimatedVisibility(
        visible = selectedVideo != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedVideo?.let { video ->
            VideoInfoDialog(
                video = video,
                onDismiss = { selectedVideo = null }
            )
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
    TopAppBar(
        title = {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp
                ),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            stringResource(R.string.video_list_search_hint),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 18.sp
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
                    contentDescription = stringResource(R.string.content_desc_close_search),
                    tint = Color.White
                )
            }
        },
        actions = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.video_list_clear),
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun VideoItem(
    video: VideoFileParcelable,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(video.uri) {
        withContext(Dispatchers.IO) {
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatFileSize(video.size),
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = formatDuration(video.duration),
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.content_desc_more),
                    tint = Color(0xFF757575)
                )
            }
        }
    }
}

@Composable
private fun VideoInfoDialog(
    video: VideoFileParcelable,
    onDismiss: () -> Unit
) {
    // 简化版对话框，不加载元数据
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = video.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(stringResource(R.string.video_file_size), formatFileSize(video.size))
                InfoRow(stringResource(R.string.video_duration), formatDuration(video.duration))
                InfoRow(stringResource(R.string.video_path), video.path)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF757575),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF212121),
            modifier = Modifier.weight(0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
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
            Text(stringResource(R.string.video_sort_by), fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                SortOption(
                    text = stringResource(R.string.video_list_name_asc),
                    isSelected = currentSortType == "NAME" && currentSortOrder == "ASCENDING",
                    onClick = { onSortSelected("NAME", "ASCENDING") }
                )
                SortOption(
                    text = stringResource(R.string.video_list_name_desc),
                    isSelected = currentSortType == "NAME" && currentSortOrder == "DESCENDING",
                    onClick = { onSortSelected("NAME", "DESCENDING") }
                )
                SortOption(
                    text = stringResource(R.string.video_list_date_asc),
                    isSelected = currentSortType == "DATE" && currentSortOrder == "ASCENDING",
                    onClick = { onSortSelected("DATE", "ASCENDING") }
                )
                SortOption(
                    text = stringResource(R.string.video_list_date_desc),
                    isSelected = currentSortType == "DATE" && currentSortOrder == "DESCENDING",
                    onClick = { onSortSelected("DATE", "DESCENDING") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
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
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF212121)
        )
    }
}
