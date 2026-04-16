package com.fam4k007.videoplayer.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.dandanplay.AnimeSearchInfo
import com.fam4k007.videoplayer.dandanplay.EpisodeInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 网络弹幕搜索对话框 - 右侧抽屉式
 * 参考字幕设置和弹幕设置的样式
 */
@Composable
fun DanDanPlaySearchDialog(
    onDismiss: () -> Unit,
    onEpisodeSelected: (episodeId: Int, animeTitle: String, episodeTitle: String) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 启动时触发动画
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 处理返回键
    BackHandler(enabled = isVisible) {
        isVisible = false
        coroutineScope.launch {
            delay(300)
            onDismiss()
        }
    }

    // 点击背景关闭
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                isVisible = false
                coroutineScope.launch {
                    delay(300)
                    onDismiss()
                }
            }
    ) {
        // 右侧抽屉
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(380.dp)  // 比弹幕设置稍宽一点，因为需要显示搜索结果
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击穿透 */ }
            ) {
                // 半透明背景层（与字幕/弹幕设置风格一致）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xCC121212), // 左边缘 80% 不透明
                                    Color(0xE6121212)  // 右边缘 90% 不透明
                                )
                            )
                        )
                )

                // 内容层
                DanDanPlaySearchContent(
                    onDismiss = {
                        isVisible = false
                        coroutineScope.launch {
                            delay(300)
                            onDismiss()
                        }
                    },
                    onEpisodeSelected = onEpisodeSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DanDanPlaySearchContent(
    onDismiss: () -> Unit,
    onEpisodeSelected: (episodeId: Int, animeTitle: String, episodeTitle: String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AnimeSearchInfo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedAnime by remember { mutableStateOf<AnimeSearchInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // 预获取所有需要的字符串资源
    val noResultsText = stringResource(R.string.dandanplay_no_results)
    val accessDeniedText = stringResource(R.string.dandanplay_access_denied)
    val apiNotFoundText = stringResource(R.string.dandanplay_api_not_found)
    val timeoutText = stringResource(R.string.dandanplay_timeout)
    val searchFailedFormat = stringResource(R.string.dandanplay_search_failed)
    val searchExceptionFormat = stringResource(R.string.dandanplay_search_exception)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedAnime == null) stringResource(R.string.dandanplay_network_danmaku) else stringResource(R.string.dandanplay_select_episode),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮（在选择剧集界面显示）
                if (selectedAnime != null) {
                    TextButton(
                        onClick = { selectedAnime = null },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF64B5F6)
                        )
                    ) {
                        Text("← 返回", fontSize = 13.sp)
                    }
                }
                
                // 关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "✕",
                        fontSize = 20.sp,
                        color = Color(0xFFBBBBBB)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 搜索框（仅在搜索界面显示）
        if (selectedAnime == null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(stringResource(R.string.dandanplay_search_placeholder), color = Color(0xFF888888)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF64B5F6),
                        unfocusedBorderColor = Color(0xFF555555),
                        cursorColor = Color(0xFF64B5F6)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        if (searchText.isNotBlank()) {
                            coroutineScope.launch {
                                isSearching = true
                                errorMessage = null
                                try {
                                    android.util.Log.d("DanDanPlayUI", "开始搜索: $searchText")
                                    val api = com.fam4k007.videoplayer.dandanplay.DanDanPlayApi()
                                    val result = api.searchAnime(searchText)
                                    result.fold(
                                        onSuccess = { response ->
                                            android.util.Log.d("DanDanPlayUI", "搜索成功，返回 ${response.animes.size} 个结果")
                                            searchResults = response.animes
                                            if (searchResults.isEmpty()) {
                                                errorMessage = noResultsText
                                            }
                                        },
                                        onFailure = { e ->
                                            val errorMsg = when {
                                                e.message?.contains("403") == true -> accessDeniedText
                                                e.message?.contains("404") == true -> apiNotFoundText
                                                e.message?.contains("timeout") == true -> timeoutText
                                                else -> searchFailedFormat.format(e.message ?: "")
                                            }
                                            android.util.Log.e("DanDanPlayUI", "搜索失败: ${e.message}", e)
                                            errorMessage = errorMsg
                                        }
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("DanDanPlayUI", "搜索异常", e)
                                    errorMessage = searchExceptionFormat.format(e.message ?: "")
                                } finally {
                                    isSearching = false
                                }
                            }
                        }
                    },
                    enabled = searchText.isNotBlank() && !isSearching,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64B5F6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.common_search), fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 错误提示
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C1810), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = Color(0xFFFF9800)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 加载指示器
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF64B5F6),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.dandanplay_searching),
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        } else if (selectedAnime != null) {
            // 显示剧集列表
            EpisodeList(
                anime = selectedAnime!!,
                onEpisodeSelected = { episode ->
                    onEpisodeSelected(
                        episode.episodeId,
                        selectedAnime!!.animeTitle,
                        episode.episodeTitle
                    )
                    onDismiss()
                }
            )
        } else if (searchResults.isNotEmpty()) {
            // 显示搜索结果
            SearchResultsList(
                results = searchResults,
                onAnimeSelected = { anime ->
                    selectedAnime = anime
                }
            )
        } else {
            // 空状态提示
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.dandanplay_input_name),
                        fontSize = 16.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<AnimeSearchInfo>,
    onAnimeSelected: (AnimeSearchInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results) { anime ->
            AnimeCard(anime = anime, onClick = { onAnimeSelected(anime) })
        }
    }
}

@Composable
private fun AnimeCard(
    anime: AnimeSearchInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2332)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = anime.animeTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // 类型标签
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF64B5F6),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = anime.typeDescription,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 剧集数量
            Text(
                text = stringResource(R.string.dandanplay_episode_count, anime.episodes.size),
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
private fun EpisodeList(
    anime: AnimeSearchInfo,
    onEpisodeSelected: (EpisodeInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 番剧信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2332)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = anime.animeTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${anime.typeDescription} · ${stringResource(R.string.dandanplay_episode_count, anime.episodes.size)}",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 剧集列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(anime.episodes) { episode ->
                EpisodeCard(episode = episode, onClick = { onEpisodeSelected(episode) })
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: EpisodeInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2332)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = episode.episodeTitle,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // 箭头图标
            Text(
                text = "→",
                fontSize = 18.sp,
                color = Color(0xFF64B5F6)
            )
        }
    }
}
