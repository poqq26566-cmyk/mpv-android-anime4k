package com.fam4k007.videoplayer.tvbox.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.tvbox.model.VodInfo
import com.fam4k007.videoplayer.tvbox.viewmodel.TvBoxSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TvBoxSearchContainerScreen(
    viewModel: TvBoxSearchViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedVod by remember { mutableStateOf<VodInfo?>(null) }
    val scope = rememberCoroutineScope()

    // 免责声明弹窗
    val prefs = remember { context.getSharedPreferences("tvbox_disclaimer", android.content.Context.MODE_PRIVATE) }
    var showDisclaimer by remember { mutableStateOf(!prefs.getBoolean("dismissed", false)) }

    if (showDisclaimer) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDisclaimer = false },
            title = {
                androidx.compose.material3.Text(
                    "TVBox 功能说明",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            },
            text = {
                androidx.compose.material3.Text(
                    "TVBox 的接入由于技术问题，存在以下限制，请悉知：\n\n" +
                        "1. 仅支持在线播放，不支持设置网盘 Cookie 进行网盘播放\n" +
                        "2. 点击影片后无反应请耐心等待，搜索尚未结束时请先手动停止搜索再尝试播放，否则可能闪退\n" +
                        "3. 若遇到其他问题无需反馈，技术有限暂无法修复\n\n" +
                        "强烈建议直接下载原版 TVBox 使用，功能更强大！",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { showDisclaimer = false }) {
                    androidx.compose.material3.Text("确定")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    prefs.edit().putBoolean("dismissed", true).apply()
                    showDisclaimer = false
                }) {
                    androidx.compose.material3.Text("不再提示")
                }
            }
        )
    }

    TvBoxSearchScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSearchKeywordChange = { viewModel.updateSearchKeyword(it) },
        onSearch = { viewModel.searchAll() },
        onStopSearch = { viewModel.stopSearch() },
        onSelectSite = { viewModel.selectSite(it) },
        onConfigUrlChange = { viewModel.updateConfigUrl(it) },
        onLoadConfig = { viewModel.loadConfig(it) },
        onRemoveUrl = { viewModel.removeUrl(it) },
        onToggleConfigDialog = { viewModel.toggleConfigDialog(it) },
        onClearError = { viewModel.clearError() },
        onClickVod = { vod ->
            selectedVod = vod
            viewModel.loadDetail(vod)
        }
    )

    selectedVod?.let { vod ->
        VodDetailDialog(
            vod = vod,
            isLoadingDetail = uiState.isLoadingDetail,
            detailVod = uiState.detailVod,
            onDismiss = {
                selectedVod = null
                viewModel.clearDetail()
            },
            onPlayEpisode = { playVod, flag, episodeUrl, episodeName ->
                selectedVod = null
                viewModel.clearDetail()
                // 先调用 spider.playerContent() 获取真实播放地址
                scope.launch {
                    try {
                        val playerUrlResult = viewModel.getPlayerUrl(playVod, flag, episodeUrl)
                        playerUrlResult.fold(
                            onSuccess = { result ->
                                withContext(Dispatchers.Main) {
                                    val url = result.url
                                    val headers = result.header
                                    val title = "${playVod.vodName} - $episodeName"
                                    val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                        data = Uri.parse(url)
                                        putExtra("video_title", title)
                                        // 传递 header（播放器读取 referer/user_agent/cookies）
                                        headers.forEach { (key, value) ->
                                            when (key.lowercase()) {
                                                "referer" -> putExtra("referer", value)
                                                "user-agent", "user_agent" -> putExtra("user_agent", value)
                                                "cookie" -> putExtra("cookies", value)
                                            }
                                        }
                                    }
                                    context.startActivity(intent)
                                    (context as? android.app.Activity)?.overridePendingTransition(
                                        com.fam4k007.videoplayer.R.anim.slide_in_right,
                                        com.fam4k007.videoplayer.R.anim.slide_out_left
                                    )
                                }
                            },
                            onFailure = { e ->
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}
