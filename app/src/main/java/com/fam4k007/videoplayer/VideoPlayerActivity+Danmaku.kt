package com.fam4k007.videoplayer

import android.net.Uri
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "VideoPlayerActivity"

/**
 * 加载视频对应的弹幕文件
 */
internal fun VideoPlayerActivity.loadDanmakuForVideo(videoUri: android.net.Uri) {
    try {
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Loading danmaku for video: $videoUri")

        val history = historyManager.getHistoryForUri(videoUri)

        // 如果历史记录中有弹幕路径且文件存在，恢复弹幕
        if (history?.danmuPath != null && File(history.danmuPath).exists()) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Restoring danmaku from history: ${history.danmuPath}")
            // 恢复用户上次的弹幕可见性设置(这会设置trackSelected状态)
            val autoShow = history.danmuVisible
            val loaded = danmakuManager.loadDanmakuFile(
                history.danmuPath,
                autoShow = autoShow
            )

            if (loaded) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Danmaku restored: path=${history.danmuPath}, trackSelected=$autoShow")

                // 同步Compose弹幕按钮状态
                viewModel.setDanmakuVisible(autoShow)
            } else {
                com.fam4k007.videoplayer.utils.Logger.w(TAG, "Failed to restore danmaku")
            }
        }
        // 只有当历史记录不存在时，才执行自动查找
        // 如果历史记录存在但 danmuPath 为 null，说明之前尝试过但没找到，不要重复尝试
        else if (history == null) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "No history found, trying auto-find...")
            autoFindAndLoadDanmaku(videoUri)
        } else {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "History exists but no danmaku path, skipping auto-find")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading danmaku", e)
        DialogUtils.showToastShort(this, "Danmaku loading failed: ${e.message}")
    }
}

/**
 * 自动查找并加载同名弹幕文件
 */
internal fun VideoPlayerActivity.autoFindAndLoadDanmaku(videoUri: android.net.Uri) {
    try {
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load danmaku start =====")
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video URI: $videoUri")

        // 【修复】使用 getRealPathFromUri 获取真实文件路径（而不是 fd:// 路径）
        val videoPath = getRealPathFromUri(videoUri)
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Real video path: $videoPath")

        if (videoPath != null) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Auto-finding danmaku for: $videoPath")

            // 自动加载弹幕并显示（autoShow = true），找到弹幕时自动显示
            // 切集时弹幕状态会通过历史记录中的 danmuVisible 恢复
            val loaded = danmakuManager.loadDanmakuForVideo(
                videoPath,
                autoShow = true  // 找到弹幕时自动显示
            )

            if (loaded) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "✓ Danmaku auto-loaded successfully")

                // 在主线程显示提示和更新UI
                runOnUiThread {
                    // 获取实际加载的弹幕文件名
                    val danmakuPath = danmakuManager.getCurrentDanmakuPath()
                    val fileName = danmakuPath?.substringAfterLast("/") ?: "danmaku file"

                    // 显示加载成功提示，提醒用户需要手动显示
                    DialogUtils.showToastShort(this, "Auto-loaded danmaku: $fileName")

                    // 根据实际的 trackSelected 状态更新Compose弹幕按钮
                    val isTrackSelected = danmakuManager.getTrackSelected()
                    viewModel.setDanmakuVisible(isTrackSelected)

                    com.fam4k007.videoplayer.utils.Logger.d(TAG, "Button state updated: trackSelected=$isTrackSelected")

                    // 【修复】保存弹幕信息到历史记录，避免下次重复自动加载
                    if (danmakuPath != null) {
                        historyManager.updateDanmu(
                            uri = videoUri,
                            danmuPath = danmakuPath,
                            danmuVisible = isTrackSelected,
                            danmuOffsetTime = 0L
                        )
                        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Danmu info saved to history: path=$danmakuPath")
                    }
                }
            } else {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "✗ No danmaku file found")
            }
        } else {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Cannot get real path from URI, skipping danmaku auto-load")
        }

        com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load danmaku end =====")
    } catch (e: Exception) {
        Log.e(TAG, "Error auto-finding danmaku", e)
        // 自动查找失败不提示，避免打扰用户
    }
}

