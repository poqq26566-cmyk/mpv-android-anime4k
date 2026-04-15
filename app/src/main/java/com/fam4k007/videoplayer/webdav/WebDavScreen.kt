package com.fam4k007.videoplayer.webdav

import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.fam4k007.videoplayer.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fam4k007.videoplayer.compose.ImmersiveTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * WebDAV 账户列表屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavAccountListScreen(
    onNavigateBack: () -> Unit,
    onAccountSelected: (WebDavAccount) -> Unit
) {
    val context = LocalContext.current
    val accountManager = remember { WebDavAccountManager.getInstance(context) }
    var accounts by remember { mutableStateOf(accountManager.getAllAccounts()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<WebDavAccount?>(null) }
    
    // 刷新账户列表
    fun refreshAccounts() {
        accounts = accountManager.getAllAccounts()
    }
    
    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = { Text(stringResource(R.string.webdav_account_management), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.webdav_add_account))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F5F5),
                            Color(0xFFE8EAF6)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (accounts.isEmpty()) {
                // 空状态
                EmptyAccountsView(
                    onAddClick = { showAddDialog = true }
                )
            } else {
                // 账户列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { account ->
                        WebDavAccountCard(
                            account = account,
                            onClick = { onAccountSelected(account) },
                            onDeleteClick = { accountToDelete = account }
                        )
                    }
                }
            }
        }
    }
    
    // 添加账户对话框
    if (showAddDialog) {
        WebDavAddAccountDialog(
            onDismiss = { showAddDialog = false },
            onAccountAdded = {
                refreshAccounts()
                showAddDialog = false
            }
        )
    }
    
    // 删除确认对话框
    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text(stringResource(R.string.webdav_delete_account)) },
            text = { Text(stringResource(R.string.webdav_delete_confirm, account.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountManager.deleteAccount(account.id)
                        refreshAccounts()
                        accountToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.webdav_delete), color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(stringResource(R.string.webdav_cancel))
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
            tint = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.webdav_no_accounts),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.webdav_no_accounts_hint),
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.webdav_add_account))
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
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            // 图标
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 账户信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = account.serverUrl,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!account.isAnonymous && account.account.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.webdav_account_info, account.account),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // 删除按钮
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.webdav_delete),
                    tint = Color(0xFFD32F2F)
                )
            }
            
            // 箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

