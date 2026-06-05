package com.fam4k007.videoplayer.ui.webdav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.data.model.WebDavAccount
import com.fam4k007.videoplayer.domain.webdav.WebDavClient
import com.fam4k007.videoplayer.domain.webdav.WebDavFileCategory
import com.fam4k007.videoplayer.presentation.WebDavViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*


/**
 * WebDAV 账户列表屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavAccountListScreen(
    onNavigateBack: () -> Unit,
    onAccountSelected: (WebDavAccount) -> Unit,
    viewModel: WebDavViewModel = koinViewModel()
) {
    val state by viewModel.accountListState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebDAV Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.accounts.isEmpty()) {
                EmptyAccountsView(
                    onAddClick = { viewModel.showAddDialog() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.accounts, key = { it.id }) { account ->
                        WebDavAccountCard(
                            account = account,
                            onClick = { onAccountSelected(account) },
                            onEditClick = { viewModel.showEditDialog(account) },
                            onDeleteClick = { viewModel.requestDeleteAccount(account) }
                        )
                    }
                }
            }
        }
    }

    // 添加/编辑账户对话框
    if (state.showAddDialog) {
        WebDavAddAccountDialog(
            viewModel = viewModel,
            isEditMode = state.editingAccountId != null
        )
    }

    // 删除确认对话框
    if (state.accountToDelete != null) {
        val deleteAccount = state.accountToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteAccount() },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除账户") },
            text = { Text("确定要删除账户 \"${deleteAccount.displayName.orEmpty()}\" 吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteAccount() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteAccount() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyAccountsView(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No WebDAV accounts",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to add an account",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Account")
        }
    }
}

/**
 * WebDAV 账户卡片
 */
