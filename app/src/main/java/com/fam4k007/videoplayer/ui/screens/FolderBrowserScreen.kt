package com.fam4k007.videoplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.VideoFolder
import com.fam4k007.videoplayer.preferences.PreferencesManager
import com.fam4k007.videoplayer.ui.components.BatchDeleteConfirmDialog
import com.fam4k007.videoplayer.ui.components.DeleteConfirmDialog
import com.fam4k007.videoplayer.ui.components.FileOperationMenu
import com.fam4k007.videoplayer.ui.components.MultiSelectActionBar
import com.fam4k007.videoplayer.ui.components.RenameDialog
import com.fam4k007.videoplayer.utils.FileOperationManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderBrowserScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onScanVideos: ((List<VideoFolder>) -> Unit) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenFolder: (VideoFolder) -> Unit,
    preferencesManager: PreferencesManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var folders by remember { mutableStateOf<List<VideoFolder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(preferencesManager.getFolderSortType()) }
    var sortOrder by remember { mutableStateOf(preferencesManager.getFolderSortOrder()) }
    var showSortDialog by remember { mutableStateOf(false) }
    
    // 文件操作菜单相关状态
    var selectedFolder by remember { mutableStateOf<VideoFolder?>(null) }
    var showOperationMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // 多选编辑模式
    var isEditMode by remember { mutableStateOf(false) }
    var selectedFolders by remember { mutableStateOf<Set<VideoFolder>>(emptySet()) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    
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
            TopAppBar(
                title = {
                    Text(
                        "文件夹",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (hasPermission) {
                        // 编辑按钮
                        IconButton(onClick = { 
                            isEditMode = !isEditMode
                            if (!isEditMode) {
                                selectedFolders = emptySet()
                            }
                        }) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = if (isEditMode) "退出编辑" else "编辑"
                            )
                        }
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "排序"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    EmptyState("未找到视频文件夹")
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
                                    isSelected = folder == selectedFolder,
                                    isEditMode = isEditMode,
                                    isChecked = selectedFolders.contains(folder),
                                    onClick = { 
                                        if (isEditMode) {
                                            // 编辑模式下切换选中状态
                                            selectedFolders = if (selectedFolders.contains(folder)) {
                                                selectedFolders - folder
                                            } else {
                                                selectedFolders + folder
                                            }
                                        } else {
                                            // 正常模式下打开文件夹
                                            onOpenFolder(folder)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isEditMode) {
                                            selectedFolder = folder
                                            showOperationMenu = true
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 文件操作菜单（全屏显示）
                        FileOperationMenu(
                            visible = showOperationMenu && selectedFolder != null && !isEditMode,
                            fileName = selectedFolder?.folderName ?: "",
                            onDismiss = { 
                                showOperationMenu = false
                                selectedFolder = null
                            },
                            onRename = { 
                                showRenameDialog = true
                                showOperationMenu = false  // 关闭菜单
                            },
                            onDelete = { 
                                showDeleteDialog = true
                                showOperationMenu = false  // 关闭菜单
                            },
                            onCopy = null  // 文件夹不提供复制功能
                        )
                        
                        // 多选操作栏
                        MultiSelectActionBar(
                            visible = isEditMode && selectedFolders.isNotEmpty(),
                            selectedCount = selectedFolders.size,
                            totalCount = folders.size,
                            onSelectAll = {
                                if (selectedFolders.size == folders.size) {
                                    // 取消全选
                                    selectedFolders = emptySet()
                                } else {
                                    // 全选
                                    selectedFolders = folders.toSet()
                                }
                            },
                            onRename = {
                                if (selectedFolders.size == 1) {
                                    selectedFolder = selectedFolders.first()
                                    showRenameDialog = true
                                }
                            },
                            onDelete = {
                                showBatchDeleteDialog = true
                            },
                            onCopy = null,  // 文件夹不提供复制功能
                            onCancel = {
                                isEditMode = false
                                selectedFolders = emptySet()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        )
                        
                        // 添加刷新按钮（编辑模式时隐藏）
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
                                onClick = { refreshFolders() }
                            ) {
                                Icon(Icons.Default.Refresh, "刷新", tint = MaterialTheme.colorScheme.onPrimary)
                            }
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
    
    // 重命名对话框
    if (showRenameDialog && selectedFolder != null) {
        RenameDialog(
            visible = true,
            currentName = selectedFolder!!.folderName,
            onDismiss = { 
                showRenameDialog = false
                selectedFolder = null
            },
            onConfirm = { newName ->
                lifecycleOwner.lifecycleScope.launch {
                    val oldPath = selectedFolder!!.folderPath
                    val newPath = FileOperationManager.rename(context, oldPath, newName)
                    if (newPath != null) {
                        // 刷新列表
                        refreshFolders()
                    }
                    showRenameDialog = false
                    selectedFolder = null
                    // 退出编辑模式
                    if (isEditMode) {
                        selectedFolders = emptySet()
                        isEditMode = false
                    }
                }
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog && selectedFolder != null) {
        DeleteConfirmDialog(
            visible = true,
            fileName = selectedFolder!!.folderName,
            isFolder = true,
            onDismiss = { 
                showDeleteDialog = false
                selectedFolder = null
            },
            onConfirm = {
                lifecycleOwner.lifecycleScope.launch {
                    val success = FileOperationManager.delete(
                        context,
                        selectedFolder!!.folderPath,
                        isFolder = true
                    )
                    if (success) {
                        // 刷新列表
                        refreshFolders()
                    }
                    showDeleteDialog = false
                    selectedFolder = null
                }
            }
        )
    }
    
    // 批量删除确认对话框
    if (showBatchDeleteDialog && selectedFolders.isNotEmpty()) {
        BatchDeleteConfirmDialog(
            visible = true,
            count = selectedFolders.size,
            isFolder = true,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = {
                lifecycleOwner.lifecycleScope.launch {
                    selectedFolders.forEach { folder ->
                        FileOperationManager.delete(
                            context,
                            folder.folderPath,
                            isFolder = true
                        )
                    }
                    // 刷新列表
                    refreshFolders()
                    selectedFolders = emptySet()
                    isEditMode = false
                    showBatchDeleteDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderItem(
    folder: VideoFolder,
    isSelected: Boolean,
    isEditMode: Boolean,
    isChecked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
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
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected && !isEditMode) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${folder.videoCount} 个视频",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右箭头（非编辑模式）
            if (!isEditMode) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "需要存储权限",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请授予存储权限以浏览视频文件",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("授予权限")
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
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Text(
                text = "排序方式",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
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
                    text = "视频数量 (升序)",
                    isSelected = currentSortType == "VIDEO_COUNT" && currentSortOrder == "ASCENDING",
                    onClick = { onSortSelected("VIDEO_COUNT", "ASCENDING") }
                )
                SortOption(
                    text = "视频数量 (降序)",
                    isSelected = currentSortType == "VIDEO_COUNT" && currentSortOrder == "DESCENDING",
                    onClick = { onSortSelected("VIDEO_COUNT", "DESCENDING") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
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
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
