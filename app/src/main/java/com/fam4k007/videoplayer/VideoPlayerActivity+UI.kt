package com.fam4k007.videoplayer

import android.content.Intent
import android.util.Log
import android.view.View
import com.fam4k007.videoplayer.domain.player.Anime4KManager
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.Logger

private const val TAG = "VideoPlayerActivity"

/**
 * 加载用户设置
 */
internal fun VideoPlayerActivity.loadUserSettings() {
    val seekTime = preferencesManager.getSeekTime()
    viewModel.setSeekTimeSeconds(seekTime)
    Log.d(TAG, "loadUserSettings: seekTimeSeconds loaded = $seekTime seconds")

    // 如果启用了Anime4K记忆功能，恢复上次使用的模式
    if (preferencesManager.isAnime4KMemoryEnabled()) {
        val lastMode = preferencesManager.getLastAnime4KMode()
        try {
            anime4KMode = Anime4KManager.Mode.valueOf(lastMode)
            // 只有非OFF模式才启用Anime4K
            val enabled = (anime4KMode != Anime4KManager.Mode.OFF)
            viewModel.setAnime4K(enabled, anime4KMode, viewModel.anime4KQuality.value)
            Logger.d(TAG, "Anime4K mode restored from memory: $lastMode, enabled=$enabled")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid Anime4K mode in preferences: $lastMode", e)
            anime4KMode = Anime4KManager.Mode.OFF
            viewModel.setAnime4K(false, Anime4KManager.Mode.OFF, viewModel.anime4KQuality.value)
        }
    } else {
        anime4KMode = Anime4KManager.Mode.OFF
        viewModel.setAnime4K(false, Anime4KManager.Mode.OFF, viewModel.anime4KQuality.value)
    }
}

/**
 * 显示暂停指示器（方案A：缩放+透明度动画，2秒后自动隐藏）
 */
internal fun VideoPlayerActivity.showPauseIndicator() {
    // 取消之前的自动隐藏任务
    pauseIndicatorHideRunnable?.let { pauseIndicatorHandler.removeCallbacks(it) }

    pauseIndicator.apply {
        visibility = View.VISIBLE
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f

        // 入场动画：缩放（0.8x→1.0x）+ 透明度（0→0.9）
        animate()
            .alpha(0.9f)  // 半透明，不完全不透明
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(300)  // 300ms动画
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    // 2秒后自动隐藏
    pauseIndicatorHideRunnable = Runnable {
        hidePauseIndicator()
    }
    pauseIndicatorHandler.postDelayed(pauseIndicatorHideRunnable!!, 2000)
}

/**
 * 隐藏暂停指示器（淡出动画）
 */
internal fun VideoPlayerActivity.hidePauseIndicator() {
    // 取消自动隐藏任务
    pauseIndicatorHideRunnable?.let { pauseIndicatorHandler.removeCallbacks(it) }

    pauseIndicator.animate()
        .alpha(0f)
        .setDuration(300)  // 淡出300ms
        .setInterpolator(android.view.animation.AccelerateInterpolator())
        .withEndAction {
            pauseIndicator.visibility = View.GONE
            // 重置状态
            pauseIndicator.scaleX = 0.8f
            pauseIndicator.scaleY = 0.8f
        }
        .start()
}

/**
 * 显示速度变化提示
 */
internal fun VideoPlayerActivity.showSpeedChangeHint(speed: Float) {
    speedHintText?.text = String.format("%.1f倍速", speed)
    speedHint?.apply {
        visibility = View.VISIBLE
        alpha = 0f

        // 入场动画
        animate()
            .alpha(1.0f)
            .setDuration(200)
            .withEndAction {
                // 2秒后自动隐藏
                postDelayed({
                    animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction { visibility = View.GONE }
                        .start()
                }, 2000)
            }
            .start()
    }
}

internal fun VideoPlayerActivity.applyAnime4K() {
    if (viewModel.anime4KEnabled.value) {
        val shaderString = anime4KManager.getShaderChain(anime4KMode, viewModel.anime4KQuality.value)
        val shaders = if (shaderString.isNotEmpty()) {
            shaderString.split(":")
        } else {
            emptyList()
        }
        playbackEngine.setShaderList(shaders)
        Logger.d(TAG, "Anime4K applied: mode=$anime4KMode, quality=${viewModel.anime4KQuality.value}")
    } else {
        playbackEngine.setShaderList(emptyList())
        Logger.d(TAG, "Anime4K disabled")
    }
}

/**
 * 显示视频列表抽屉
 */
internal fun VideoPlayerActivity.showVideoListDrawer() {
    // 如果没有视频列表，提示用户
    if (currentVideoList.isEmpty()) {
        DialogUtils.showToastShort(this, "当前没有可用的视频列表")
        return
    }

    videoUri?.let { uri ->
        composeOverlayManager.showVideoListDrawer(
            videoList = currentVideoList,
            currentVideoUri = uri,
            onVideoSelected = { video, index ->
                // 切换到选中的视频
                val selectedUri = android.net.Uri.parse(video.uri)
                Logger.d(TAG, "Video selected from list: ${video.name}, index: $index")
                playVideo(selectedUri)
            }
        )
    }
}

/**
 * 处理返回导航
 */
internal fun VideoPlayerActivity.handleBackNavigation() {
    gestureHandler?.restoreOriginalSettings()

    if (viewModel.isFromHomeContinue.value) {
        // 从主页继续播放进入，直接返回到主页（MainActivity）
        Logger.d(TAG, "Returning to MainActivity from continue play")

        val intent = Intent(this, MainActivity::class.java).apply {
            // 清除任务栈，确保回到主页
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    } else {
        // 正常返回
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
