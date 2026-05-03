package com.fanchen.fam4k007.manager.compose

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam4k007.videoplayer.BaseComponentActivity
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.bilibili.model.LoginResult
import com.fam4k007.videoplayer.bilibili.model.QRCodeInfo
import com.fam4k007.videoplayer.bilibili.model.UserInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * B站登录界面
 */
class BiliBiliLoginActivity : BaseComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val authManager = BiliBiliAuthManager.getInstance(this)
        
        setContent {
            MaterialTheme {
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

/**
 * 登录界面UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliBiliLoginScreen(
    authManager: BiliBiliAuthManager,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel { LoginViewModel(authManager, context.applicationContext) }
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
                title = { Text("登录 Bilibili", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF6699)
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
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
            color = Color(0xFFFF6699)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.bilibili_generating_qr), fontSize = 16.sp, color = Color.Gray)
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
            // 将状态文本本地化
            val displayText = when (currentStatus) {
                "等待扫码..." -> stringResource(R.string.bilibili_waiting_scan)
                "已扫码，等待确认..." -> stringResource(R.string.bilibili_scanned_waiting)
                "二维码已过期" -> stringResource(R.string.bilibili_qr_expired)
                else -> currentStatus
            }
            
            Text(
                text = displayText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = when (currentStatus) {
                    "等待扫码..." -> Color.Gray
                    "已扫码，等待确认..." -> Color(0xFFFF6699)
                    "二维码已过期" -> Color.Red
                    else -> Color.Black
                },
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.bilibili_login_notice),
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Start,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (status == stringResource(R.string.bilibili_qr_expired)) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6699)
                )
            ) {
                Text(stringResource(R.string.bilibili_refresh_qr))
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
            text = stringResource(R.string.bilibili_login_success),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00C853)
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
                Text(stringResource(R.string.bilibili_username, userInfo.uname), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("UID: ${userInfo.mid}", fontSize = 14.sp, color = Color.Gray)
                
                if (userInfo.vipStatus == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.bilibili_vip),
                        fontSize = 14.sp,
                        color = Color(0xFFFF6699),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Gray
            )
        ) {
            Text(stringResource(R.string.bilibili_logout))
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
            color = Color.Red
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6699)
            )
        ) {
            Text(stringResource(R.string.common_retry))
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
class LoginViewModel(private val authManager: BiliBiliAuthManager, private val context: Context) : ViewModel() {
    
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
                _uiState.value = LoginUiState.ShowQRCode(qrCodeInfo, context.getString(R.string.bilibili_waiting_scan))
            }.onFailure { error ->
                _uiState.value = LoginUiState.Error(error.message ?: context.getString(R.string.bilibili_qr_gen_failed))
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
                                uname = context.getString(R.string.bilibili_user),
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
                        updateStatus(context.getString(R.string.bilibili_waiting_scan))
                    }
                    is LoginResult.WaitingConfirm -> {
                        updateStatus(context.getString(R.string.bilibili_scanned_waiting))
                    }
                    is LoginResult.Expired -> {
                        updateStatus(context.getString(R.string.bilibili_qr_expired))
                        return@launch
                    }
                    is LoginResult.Failed -> {
                        _uiState.value = LoginUiState.Error(result.message)
                        return@launch
                    }
                }
            }
            
            // 超时
            updateStatus(context.getString(R.string.bilibili_qr_expired))
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
