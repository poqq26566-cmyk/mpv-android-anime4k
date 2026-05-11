package com.fanchen.fam4k007.manager.compose

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.bilibili.model.LoginResult
import com.fam4k007.videoplayer.bilibili.model.QRCodeInfo
import com.fam4k007.videoplayer.bilibili.model.UserInfo
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.KoinAndroidContext
import androidx.compose.runtime.mutableIntStateOf

/**
 * B站登录界面
 */
class BiliBiliLoginActivity : ComponentActivity() {

    private var themeRevision by mutableIntStateOf(0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val authManager = BiliBiliAuthManager.getInstance(this)
        
        setContent {
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@BiliBiliLoginActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    BiliBiliLoginScreen(
                        authManager = authManager,
                        onClose = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        themeRevision++
    }
}

/**
 * 登录界面UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliBiliLoginScreen(
    authManager: BiliBiliAuthManager,
    onClose: () -> Unit,
    viewModel: LoginViewModel = viewModel { LoginViewModel(authManager) }
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        if (!authManager.isLoggedIn()) {
            viewModel.generateQRCode()
        }
    }
    
    // 监听登录成功，自动关闭
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.LoggedIn) {
            delay(1500) // 显示1.5秒成功状态
            onClose()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录 Bilibili") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is LoginUiState.Loading -> {
                    LoadingContent()
                }
                is LoginUiState.ShowQRCode -> {
                    QRCodeContent(
                        qrCodeInfo = state.qrCodeInfo,
                        status = state.status,
                        onRefresh = { viewModel.generateQRCode() }
                    )
                    
                    LaunchedEffect(state.qrCodeInfo.qrcodeKey) {
                        viewModel.startPolling(state.qrCodeInfo.qrcodeKey)
                    }
                }
                is LoginUiState.LoggedIn -> {
                    LoggedInContent(
                        userInfo = state.userInfo,
                        onLogout = { viewModel.logout() }
                    )
                }
                is LoginUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.generateQRCode() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("正在生成二维码...", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QRCodeContent(
    qrCodeInfo: QRCodeInfo,
    status: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val qrBitmap = remember(qrCodeInfo.url) {
                    generateQRCodeBitmap(qrCodeInfo.url, 240)
                }
                
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "status"
        ) { currentStatus ->
            Text(
                text = currentStatus,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = when (currentStatus) {
                    "等待扫码..." -> MaterialTheme.colorScheme.onSurfaceVariant
                    "已扫码，等待确认..." -> MaterialTheme.colorScheme.primary
                    "二维码已过期" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "1.扫描以后不要马上点确认！\n2.等二维码下方等待扫码变为已扫码以后，再点确定！\n3.点确定以后不要急！本页面会自动返回到上一级，在此期间请不要进行任何操作！",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (status == "二维码已过期") {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("刷新二维码")
            }
        }
    }
}

@Composable
private fun LoggedInContent(
    userInfo: UserInfo,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✓ 登录成功",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("用户名: ${userInfo.uname}", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("UID: ${userInfo.mid}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                if (userInfo.vipStatus == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "大会员 ✨",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("退出登录")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✕",
            fontSize = 48.sp,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("重试")
        }
    }
}

/**
 * 生成二维码Bitmap
 */
private fun generateQRCodeBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

/**
 * 登录UI状态
 */
sealed class LoginUiState {
    object Loading : LoginUiState()
    data class ShowQRCode(val qrCodeInfo: QRCodeInfo, val status: String) : LoginUiState()
    data class LoggedIn(val userInfo: UserInfo) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * 登录ViewModel
 */
class LoginViewModel(private val authManager: BiliBiliAuthManager) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState
    
    init {
        // 检查登录状态
        if (authManager.isLoggedIn()) {
            authManager.getUserInfo()?.let {
                _uiState.value = LoginUiState.LoggedIn(it)
            }
        }
    }
    
    fun generateQRCode() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            
            val result = authManager.generateQRCode()
            result.onSuccess { qrCodeInfo ->
                _uiState.value = LoginUiState.ShowQRCode(qrCodeInfo, "等待扫码...")
            }.onFailure { error ->
                _uiState.value = LoginUiState.Error(error.message ?: "生成二维码失败")
            }
        }
    }
    
    fun startPolling(qrcodeKey: String) {
        viewModelScope.launch {
            repeat(60) { // 最多轮询60次 (3分钟)
                delay(3000) // 每3秒轮询一次
                
                val result = authManager.pollQRCodeStatus(qrcodeKey)
                
                when (result) {
                    is LoginResult.Success -> {
                        // 登录成功，更新UI状态
                        com.fam4k007.videoplayer.utils.Logger.d("BiliLogin", "Login success in ViewModel!")
                        
                        // 等待一下确保用户信息已保存
                        delay(500)
                        
                        var userInfo = authManager.getUserInfo()
                        com.fam4k007.videoplayer.utils.Logger.d("BiliLogin", "User info retrieved: ${userInfo?.uname}")
                        
                        if (userInfo == null) {
                            com.fam4k007.videoplayer.utils.Logger.e("BiliLogin", "User info is null, creating placeholder")
                            // 即使用户信息为空，也创建一个占位符让登录成功
                            userInfo = UserInfo(
                                mid = 0,
                                uname = "B站用户",
                                face = "",
                                vipStatus = 0,
                                vipType = 0
                            )
                        }
                        
                        com.fam4k007.videoplayer.utils.Logger.d("BiliLogin", "Setting LoggedIn state")
                        _uiState.value = LoginUiState.LoggedIn(userInfo)
                        return@launch // 退出轮询
                    }
                    is LoginResult.WaitingScan -> {
                        updateStatus("等待扫码...")
                    }
                    is LoginResult.WaitingConfirm -> {
                        updateStatus("已扫码，等待确认...")
                    }
                    is LoginResult.Expired -> {
                        updateStatus("二维码已过期")
                        return@launch
                    }
                    is LoginResult.Failed -> {
                        _uiState.value = LoginUiState.Error(result.message)
                        return@launch
                    }
                }
            }
            
            // 超时
            updateStatus("二维码已过期")
        }
    }
    
    private fun updateStatus(status: String) {
        val currentState = _uiState.value
        if (currentState is LoginUiState.ShowQRCode) {
            _uiState.value = currentState.copy(status = status)
        }
    }
    
    fun logout() {
        authManager.logout()
        _uiState.value = LoginUiState.Loading
        generateQRCode()
    }
}