/**
 * 添加账户对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebDavAddAccountDialog(
    onDismiss: () -> Unit,
    onAccountAdded: () -> Unit
) {
    val context = LocalContext.current
    val accountManager = remember { WebDavAccountManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    var displayName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "添加 WebDAV 账户",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("显示名称") },
                        placeholder = { Text("如: 我的网盘") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("http://example.com/dav/") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }
                
                item {
                    // 登录模式选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isAnonymous,
                            onClick = { isAnonymous = false },
                            label = { Text("账号登录") },
                            leadingIcon = {
                                if (!isAnonymous) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        FilterChip(
                            selected = isAnonymous,
                            onClick = { isAnonymous = true },
                            label = { Text("匿名访问") },
                            leadingIcon = {
                                if (isAnonymous) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                if (!isAnonymous) {
                    item {
                        OutlinedTextField(
                            value = account,
                            onValueChange = { account = it },
                            label = { Text("账号") },
                            placeholder = { Text("用户名") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            placeholder = { Text("密码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) 
                                            Icons.Default.Visibility 
                                        else 
                                            Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            }
                        )
                    }
                }
                
                // 测试连接按钮和结果
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (serverUrl.isEmpty()) {
                                    testResult = "❌ 请填写服务器地址"
                                    return@Button
                                }
                                if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                                    testResult = "❌ 服务器地址必须以 http:// 或 https:// 开头"
                                    return@Button
                                }
                                if (!isAnonymous && (account.isEmpty() || password.isEmpty())) {
                                    testResult = "❌ 请填写账号和密码"
                                    return@Button
                                }
                                
                                testing = true
                                testResult = null
                                
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            val testConfig = WebDavConfig(
                                                serverUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/",
                                                account = account,
                                                password = password,
                                                isAnonymous = isAnonymous
                                            )
                                            val client = WebDavClient(testConfig)
                                            client.testConnection()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            false
                                        }
                                    }
                                    
                                    testing = false
                                    testResult = if (result) {
                                        "✅ 连接成功"
                                    } else {
                                        "❌ 连接失败，请检查配置"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !testing
                        ) {
                            if (testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试中...")
                            } else {
                                Icon(Icons.Default.Wifi, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试连接")
                            }
                        }
                        
                        testResult?.let { result ->
                            Text(
                                text = result,
                                fontSize = 14.sp,
                                color = if (result.startsWith("✅")) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // 底部按钮
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        
                        Button(
                            onClick = {
                                // 验证输入
                                if (displayName.isEmpty()) {
                                    testResult = "❌ 请填写显示名称"
                                    return@Button
                                }
                                if (serverUrl.isEmpty()) {
                                    testResult = "❌ 请填写服务器地址"
                                    return@Button
                                }
                                if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                                    testResult = "❌ 服务器地址必须以 http:// 或 https:// 开头"
                                    return@Button
                                }
                                if (!isAnonymous && (account.isEmpty() || password.isEmpty())) {
                                    testResult = "❌ 请填写账号和密码"
                                    return@Button
                                }
                                
                                // 添加账户
                                val newAccount = WebDavAccount(
                                    displayName = displayName,
                                    serverUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/",
                                    account = account,
                                    password = password,
                                    isAnonymous = isAnonymous
                                )
                                
                                if (accountManager.addAccount(newAccount)) {
                                    onAccountAdded()
                                } else {
                                    testResult = "❌ 该账户已存在"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
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
    onBackCallbackChanged: (((() -> Unit)?) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { 
        WebDavClient(WebDavConfig(
            serverUrl = account.serverUrl,
            account = account.account,
            password = account.password,
            isAnonymous = account.isAnonymous
        ))
    }
    
    var files by remember { mutableStateOf<List<WebDavClient.WebDavFile>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentPath by remember { mutableStateOf("") }
    val pathStack = remember { mutableListOf<String>() }
    var sortType by remember { mutableStateOf(0) } // 0:名称升序, 1:名称降序, 2:时间降序, 3:时间升序
    var showSortDialog by remember { mutableStateOf(false) }
    
    // 加载文件函数
    fun loadFiles(path: String) {
        loading = true
        error = null
        
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    client.listFiles(path)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            loading = false
            
            if (result == null) {
                error = "加载失败"
                if (pathStack.isNotEmpty()) {
                    pathStack.removeAt(pathStack.size - 1)
                    currentPath = if (pathStack.isEmpty()) "" else pathStack.last()
                }
            } else {
                files = sortFiles(result, sortType)
                currentPath = path
            }
        }
    }
    
    // 初始加载
    LaunchedEffect(Unit) {
        loadFiles("")
    }
    
    // 处理返回
    val onBack: () -> Unit = {
        if (pathStack.isNotEmpty()) {
            pathStack.removeAt(pathStack.size - 1)
            val previousPath = if (pathStack.isEmpty()) "" else pathStack.last()
            loadFiles(previousPath)
        } else {
            onNavigateBack()
        }
    }
    
    // 更新回调，让 Activity 知道当前的返回逻辑
    LaunchedEffect(pathStack.size) {
        if (pathStack.isNotEmpty()) {
            // 有文件夹栈，返回上一级
            onBackCallbackChanged?.invoke(onBack)
        } else {
            // 没有文件夹栈，返回到账户列表
            onBackCallbackChanged?.invoke(null)
        }
    }
    
    // Compose 的返回处理
    BackHandler {
        onBack()
    }
    
    Scaffold(
        topBar = {
            Column {
                ImmersiveTopAppBar(
                    title = { 
                        Text(
                            account.displayName, 
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(
                                Icons.Default.Sort, 
                                contentDescription = "排序",
                                tint = Color.White
                            )
                        }
                    }
                )
                
                // 当前路径显示
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (currentPath.isEmpty()) "/" else "/$currentPath",
                        fontSize = 13.sp,
                        color = Color.Black,
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F5F5),
                            Color(0xFFE8EAF6)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            when {
                loading -> {
                    // 加载中
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "加载中...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                error != null -> {
                    // 错误状态
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "未知错误",
                            fontSize = 16.sp,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadFiles(currentPath) }) {
                            Text("重试")
                        }
                    }
                }
                
                files.isEmpty() -> {
                    // 空状态
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "此文件夹为空",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                else -> {
                    // 文件列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files, key = { it.path }) { file ->
                            WebDavFileCard(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        pathStack.add(currentPath)
                                        loadFiles(file.path)
                                    } else if (WebDavClient.isVideoFile(file.name)) {
                                        onPlayVideo(file, client)
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
    if (showSortDialog) {
        WebDavSortDialog(
            currentSortType = sortType,
            onDismiss = { showSortDialog = false },
            onSortSelected = { newSortType ->
                sortType = newSortType
                files = sortFiles(files, sortType)
                showSortDialog = false
            }
        )
    }
}

/**
 * 排序文件列表
 */
private fun sortFiles(fileList: List<WebDavClient.WebDavFile>, sortType: Int): List<WebDavClient.WebDavFile> {
    val folders = fileList.filter { it.isDirectory }
    val files = fileList.filter { !it.isDirectory }
    
    val sortedFolders = when (sortType) {
        0 -> folders.sortedWith(naturalComparator { it.name }) // 名称升序
        1 -> folders.sortedWith(naturalComparator<WebDavClient.WebDavFile> { it.name }.reversed()) // 名称降序
        2 -> folders.sortedByDescending { it.modifiedTime } // 时间降序（新到旧）
        3 -> folders.sortedBy { it.modifiedTime } // 时间升序（旧到新）
        else -> folders.sortedWith(naturalComparator { it.name })
    }
    
    val sortedFiles = when (sortType) {
        0 -> files.sortedWith(naturalComparator { it.name })
        1 -> files.sortedWith(naturalComparator<WebDavClient.WebDavFile> { it.name }.reversed())
        2 -> files.sortedByDescending { it.modifiedTime } // 时间降序（新到旧）
        3 -> files.sortedBy { it.modifiedTime } // 时间升序（旧到新）
        else -> files.sortedWith(naturalComparator { it.name })
    }
    
    return sortedFolders + sortedFiles
}

/**
 * 自然排序比较器
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
                            onClick = { onSortSelected(type) }
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            // 图标
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.VideoFile,
                contentDescription = null,
                tint = if (file.isDirectory) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文件信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (file.isDirectory) {
                    Text(
                        text = "文件夹",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    val sizeStr = formatFileSize(file.size)
                    val dateStr = if (file.modifiedTime > 0) {
                        dateFormat.format(Date(file.modifiedTime))
                    } else {
                        ""
                    }
                    Text(
                        text = if (dateStr.isNotEmpty()) "$sizeStr · $dateStr" else sizeStr,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // 箭头
            if (file.isDirectory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
        else -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
    }
}
