package com.fam4k007.videoplayer

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.remote.RemotePlaybackHeaders
import com.fam4k007.videoplayer.remote.RemotePlaybackRequest
import com.fam4k007.videoplayer.remote.RemotePlaybackResolver
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.Logger
import com.fam4k007.videoplayer.utils.UriUtils.getFolderName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "VideoPlayerActivity"
private val REMOTE_URI_SCHEMES = setOf("http", "https", "rtsp", "rtmp", "rtmps")

/**
 * 加载视频
 */
internal fun VideoPlayerActivity.loadVideo() {
    videoUri?.let { uri ->
        // 重置自动加载字幕标志（新视频开始播放）
        viewModel.setHasAutoLoadedSubtitle(false)

        val position = if (duration > 0 && duration < 30) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Short video detected, starting from 0")
            0.0
        } else {
            savedPosition
        }

        if (position > 5.0) {
            viewModel.showResumeToast()
        }

        // 对于在线视频,直接使用URI字符串;对于本地文件,使用URI对象
        if (isOnlineVideo) {
            val request = remotePlaybackRequest ?: buildLegacyRemotePlaybackRequest(uri)
            remotePlaybackRequest = request
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Loading online video with request: ${request.url}")
            loadResolvedRemoteVideo(request, position)
        } else {
            // 本地视频:使用URI对象
            playbackEngine?.loadVideo(uri, position)
            applyRememberedSpeed()
        }

        loadDanmakuForVideo(uri)

        // 只在position > 0时同步弹幕(延迟更长,等待在线视频加载)
        if (position > 0) {
            lifecycleScope.launch {
                // 在线视频需要更长延迟
                val delayTime = if (isOnlineVideo) 3000L else 800L
                delay(delayTime)
                danmakuManager.seekTo((position * 1000).toLong())
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Synced danmaku to position: $position seconds")
            }
        }

        // 使用协程延迟恢复字幕设置和Anime4K效果
        lifecycleScope.launch {
            delay(500)

            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Post-load coroutine started, isOnlineVideo=$isOnlineVideo")

            // 先尝试自动加载同名字幕（本地视频，包括content://）
            if (!isOnlineVideo) {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Calling autoLoadSubtitleIfExists")
                autoLoadSubtitleIfExists(uri)
            } else {
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Skipping subtitle auto-load for online video")
            }

            // 再恢复用户的字幕偏好设置（会覆盖自动加载的）
            restoreSubtitlePreferences(uri)

            // 如果记忆的Anime4K模式已启用，在视频加载后应用shader
            if (viewModel.anime4KEnabled.value && anime4KMode != com.fam4k007.videoplayer.domain.player.Anime4KManager.Mode.OFF) {
                delay(200) // 额外延迟确保MPV完全初始化
                applyAnime4K()
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Applied remembered Anime4K mode: $anime4KMode")
            }
        }
    }
}

/**
 * 应用记忆的播放倍速
 */
internal fun VideoPlayerActivity.applyRememberedSpeed() {
    if (preferencesManager.isRememberSpeedEnabled()) {
        val lastSpeed = preferencesManager.getLastPlaybackSpeed()
        if (lastSpeed != 1.0f) {
            viewModel.setSpeed(lastSpeed.toDouble())
            playbackEngine.setSpeed(lastSpeed.toDouble())
            danmakuManager.setSpeed(lastSpeed)
            Logger.d(TAG, "Restored remembered playback speed: ${lastSpeed}x")
        }
    }
}

internal fun VideoPlayerActivity.loadResolvedRemoteVideo(
    request: RemotePlaybackRequest,
    position: Double
) {
    remoteResolveJob?.cancel()
    val sequence = ++remoteResolveSequence
    remoteResolveJob = lifecycleScope.launch {
        val result = RemotePlaybackResolver.resolve(request)
        val debugSummary = RemotePlaybackResolver.buildDebugSummary(result)
        preferencesManager.setLastRemoteDebugSummary(debugSummary)
        Logger.d(TAG, "Remote debug summary:\n$debugSummary")
        if (sequence != remoteResolveSequence) {
            Logger.d(TAG, "Discarding stale remote resolve result for: ${request.url}")
            return@launch
        }

        when (result) {
            is RemotePlaybackResolver.ResolveResult.Success -> {
                remotePlaybackRequest = result.request
                videoUri = Uri.parse(result.request.url)
                Logger.d(
                    TAG,
                    "Remote request resolved via ${result.probeMethod}: ${request.url} -> ${result.request.url}"
                )
                playbackEngine.loadRemote(result.request, position)
            }
            is RemotePlaybackResolver.ResolveResult.Failed -> {
                remotePlaybackRequest = result.request
                Logger.w(TAG, "Remote resolve fallback: reason=${result.reason}, message=${result.message}", result.cause)
                val suggestion = RemotePlaybackResolver.buildFailureSuggestion(result.reason)
                DialogUtils.showToastLong(
                    this@loadResolvedRemoteVideo,
                    "${result.message}，继续尝试直接播放\n$suggestion"
                )
                playbackEngine.loadRemote(result.request, position)
            }
        }
        applyRememberedSpeed()
    }
}

