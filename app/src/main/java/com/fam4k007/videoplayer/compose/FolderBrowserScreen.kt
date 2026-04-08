package com.fam4k007.videoplayer.compose

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.R
import androidx.compose.ui.res.stringResource
import com.fam4k007.videoplayer.VideoFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onScanVideos: ((List<VideoFolder>) -> Unit) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenFolder: (VideoFolder) -> Unit,
    preferencesManager: com.fam4k007.videoplayer.manager.PreferencesManager
) {
    var folders by remember { mutableStateOf<List<VideoFolder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(preferencesManager.getFolderSortType()) }
    var sortOrder by remember { mutableStateOf(preferencesManager.getFolderSortOrder()) }
    var showSortDialog by remember { mutableStateOf(false) }
    
    fun refreshFolders() {
        isRefreshing = true
        onScanVideos { scannedFolders ->
            folders = sortFolders(scannedFolders, sortType, sortOrder)
            isRefreshing = false
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            onScanVideos { scannedFolders ->
                folders = sortFolders(scannedFolders, sortType, sortOrder)
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.folder_browser_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                    if (hasPermission) {
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = stringResource(R.string.folder_sort),
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !hasPermission -> {
                    PermissionPrompt(onRequestPermission)
                }
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                folders.isEmpty() -> {
                    EmptyState(stringResource(R.string.folder_no_videos))
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
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
                            items(folders) { folder ->
                                FolderItem(
                                    folder = folder,
                                    onClick = { onOpenFolder(folder) }
                                )
                            }
                        }
                        
                        // 添加刷新按钮
                        FloatingActionButton(
                            onClick = { refreshFolders() },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, stringResource(R.string.common_refresh), tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        SortDialog(
            currentSortType = sortType,
            currentSortOrder = sortOrder,
            onDismiss = { showSortDialog = false },
            onSortSelected = { newType, newOrder ->
                sortType = newType
                sortOrder = newOrder
                preferencesManager.setFolderSortType(newType)
                preferencesManager.setFolderSortOrder(newOrder)
                folders = sortFolders(folders, newType, newOrder)
                showSortDialog = false
            }
        )
    }
}

@Composable
private fun FolderItem(
    folder: VideoFolder,
    onClick: () -> Unit
) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件夹图标
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 文件夹信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = folder.folderName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.folder_videos_count, folder.videoCount),
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFBDBDBD)
            )
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFBDBDBD)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.folder_permission_needed),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.folder_permission_desc),
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.folder_grant_permission))
            }
        }
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
                imageVector = Icons.Default.FolderOff,
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

@Composable
private fun SortDialog(
    currentSortType: String,
    currentSortOrder: String,
    onDismiss: () -> Unit,
    onSortSelected: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.folder_sort_dialog_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                SortOption(
                    text = stringResource(R.string.folder_sort_name_asc),
                    isSelected = currentSortType == "NAME" && currentSortOrder == "ASCENDING",
                    onClick = { onSortSelected("NAME", "ASCENDING") }
                )
                SortOption(
                    text = stringResource(R.string.folder_sort_name_desc),
                    isSelected = currentSortType == "NAME" && currentSortOrder == "DESCENDING",
                    onClick = { onSortSelected("NAME", "DESCENDING") }
                )
                SortOption(
                    text = stringResource(R.string.folder_sort_video_count_asc),
                    isSelected = currentSortType == "VIDEO_COUNT" && currentSortOrder == "ASCENDING",
                    onClick = { onSortSelected("VIDEO_COUNT", "ASCENDING") }
                )
                SortOption(
                    text = stringResource(R.string.folder_sort_video_count_desc),
                    isSelected = currentSortType == "VIDEO_COUNT" && currentSortOrder == "DESCENDING",
                    onClick = { onSortSelected("VIDEO_COUNT", "DESCENDING") }
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

private fun sortFolders(
    folders: List<VideoFolder>,
    sortType: String,
    sortOrder: String
): List<VideoFolder> {
    return when (sortType) {
        "NAME" -> {
            if (sortOrder == "ASCENDING") {
                folders.sortedWith(naturalComparator { it.folderName })
            } else {
                folders.sortedWith(naturalComparator<VideoFolder> { it.folderName }.reversed())
            }
        }
        "VIDEO_COUNT" -> {
            if (sortOrder == "ASCENDING") {
                folders.sortedBy { it.videoCount }
            } else {
                folders.sortedByDescending { it.videoCount }
            }
        }
        else -> folders
    }
}

/**
 * 自然排序比较器 - 支持字符串中数字的正确排序
 */
private fun <T> naturalComparator(selector: (T) -> String): Comparator<T> {
    return Comparator { a, b ->
        compareNatural(selector(a), selector(b))
    }
}

private fun compareNatural(str1: String, str2: String): Int {
    val s1 = str1.lowercase()
    val s2 = str2.lowercase()
    
    var i1 = 0
    var i2 = 0
    
    while (i1 < s1.length && i2 < s2.length) {
        val c1 = s1[i1]
        val c2 = s2[i2]
        
        if (c1.isDigit() && c2.isDigit()) {
            var num1 = 0
            while (i1 < s1.length && s1[i1].isDigit()) {
                num1 = num1 * 10 + (s1[i1] - '0')
                i1++
            }
            
            var num2 = 0
            while (i2 < s2.length && s2[i2].isDigit()) {
                num2 = num2 * 10 + (s2[i2] - '0')
                i2++
            }
            
            if (num1 != num2) {
                return num1 - num2
            }
        } else {
            if (c1 != c2) {
                return c1 - c2
            }
            i1++
            i2++
        }
    }
    
    return s1.length - s2.length
}
