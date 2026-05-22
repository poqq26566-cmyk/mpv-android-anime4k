package com.fam4k007.videoplayer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.VideoFileParcelable
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.remote.RemotePlaybackLauncher
import com.fam4k007.videoplayer.remote.RemotePlaybackRequest

/**
 * 播放器页面 - Compose入口
 * 
 * 由于VideoPlayerActivity使用MPV原生视图（CustomMPVView）、弹幕视图（DanmakuPlayerView）等
 * 原生Android View组件，且包含复杂的管理器交互（2200+行），目前保持Activity模式。
 * 
 * 此Screen提供Compose兼容的入口，自动启动VideoPlayerActivity：
 * - 统一参数传递接口
 * - 支持本地视频和在线视频
 * - 支持视频列表（连续播放）
 * - 为Stage 4 Navigation3迁移做准备
 */
@Composable
fun PlayerScreen(
    videoUri: String,
    videoTitle: String = "",
    onNavigateBack: () -> Unit,
    isOnlineVideo: Boolean = false,
    folderPath: String? = null,
    videoList: List<VideoFileParcelable>? = null,
    lastPosition: Long = -1L,
    cookies: String? = null,
    referer: String? = null,
    userAgent: String? = null,
    isWebDav: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 启动VideoPlayerActivity
    LaunchedEffect(videoUri) {
        PlayerLauncher.launch(
            context = context,
            videoUri = videoUri,
            videoTitle = videoTitle,
            isOnlineVideo = isOnlineVideo,
            folderPath = folderPath,
            videoList = videoList,
            lastPosition = lastPosition,
            cookies = cookies,
            referer = referer,
            userAgent = userAgent,
            isWebDav = isWebDav
        )
        // 启动后立即回退，避免显示空白页面
        onNavigateBack()
    }
    
    // 启动过程中显示黑色背景（与播放器一致）
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim),
        contentAlignment = Alignment.Center
    ) {
        // 保持空白，VideoPlayerActivity会接管显示
    }
}

/**
 * 播放器启动工具类
 * 统一管理VideoPlayerActivity的Intent构建和启动
 * 所有启动播放器的地方都应使用此类，确保参数一致性
 */
object PlayerLauncher {
    
    /**
     * 启动本地视频播放
     */
    fun launchLocal(
        context: Context,
        videoUri: Uri,
        videoTitle: String = "",
        folderPath: String? = null,
        videoList: List<VideoFileParcelable>? = null,
        lastPosition: Long = -1L
    ) {
        val intent = buildBaseIntent(context, videoUri).apply {
            if (videoTitle.isNotBlank()) {
                putExtra("video_name", videoTitle)
            }
            folderPath?.let { putExtra("folder_path", it) }
            if (lastPosition > 0) {
                putExtra("lastPosition", lastPosition)
            }
            videoList?.let {
                putParcelableArrayListExtra("video_list", ArrayList(it))
            }
        }
        startWithTransition(context, intent)
    }
    
    /**
     * 启动在线视频播放
     */
    fun launchOnline(
        context: Context,
        url: String,
        title: String = "",
        cookies: String? = null,
        referer: String? = null,
        userAgent: String? = null,
        source: RemotePlaybackRequest.Source = RemotePlaybackRequest.Source.UNKNOWN
    ) {
        val headers = linkedMapOf<String, String>().apply {
            cookies?.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
            referer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
            userAgent?.takeIf { it.isNotBlank() }?.let { put("User-Agent", it) }
        }
        
        val request = RemotePlaybackRequest(
            url = url,
            title = title,
            headers = headers,
            source = source
        )
        RemotePlaybackLauncher.start(context, request)
    }
    
    /**
     * 启动WebDAV视频播放
     */
    fun launchWebDav(
        context: Context,
        fileUrl: String,
        fileName: String = ""
    ) {
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            data = Uri.parse(fileUrl)
            action = Intent.ACTION_VIEW
            putExtra("is_webdav", true)
            if (fileName.isNotBlank()) {
                putExtra("file_name", fileName)
            }
        }
        startWithTransition(context, intent)
    }
    
    /**
     * 通用启动方法（兼容PlayerScreen参数）
     */
    fun launch(
        context: Context,
        videoUri: String,
        videoTitle: String = "",
        isOnlineVideo: Boolean = false,
        folderPath: String? = null,
        videoList: List<VideoFileParcelable>? = null,
        lastPosition: Long = -1L,
        cookies: String? = null,
        referer: String? = null,
        userAgent: String? = null,
        isWebDav: Boolean = false
    ) {
        when {
            isWebDav -> launchWebDav(context, videoUri, videoTitle)
            isOnlineVideo -> launchOnline(
                context = context,
                url = videoUri,
                title = videoTitle,
                cookies = cookies,
                referer = referer,
                userAgent = userAgent
            )
            else -> launchLocal(
                context = context,
                videoUri = Uri.parse(videoUri),
                videoTitle = videoTitle,
                folderPath = folderPath,
                videoList = videoList,
                lastPosition = lastPosition
            )
        }
    }
    
    private fun buildBaseIntent(context: Context, videoUri: Uri): Intent {
        return Intent(context, VideoPlayerActivity::class.java).apply {
            data = videoUri
        }
    }
    
    private fun startWithTransition(context: Context, intent: Intent) {
        context.startActivity(intent)
        (context as? Activity)?.overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }
}
