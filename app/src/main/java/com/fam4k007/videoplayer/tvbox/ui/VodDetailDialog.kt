package com.fam4k007.videoplayer.tvbox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fam4k007.videoplayer.tvbox.model.VodInfo
import com.fam4k007.videoplayer.tvbox.model.VodPlayLine
import kotlinx.coroutines.launch

/**
 * 影片详情对话框
 *
 * 点击搜索结果后弹出：
 * 1. 先用搜索结果的基本信息展示（标题、封面、来源）
 * 2. 同时在后台调用 spider.detailContent() 获取完整播放线路
 * 3. 获取到后展示线路选择和集数列表
 */
@Composable
fun VodDetailDialog(
    vod: VodInfo,
    isLoadingDetail: Boolean,
    detailVod: VodInfo?,
    onDismiss: () -> Unit,
    onPlayEpisode: (vod: VodInfo, flag: String, episodeUrl: String, episodeName: String) -> Unit
) {
    // 优先用详情数据，没有则用搜索结果的基本数据
    val displayVod = detailVod ?: vod
    val playLines = remember(displayVod) { displayVod.parsePlayLines() }
    var selectedLineIndex by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题
                Text(
                    text = displayVod.vodName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 基本信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (displayVod.typeName.isNotBlank()) {
                        InfoChip(displayVod.typeName)
                    }
                    if (displayVod.vodYear.isNotBlank()) {
                        InfoChip(displayVod.vodYear)
                    }
                    if (displayVod.vodArea.isNotBlank()) {
                        InfoChip(displayVod.vodArea)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 来源
                if (displayVod.sourceName.isNotBlank()) {
                    Text(
                        text = "来源: ${displayVod.sourceName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 简介
                if (displayVod.vodContent.isNotBlank()) {
                    Text(
                        text = displayVod.vodContent.replace("<[^>]+>".toRegex(), ""),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 播放区域
                when {
                    // 正在加载详情
                    isLoadingDetail -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("正在获取播放信息…", fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    // 有播放线路
                    playLines.isNotEmpty() -> {
                        Text("播放线路", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // 线路选择
                        if (playLines.size > 1) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(playLines.size) { index ->
                                    val line = playLines[index]
                                    FilterChip(
                                        selected = index == selectedLineIndex,
                                        onClick = { selectedLineIndex = index },
                                        label = { Text(line.flag, fontSize = 12.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 集数列表
                        val currentLine = playLines.getOrNull(selectedLineIndex)
                        if (currentLine != null && currentLine.episodes.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(currentLine.episodes.chunked(3)) { episodeRow ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        episodeRow.forEach { episode ->
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        onPlayEpisode(
                                                            displayVod,
                                                            currentLine.flag,
                                                            episode.url,
                                                            episode.name
                                                        )
                                                    },
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, null,
                                                        Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(episode.name, fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                        repeat(3 - episodeRow.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        } else {
                            Text("暂无播放源", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // 无播放信息且已加载完成
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无播放信息", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 关闭按钮
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
