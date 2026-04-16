package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.bilibili.model.BiliApiResponse
import com.fam4k007.videoplayer.compose.ImmersiveTopAppBar
import com.fam4k007.videoplayer.remote.RemotePlaybackHeaders
import com.fam4k007.videoplayer.remote.RemotePlaybackLauncher
import com.fam4k007.videoplayer.remote.RemotePlaybackRequest
import com.fanchen.fam4k007.manager.compose.BiliBiliLoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * B站番剧播放页面 - 简单版
 * 用户输入番剧链接，解析后显示集数列表
 */
@OptIn(ExperimentalFoundationApi::class)
class BiliBiliPlayActivity : ComponentActivity() {
    
    private lateinit var authManager: BiliBiliAuthManager
    private val client by lazy { authManager.getClient() }
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableEdgeToEdge()
        
        authManager = BiliBiliAuthManager.getInstance(this)
        
        setContent {
            MaterialTheme {
                BiliBiliPlayScreen(
                    authManager = authManager,
                    onBack = { 
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onPlayEpisode = { epId, cid, title ->
                        playEpisode(epId, cid, title)
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 从登录页返回时，触发重组以更新登录状态
        setContent {
            MaterialTheme {
                BiliBiliPlayScreen(
                    authManager = authManager,
                    onBack = { 
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onPlayEpisode = { epId, cid, title ->
                        playEpisode(epId, cid, title)
                    }
                )
            }
        }
    }
    
    /**
     * 播放B站番剧集数
     */
    private fun playEpisode(epId: Long, cid: Long, title: String) {
        lifecycleScope.launch {
            try {
                // 检查登录
                if (!authManager.isLoggedIn()) {
                    Toast.makeText(this@BiliBiliPlayActivity, "请先登录", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                Toast.makeText(this@BiliBiliPlayActivity, "正在获取播放地址...", Toast.LENGTH_SHORT).show()
                
                // 获取播放地址
                val playUrl = getPlayUrl(epId, cid)
                if (playUrl.isNullOrEmpty()) {
                    Toast.makeText(this@BiliBiliPlayActivity, "获取播放地址失败", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                android.util.Log.d("BiliPlay", "Play URL: $playUrl")

                val requestHeaders = RemotePlaybackHeaders.normalize(
                    linkedMapOf(
                        "Cookie" to authManager.getCookieString(),
                        "Referer" to "https://www.bilibili.com"
                    )
                )

                val request = RemotePlaybackRequest(
                    url = playUrl,
                    title = title,
                    headers = requestHeaders,
                    sourcePageUrl = "https://www.bilibili.com",
                    source = RemotePlaybackRequest.Source.BILIBILI
                )
                RemotePlaybackLauncher.start(this@BiliBiliPlayActivity, request)
                
            } catch (e: Exception) {
                android.util.Log.e("BiliPlay", "Play error", e)
                Toast.makeText(this@BiliBiliPlayActivity, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 获取播放地址
     */
    private suspend fun getPlayUrl(epId: Long, cid: Long): String? = withContext(Dispatchers.IO) {
        try {
            // 根据B站API文档：
            // fnval=0 -> FLV格式（可能分段）
            // fnval=16 -> DASH格式（音视频分离，需要单独处理）
            // qn=80 -> 1080P（登录后可用，优先尝试）
            // qn=64 -> 720P（登录后保底）
            val url = "https://api.bilibili.com/pgc/player/web/playurl".toHttpUrl().newBuilder()
                .addQueryParameter("ep_id", epId.toString())
                .addQueryParameter("cid", cid.toString())
                .addQueryParameter("qn", "80")  // 1080P（登录后优先尝试最高画质）
                .addQueryParameter("fnval", "0")  // FLV格式（完整视频）
                .addQueryParameter("fnver", "0")
                .addQueryParameter("fourk", "1")
                .build()
            
            android.util.Log.d("BiliPlay", "Request URL: $url")
            android.util.Log.d("BiliPlay", "Cookies: ${authManager.getCookies()}")
            
            val request = Request.Builder()
                .url(url)
                .header("Referer", "https://www.bilibili.com")
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            android.util.Log.d("BiliPlay", "PlayUrl response code: ${response.code}")
            android.util.Log.d("BiliPlay", "PlayUrl response body length: ${body?.length}")
            android.util.Log.d("BiliPlay", "PlayUrl response: ${body?.take(500)}")  // 只显示前500字符
            
            if (!response.isSuccessful || body == null) {
                android.util.Log.e("BiliPlay", "Request failed or body is null")
                return@withContext null
            }
            
            val jsonResponse = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            val code = jsonResponse.get("code")?.asInt
            
            android.util.Log.d("BiliPlay", "API code: $code")
            
            if (code != 0) {
                val message = jsonResponse.get("message")?.asString
                android.util.Log.e("BiliPlay", "API error code: $code, message: $message")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BiliBiliPlayActivity, "获取播放地址失败: $message", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            }
            
            // 尝试从result中获取durl
            val result = jsonResponse.getAsJsonObject("result")
            if (result == null) {
                android.util.Log.e("BiliPlay", "result is null")
                return@withContext null
            }
            
            // 记录实际返回的画质信息
            val quality = result.get("quality")?.asInt
            val format = result.get("format")?.asString
            val acceptQuality = result.getAsJsonArray("accept_quality")
            android.util.Log.d("BiliPlay", "返回画质: quality=$quality, format=$format")
            android.util.Log.d("BiliPlay", "可用画质列表: $acceptQuality")
            
            // 画质映射表
            val qualityName = when(quality) {
                127 -> "8K超高清"
                126 -> "杜比视界"
                125 -> "HDR真彩"
                120 -> "4K超清"
                116 -> "1080P60帧"
                112 -> "1080P高码率"
                80 -> "1080P高清"
                64 -> "720P高清"
                32 -> "480P清晰"
                16 -> "360P流畅"
                else -> "${quality}P"
            }
            
            // 给用户提示实际画质
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BiliBiliPlayActivity, "画质: $qualityName", Toast.LENGTH_SHORT).show()
            }
            
            // 尝试durl（mp4格式）
            val durlArray = result.getAsJsonArray("durl")
            if (durlArray != null && durlArray.size() > 0) {
                val playUrl = durlArray.firstOrNull()?.asJsonObject?.get("url")?.asString
                android.util.Log.d("BiliPlay", "Got durl play URL: ${playUrl?.take(100)}")
                return@withContext playUrl
            }
            
            // 尝试dash格式
            val dash = result.getAsJsonObject("dash")
            if (dash != null) {
                val video = dash.getAsJsonArray("video")?.firstOrNull()?.asJsonObject
                val playUrl = video?.get("base_url")?.asString
                android.util.Log.d("BiliPlay", "Got dash play URL: ${playUrl?.take(100)}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BiliBiliPlayActivity, "提示：DASH格式可能需要单独下载音频", Toast.LENGTH_LONG).show()
                }
                return@withContext playUrl
            }
            
            android.util.Log.e("BiliPlay", "No durl or dash found in response")
            return@withContext null
            
        } catch (e: Exception) {
            android.util.Log.e("BiliPlay", "GetPlayUrl error", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BiliBiliPlayActivity, "获取播放地址异常: ${e.message}", Toast.LENGTH_LONG).show()
            }
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliBiliPlayScreen(
    authManager: BiliBiliAuthManager,
    onBack: () -> Unit,
    onPlayEpisode: (Long, Long, String) -> Unit,  // (epId, cid, title)
    viewModel: BiliBiliPlayViewModel = viewModel { BiliBiliPlayViewModel(authManager) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputUrl by remember { mutableStateOf("") }
    val isLoggedIn = remember { mutableStateOf(authManager.isLoggedIn()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current  // 获取焦点管理器用于隐藏键盘
    var showLogoutDialog by remember { mutableStateOf(false) }  // 退出登录对话框状态
    
    // 监听生命周期，自动更新登录状态
    LaunchedEffect(Unit) {
        isLoggedIn.value = authManager.isLoggedIn()
    }
    
    // 退出登录确认对话框
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authManager.logout()
                        isLoggedIn.value = false
                        showLogoutDialog = false
                        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("确定", color = Color(0xFFFF6699))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = { Text("B站番剧播放") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    if (!isLoggedIn.value) {
                        TextButton(onClick = {
                            context.startActivity(Intent(context, BiliBiliLoginActivity::class.java))
                        }) {
                            Text("登录", color = Color.White)
                        }
                    } else {
                        val userInfo = authManager.getUserInfo()
                        // 使用TextButton让用户名可点击
                        TextButton(
                            onClick = { showLogoutDialog = true }  // 点击显示退出对话框
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = userInfo?.uname ?: "已登录",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text("⚙", fontSize = 16.sp, color = Color.White)  // 添加设置图标提示
                            }
                        }
                    }
                },
                gradientColors = Color(0xFFFF6699) to Color(0xFFFF99BB)  // B站粉色渐变
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // 输入框区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("粘贴番剧链接", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入B站番剧链接") },
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { 
                            focusManager.clearFocus()  // 隐藏键盘
                            viewModel.parseUrl(inputUrl)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6699))
                    ) {
                        Text("解析")
                    }
                }
            }
            
            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is PlayUiState.Idle -> {
                        Text(
                            text = "请输入番剧链接",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            color = Color.Gray
                        )
                    }
                    is PlayUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFFFF6699)
                        )
                    }
                    is PlayUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(state.message, color = Color.Red)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.parseUrl(inputUrl) }) {
                                Text("重试")
                            }
                        }
                    }
                    is PlayUiState.Success -> {
                        BangumiDetailView(
                            bangumi = state.bangumi,
                            onPlayEpisode = onPlayEpisode
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BangumiDetailView(
    bangumi: SimpleBangumiInfo,
    onPlayEpisode: (Long, Long, String) -> Unit  // (epId, cid, title)
) {
    val listState = rememberLazyListState()  // 添加列表状态以提升滑动性能
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),  // 增加间距提升视觉体验
        modifier = Modifier.fillMaxSize()
    ) {
        // 番剧信息
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)  // 增加阴影
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(bangumi.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("共 ${bangumi.episodes.size} 集", color = Color.Gray)
                }
            }
        }
        
        // 集数列表 - 添加动画效果
        items(bangumi.episodes, key = { it.epId }) { episode ->  // 使用key提升性能
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))  // 圆角裁剪
                    .clickable { onPlayEpisode(episode.epId, episode.cid, episode.title) }
                    .animateItemPlacement(),  // 添加item位置动画
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 6.dp  // 点击时增加阴影
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),  // 调整内边距
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        episode.title, 
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)  // 让标题占据剩余空间
                    )
                    Text("▶", fontSize = 20.sp, color = Color(0xFFFF6699))
                }
            }
        }
    }
}

