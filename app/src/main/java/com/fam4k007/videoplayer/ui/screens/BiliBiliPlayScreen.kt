package com.fam4k007.videoplayer.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.presentation.BiliBiliPlayViewModel
import com.fam4k007.videoplayer.presentation.BiliPlayUiState
import com.fam4k007.videoplayer.presentation.SimpleBangumiInfo
import com.fanchen.fam4k007.manager.compose.BiliBiliLoginActivity
import org.koin.androidx.compose.koinViewModel

/**
 * B站番剧播放界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliBiliPlayScreen(
    onBack: () -> Unit,
    viewModel: BiliBiliPlayViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()

    var inputUrl by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(viewModel.isLoggedIn()) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 监听生命周期，自动更新登录状态
    LaunchedEffect(Unit) {
        isLoggedIn = viewModel.isLoggedIn()
    }

    // 退出登录确认对话框
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "确定要退出当前账号吗？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        isLoggedIn = false
                        showLogoutDialog = false
                        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("B站番剧播放", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (!isLoggedIn) {
                        TextButton(onClick = {
                            context.startActivity(Intent(context, BiliBiliLoginActivity::class.java))
                        }) {
                            Text("登录")
                        }
                    } else {
                        TextButton(onClick = { showLogoutDialog = true }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = viewModel.getUserName() ?: "已登录",
                                    fontSize = 14.sp
                                )
                                Text("⚙", fontSize = 16.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 地址栏卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入B站番剧链接") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.parseUrl(inputUrl)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("解析")
                    }
                }
            }

            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is BiliPlayUiState.Idle -> {
                        Text(
                            text = "请输入番剧链接",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is BiliPlayUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is BiliPlayUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.parseUrl(inputUrl) }) {
                                Text("重试")
                            }
                        }
                    }
                    is BiliPlayUiState.Success -> {
                        BangumiDetailView(
                            bangumi = state.bangumi,
                            onPlayEpisode = { epId, cid, title ->
                                viewModel.playEpisode(context, epId, cid, title)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BangumiDetailView(
    bangumi: SimpleBangumiInfo,
    onPlayEpisode: (Long, Long, String) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 番剧信息
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(bangumi.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("共 ${bangumi.episodes.size} 集", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 集数列表
        items(bangumi.episodes, key = { it.epId }) { episode ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                onClick = { onPlayEpisode(episode.epId, episode.cid, episode.title) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        episode.title,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text("▶", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