/**
 * 处理从列表传入的视频数据
 */
internal fun VideoPlayerActivity.handleVideoListIntent() {
    val lastPosition = intent.getLongExtra("lastPosition", -1L)
    if (lastPosition > 0) {
        viewModel.setLastPlaybackPosition(lastPosition)
    }
}

/**
 * 播放上一集
 */
internal fun VideoPlayerActivity.playPreviousVideo() {
    Logger.d(TAG, "playPreviousVideo called")
    viewModel.previousVideo()
}

/**
 * 播放下一集
 */
internal fun VideoPlayerActivity.playNextVideo() {
    Logger.d(TAG, "playNextVideo called")
    viewModel.nextVideo()
}

/**
 * 播放指定视频
 */
internal fun VideoPlayerActivity.playVideo(uri: Uri) {
    videoUri = uri

    // 使用 ViewModel 的 currentIndex 同步 seriesManager（避免 URI 字符串比较不匹配）
    val vmIndex = viewModel.currentIndex.value
    if (vmIndex >= 0 && vmIndex < seriesManager.getVideoList().size) {
        seriesManager.syncIndex(vmIndex)
    } else {
        // 回退到 URI 匹配
        seriesManager.switchToVideo(uri)
    }

    // 重置自动加载字幕标志（新视频开始播放）
    viewModel.setHasAutoLoadedSubtitle(false)

    // 更新在线视频标志
    val isOnline = isRemotePlaybackUri(uri)
    viewModel.setIsOnlineVideo(isOnline)
    remotePlaybackRequest = if (isOnline) {
        RemotePlaybackRequest(
            url = uri.toString(),
            title = resolveVideoTitle(uri),
            source = RemotePlaybackRequest.Source.UNKNOWN
        )
    } else {
        null
    }

    // 显示加载动画（仅在线视频）
    if (isOnlineVideo) {
        loadingIndicator.visibility = View.VISIBLE
        loadingIndicator.alpha = 1f
    } else {
        loadingIndicator.visibility = View.GONE
    }

    val fileName = getFileNameFromUri(uri)
    viewModel.setVideoTitle(fileName)

    val position = preferencesManager.getPlaybackPosition(uri.toString())

    // 【重要】清除旧弹幕，避免切换视频时弹幕残留
    Logger.d(TAG, "Releasing old danmaku before playing new video")
    danmakuManager.release()

    if (isOnlineVideo) {
        loadResolvedRemoteVideo(remotePlaybackRequest!!, position)
    } else {
        playbackEngine?.loadVideo(uri, position)
        applyRememberedSpeed()
    }

    // 设置当前视频 URI 给文件选择器管理器
    filePickerManager.setCurrentVideoUri(uri)

    // 自动加载字幕和弹幕（和初始播放一样的逻辑）
    lifecycleScope.launch {
        delay(500)

        // 本地视频自动加载同名字幕
        if (!isOnlineVideo) {
            Logger.d(TAG, "Auto-loading subtitle for next video")
            autoLoadSubtitleIfExists(uri)
        }

        // 恢复字幕偏好设置
        restoreSubtitlePreferences(uri)

        // 【重要】加载新视频的弹幕
        Logger.d(TAG, "Loading danmaku for new video: $uri")
        loadDanmakuForVideo(uri)

        // 同步弹幕到播放位置
        if (position > 0) {
            delay(300)
            danmakuManager.seekTo((position * 1000).toLong())
            Logger.d(TAG, "Synced danmaku to position: $position seconds")
        }
    }
}