// ========== ViewModel ==========

class BiliBiliPlayViewModel(private val authManager: BiliBiliAuthManager) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PlayUiState>(PlayUiState.Idle)
    val uiState: StateFlow<PlayUiState> = _uiState
    
    private val client = authManager.getClient()
    private val gson = Gson()
    
    fun parseUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = PlayUiState.Loading
            
            // 提取ssid或epid
            val seasonId = extractSeasonId(url)
            val epId = extractEpId(url)
            
            if (seasonId == null && epId == null) {
                _uiState.value = PlayUiState.Error("无效的番剧链接")
                return@launch
            }
            
            // 调用API获取详情
            val result = if (seasonId != null) {
                getBangumiDetail(seasonId)
            } else {
                getBangumiDetailByEp(epId!!)
            }
            
            result.fold(
                onSuccess = { _uiState.value = PlayUiState.Success(it) },
                onFailure = { _uiState.value = PlayUiState.Error(it.message ?: "获取失败") }
            )
        }
    }
    
    private fun extractSeasonId(url: String): Long? {
        val regex = """ss(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)?.toLongOrNull()
    }
    
    private fun extractEpId(url: String): Long? {
        val regex = """ep(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)?.toLongOrNull()
    }
    
    private suspend fun getBangumiDetail(seasonId: Long): Result<SimpleBangumiInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.bilibili.com/pgc/view/web/season?season_id=$seasonId")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("网络错误"))
            
            val apiResponse = gson.fromJson(body, BangumiDetailResponse::class.java)
            
            if (apiResponse.code != 0) {
                return@withContext Result.failure(Exception("API错误: ${apiResponse.message}"))
            }
            
            val data = apiResponse.result ?: return@withContext Result.failure(Exception("数据为空"))
            
            val bangumi = SimpleBangumiInfo(
                title = data.title ?: "未知",
                episodes = data.episodes?.map {
                    SimpleEpisode(
                        title = it.long_title ?: it.title ?: "未知集数",
                        cid = it.cid ?: 0,
                        epId = it.id ?: 0
                    )
                } ?: emptyList()
            )
            
            Result.success(bangumi)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getBangumiDetailByEp(epId: Long): Result<SimpleBangumiInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.bilibili.com/pgc/view/web/season?ep_id=$epId")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("网络错误"))
            
            val apiResponse = gson.fromJson(body, BangumiDetailResponse::class.java)
            
            if (apiResponse.code != 0) {
                return@withContext Result.failure(Exception("API错误: ${apiResponse.message}"))
            }
            
            val data = apiResponse.result ?: return@withContext Result.failure(Exception("数据为空"))
            
            val bangumi = SimpleBangumiInfo(
                title = data.title ?: "未知",
                episodes = data.episodes?.map {
                    SimpleEpisode(
                        title = it.long_title ?: it.title ?: "未知集数",
                        cid = it.cid ?: 0,
                        epId = it.id ?: 0
                    )
                } ?: emptyList()
            )
            
            Result.success(bangumi)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ========== UI State ==========

sealed class PlayUiState {
    object Idle : PlayUiState()
    object Loading : PlayUiState()
    data class Error(val message: String) : PlayUiState()
    data class Success(val bangumi: SimpleBangumiInfo) : PlayUiState()
}

// ========== 数据模型 ==========

data class SimpleBangumiInfo(
    val title: String,
    val episodes: List<SimpleEpisode>
)

data class SimpleEpisode(
    val title: String,
    val cid: Long,
    val epId: Long
)

// API响应模型
data class BangumiDetailResponse(
    val code: Int,
    val message: String?,
    val result: BangumiDetailResult?
)

data class BangumiDetailResult(
    val title: String?,
    val episodes: List<EpisodeItem>?
)

data class EpisodeItem(
    val id: Long?,
    val title: String?,
    val long_title: String?,
    val cid: Long?
)