@Composable
private fun WebDavAccountCard(
    account: WebDavAccount,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName.orEmpty(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = account.serverUrl.orEmpty(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!account.isAnonymous && account.account.orEmpty().isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "账号: ${account.account.orEmpty()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 添加/编辑账户对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebDavAddAccountDialog(
    viewModel: WebDavViewModel,
    isEditMode: Boolean = false
) {
    val state by viewModel.addAccountState.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.dismissAddDialog() },
        title = {
            Text(
                text = if (isEditMode) "Edit WebDAV Account" else "Add WebDAV Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = { viewModel.updateDisplayName(it) },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. My Cloud Drive") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )

                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = { viewModel.updateServerUrl(it) },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://example.com/dav/") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !state.isAnonymous,
                        onClick = { viewModel.setAnonymous(false) },
                        label = { Text("Account Login") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = state.isAnonymous,
                        onClick = { viewModel.setAnonymous(true) },
                        label = { Text("Anonymous") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!state.isAnonymous) {
                    OutlinedTextField(
                        value = state.account,
                        onValueChange = { viewModel.updateAccount(it) },
                        label = { Text("Username") },
                        placeholder = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Password") },
                        placeholder = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (state.passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(20.dp),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisible() }) {
                                Icon(
                                    imageVector = if (state.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (state.passwordVisible) "Hide Password" else "Show Password"
                                )
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !state.isTesting
                    ) {
                        if (state.isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Connection")
                        }
                    }
                    state.testResult?.let { result ->
                        Text(
                            text = result,
                            fontSize = 14.sp,
                            color = if (result.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    state.saveError?.let { error ->
                        Text(
                            text = error,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveAccount() },
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (isEditMode) "Save" else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { viewModel.dismissAddDialog() },
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * WebDAV 文件浏览屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavBrowserScreen(
    account: WebDavAccount,
    onNavigateBack: () -> Unit,
    onPlayVideo: (WebDavClient.WebDavFile, WebDavClient) -> Unit,
    onBackCallbackChanged: (((() -> Unit)?) -> Unit)? = null,
    viewModel: WebDavViewModel = koinViewModel()
) {
    val state by viewModel.browserState.collectAsState()

    // 初始化浏览器（首次加载或账户变更时）
    LaunchedEffect(account) {
        viewModel.initBrowser(account)
    }

    // 处理返回
    val onBack: () -> Unit = {
        if (state.canGoBack) {
            viewModel.navigateBack()
        } else {
            onNavigateBack()
        }
    }

    // 更新回调，让 Activity 知道当前的返回逻辑
    LaunchedEffect(state.pathDepth) {
        onBackCallbackChanged?.invoke(
            if (state.canGoBack) onBack else null
        )
    }

    // Compose 的返回处理
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            account.displayName.orEmpty(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.showSortDialog() }) {
                            Icon(Icons.Default.Sort, contentDescription = "排序")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // 当前路径显示
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.currentPath.isEmpty()) "/" else "/${state.currentPath}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Loading...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error ?: "Unknown error",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadFiles(state.currentPath) },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("重试")
                        }
                    }
                }

                state.files.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "此文件夹为空",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    val context = LocalContext.current
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.files, key = { it.path }) { file ->
                            WebDavFileCard(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        viewModel.navigateToFolder(file.path)
                                    } else if (WebDavClient.isVideoFile(file.name)) {
                                        state.client?.let { client ->
                                            onPlayVideo(file, client)
                                        }
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "不支持的文件类型",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 排序对话框
    if (state.showSortDialog) {
        WebDavSortDialog(
            currentSortType = state.sortType,
            onDismiss = { viewModel.dismissSortDialog() },
            onSortSelected = { sortType -> viewModel.setSortType(sortType) }
        )
    }
}

/**
 * 排序对话框
 */
@Composable
private fun WebDavSortDialog(
    currentSortType: Int,
    onDismiss: () -> Unit,
    onSortSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("排序方式") },
        text = {
            Column {
                val sortOptions = listOf(
                    0 to "名称 (A-Z)",
                    1 to "名称 (Z-A)",
                    2 to "时间 (新到旧)",
                    3 to "时间 (旧到新)"
                )
                sortOptions.forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(type) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortType == type,
                            onClick = null  // 由 Row 的 clickable 处理
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * WebDAV 文件卡片
 */
@Composable
private fun WebDavFileCard(
    file: WebDavClient.WebDavFile,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 非目录文件：根据类别获取图标和颜色
    val fileIcon = if (!file.isDirectory) {
        getFileIcon(file.name)
    } else null
    val fileColor = if (!file.isDirectory) {
        getFileColor(file.name)
    } else Color.Unspecified
    val isPlayable = !file.isDirectory && (fileIcon == Icons.Default.VideoFile || fileIcon == Icons.Default.AudioFile)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else fileIcon!!,
                contentDescription = null,
                tint = if (file.isDirectory) Color(0xFFFFB74D) else fileColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name.ifEmpty { "(无名称)" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (file.isDirectory) {
                    Text(
                        text = "文件夹",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val sizeStr = WebDavViewModel.formatFileSize(file.size)
                    val dateStr = if (file.modifiedTime > 0) {
                        dateFormat.format(Date(file.modifiedTime))
                    } else ""
                    Text(
                        text = if (dateStr.isNotEmpty()) "$sizeStr · $dateStr" else sizeStr,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (file.isDirectory || isPlayable) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Default.ChevronRight else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 根据文件扩展名获取对应的 Material 图标
 */
private fun getFileIcon(fileName: String): ImageVector {
    return when (WebDavClient.getFileCategory(fileName)) {
        WebDavFileCategory.VIDEO -> Icons.Default.VideoFile
        WebDavFileCategory.AUDIO -> Icons.Default.AudioFile
        WebDavFileCategory.IMAGE -> Icons.Default.Image
        WebDavFileCategory.DOCUMENT -> Icons.Default.Description
        WebDavFileCategory.ARCHIVE -> Icons.Default.FolderZip
        WebDavFileCategory.SUBTITLE -> Icons.Default.ClosedCaption
        WebDavFileCategory.OTHER -> Icons.Default.InsertDriveFile
    }
}

/**
 * 根据文件扩展名获取对应的图标颜色
 */
@Composable
private fun getFileColor(fileName: String): Color {
    return when (WebDavClient.getFileCategory(fileName)) {
        WebDavFileCategory.VIDEO -> MaterialTheme.colorScheme.primary
        WebDavFileCategory.AUDIO -> Color(0xFF7C4DFF)      // 紫色
        WebDavFileCategory.IMAGE -> Color(0xFF00BCD4)       // 青色
        WebDavFileCategory.DOCUMENT -> Color(0xFF42A5F5)    // 蓝色
        WebDavFileCategory.ARCHIVE -> Color(0xFFFFCA28)     // 黄色
        WebDavFileCategory.SUBTITLE -> Color(0xFF66BB6A)    // 绿色
        WebDavFileCategory.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
