package com.fam4k007.videoplayer

import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.view.View

private const val TAG = "VideoPlayerActivity"
private const val EXTRA_PORTRAIT_UI = "portrait_ui"
private const val EXTRA_AUTO_ROTATE = "auto_rotate"

internal fun VideoPlayerActivity.applyPortraitUiEnabled(enabled: Boolean) {
    if (!intent.getBooleanExtra(EXTRA_AUTO_ROTATE, false)) {
        requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    applyPortraitSizing(enabled)
}

internal fun VideoPlayerActivity.syncPortraitUiWithConfiguration(configuration: Configuration) {
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    intent.putExtra(EXTRA_PORTRAIT_UI, isPortrait)
    applyPortraitUiEnabled(isPortrait)
}

internal fun VideoPlayerActivity.applyPortraitSizing(enabled: Boolean) {
    updatePortraitFloatingButtonsVisibility(
        controlsManager.isVisible && !controlsManager.isControlsLocked()
    )
}

internal fun VideoPlayerActivity.updatePortraitFloatingButtonsVisibility(controlsVisible: Boolean) {
    // 竖屏超分辨率和旋转按钮已移至 Compose 控制面板层，不再使用老布局悬浮按钮
    findViewById<View>(R.id.btnAnime4KFloat)?.let { btn ->
        btn.animate().cancel()
        btn.visibility = View.GONE
    }
    findViewById<View>(R.id.btnRotateFloat)?.let { btn ->
        btn.animate().cancel()
        btn.visibility = View.GONE
    }
}

internal fun VideoPlayerActivity.refreshVideoLayoutAfterOrientationToggle() {
    mpvView.post {
        playbackEngine.changeVideoAspect(currentVideoAspect)
        listOf(
            R.id.surfaceView,
            R.id.danmakuView,
            R.id.clickArea,
            R.id.loadingIndicator
        ).forEach { id ->
            findViewById<View>(id)?.apply {
                requestLayout()
                invalidate()
            }
        }

        if (!isPlaying) {
            val pausedPosition = currentPosition.toInt().coerceAtLeast(0)
            mpvView.postDelayed({
                playbackEngine.seekTo(pausedPosition, precise = true)
                playbackEngine.pause()
            }, 120)
        }
    }
}