internal fun VideoPlayerActivity.buildLegacyRemotePlaybackRequest(uri: Uri): RemotePlaybackRequest {
    val headers = linkedMapOf<String, String>()
    intent.getStringExtra("cookies")
        ?.takeIf { it.isNotBlank() }
        ?.let { headers["Cookie"] = it }
    intent.getStringExtra("referer")
        ?.takeIf { it.isNotBlank() }
        ?.let { headers["Referer"] = it }
    intent.getStringExtra("user_agent")
        ?.takeIf { it.isNotBlank() }
        ?.let { headers["User-Agent"] = it }

    val urlString = uri.toString()
    if (urlString.contains("bilivideo.com") && headers["Referer"].isNullOrBlank()) {
        headers["Referer"] = "https://www.bilibili.com"
    }

    val source = when {
        intent.hasExtra("cookies") -> RemotePlaybackRequest.Source.BILIBILI
        intent.getBooleanExtra("is_webdav", false) -> RemotePlaybackRequest.Source.WEBDAV
        else -> RemotePlaybackRequest.Source.UNKNOWN
    }

    return RemotePlaybackRequest(
        url = urlString,
        title = resolveVideoTitle(uri),
        sourcePageUrl = RemotePlaybackHeaders.deriveSourcePageUrl(headers),
        headers = RemotePlaybackHeaders.normalize(headers),
        source = source
    )
}

internal fun VideoPlayerActivity.isRemotePlaybackUri(uri: Uri?): Boolean {
    val scheme = uri?.scheme?.lowercase().orEmpty()
    return scheme in REMOTE_URI_SCHEMES
}

internal fun VideoPlayerActivity.resolveVideoTitle(uri: Uri): String {
    remotePlaybackRequest?.title
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    val intentTitle = listOf("video_title", "title", "file_name", "video_name")
        .asSequence()
        .mapNotNull { key -> intent.getStringExtra(key) }
        .firstOrNull { it.isNotBlank() }
    if (!intentTitle.isNullOrBlank()) {
        return intentTitle
    }

    if (isRemotePlaybackUri(uri)) {
        val remoteName = uri.lastPathSegment
            ?.substringBefore("?")
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.decode(it) }
        return remoteName ?: uri.host ?: "在线视频"
    }

    return try {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    it.getString(nameIndex)
                } else {
                    uri.lastPathSegment ?: "未知文件"
                }
            } else {
                uri.lastPathSegment ?: "未知文件"
            }
        } ?: uri.lastPathSegment ?: "未知文件"
    } catch (e: Exception) {
        Logger.w(TAG, "Failed to resolve file name from uri: $uri", e)
        uri.lastPathSegment ?: "未知文件"
    }
}

internal fun VideoPlayerActivity.getFileNameFromUri(uri: Uri): String {
    return resolveVideoTitle(uri)
}

internal fun VideoPlayerActivity.savePlaybackState() {
    val uri = videoUri ?: return

    try {
        // 1. 保存播放进度到 PreferencesManager
        if (duration > 0 && currentPosition > 0) {
            preferencesManager.setPlaybackPosition(uri.toString(), currentPosition)
            Logger.d(TAG, "Playback position saved: $currentPosition / $duration")
        }

        // 2. 添加到历史记录 - 只记录本地视频，不记录在线视频
        if (duration > 0 && !isOnlineVideo) {  // 添加 !isOnlineVideo 判断
            val fileName = getFileNameFromUri(uri)
            val folderName = uri.getFolderName()

            historyManager.addHistory(
                uri = uri,
                fileName = fileName,
                position = (currentPosition * 1000).toLong(),
                duration = (duration * 1000).toLong(),
                folderName = folderName
            )
            Logger.d(TAG, "History saved: $fileName")

            // 3. 保存弹幕信息到历史记录(保存真实的显示状态)
            val danmakuPath = danmakuManager.getCurrentDanmakuPath()
            if (danmakuPath != null) {
                // 只有prepared且visible时才保存为true
                val actualVisible = danmakuManager.isVisible() && danmakuManager.isPrepared()
                historyManager.updateDanmu(
                    uri = uri,
                    danmuPath = danmakuPath,
                    danmuVisible = actualVisible,
                    danmuOffsetTime = 0L
                )
                Logger.d(TAG, "Danmu info saved: path=$danmakuPath, visible=$actualVisible")
            }
        } else if (isOnlineVideo) {
            Logger.d(TAG, "Skipping history for online video: $uri")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save playback state", e)
    }
}