/**
 * 加载网络弹幕
 */
internal fun VideoPlayerActivity.loadNetworkDanmaku(episodeId: Int, animeTitle: String, episodeTitle: String, serverUrl: String? = null) {
    lifecycleScope.launch {
        try {
            // 显示加载提示
            DialogUtils.showToastShort(this@loadNetworkDanmaku, "Loading danmaku...")

            // 使用指定的服务器获取弹幕
            val api = com.fam4k007.videoplayer.dandanplay.DanDanPlayApi(serverUrl)
            val result = api.getDanmaku(episodeId)

            result.fold(
                onSuccess = { danmakuResponse ->
                    // 转换为 XML 格式
                    val xmlContent = api.convertToXml(danmakuResponse)

                    // 保存到外部存储的 Android/data/包名/files/danmaku/network 目录
                    val danmakuDir = File(getExternalFilesDir(null), "danmaku/network")
                    if (!danmakuDir.exists()) {
                        danmakuDir.mkdirs()
                    }

                    // 使用番剧名和剧集名生成文件名（先清理特殊字符，再添加.xml后缀）
                    val cleanName = "${animeTitle}_${episodeTitle}_${episodeId}"
                        .replace("[^a-zA-Z0-9_\\u4e00-\\u9fa5]".toRegex(), "_")
                    val fileName = "$cleanName.xml"
                    val danmakuFile = File(danmakuDir, fileName)
                    danmakuFile.writeText(xmlContent)

                    Logger.d(TAG, "网络弹幕已保存到: ${danmakuFile.absolutePath}")

                    // 加载弹幕（状态机自动处理启动时序）
                    val loaded = danmakuManager.loadDanmakuFile(danmakuFile.absolutePath, autoShow = true)

                    if (loaded) {
                        // 同步弹幕到当前播放位置
                        val currentPosition = (playbackEngine.currentPosition * 1000).toLong()
                        danmakuManager.seekTo(currentPosition)
                        Logger.d(TAG, "Network danmaku loaded and synced to position: $currentPosition (state machine handles start)")

                        DialogUtils.showToastShort(
                            this@loadNetworkDanmaku,
                            "Danmaku loaded: $animeTitle - $episodeTitle"
                        )

                        // 更新历史记录
                        videoUri?.let { uri ->
                            historyManager.updateDanmu(
                                uri = uri,
                                danmuPath = danmakuFile.absolutePath,
                                danmuVisible = true,
                                danmuOffsetTime = 0L
                            )
                            Logger.d(TAG, "Network danmaku updated in history")
                        }
                    } else {
                        DialogUtils.showToastLong(this@loadNetworkDanmaku, "Danmaku loading failed")
                    }
                },
                onFailure = { e ->
                    DialogUtils.showToastLong(
                        this@loadNetworkDanmaku,
                        "Danmaku loading failed: ${e.message}"
                    )
                }
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load network danmaku", e)
            DialogUtils.showToastLong(this@loadNetworkDanmaku, "Danmaku loading error: ${e.message}")
        }
    }
}

/**
 * 显示匹配结果选择对话框
 */
internal fun VideoPlayerActivity.showMatchSelectionDialog(results: List<com.fam4k007.videoplayer.dandanplay.ServerMatchResult>) {
    val items = results.map { "[${it.serverName}] ${it.matchInfo.animeTitle} - ${it.matchInfo.episodeTitle}" }.toTypedArray()

    androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Select Match")
        .setItems(items) { dialog, which ->
            val result = results[which]
            loadNetworkDanmaku(result.matchInfo.episodeId, result.matchInfo.animeTitle, result.matchInfo.episodeTitle, result.serverUrl)
            dialog.dismiss()
        }
        .setNegativeButton("Cancel", null)
        .show()
}
