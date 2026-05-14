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
    val portraitEnabled = intent.getBooleanExtra(EXTRA_PORTRAIT_UI, false)
    val shouldShowPortraitButtons = portraitEnabled && controlsVisible
    val shouldShowLandscapeRotate = !portraitEnabled && controlsVisible

    // 竖屏超分辨率按钮：淡入淡出动画
    findViewById<View>(R.id.btnAnime4KFloat)?.let { btn ->
        if (shouldShowPortraitButtons && btn.visibility != View.VISIBLE) {
            btn.visibility = View.VISIBLE
            btn.alpha = 0f
            btn.animate()
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else if (!shouldShowPortraitButtons && btn.visibility == View.VISIBLE) {
            btn.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { btn.visibility = View.GONE }
                .start()
        }
    }

    // 竖屏旋转按钮：淡入淡出动画
    findViewById<View>(R.id.btnRotateFloat)?.let { btn ->
        if (shouldShowPortraitButtons && btn.visibility != View.VISIBLE) {
            btn.visibility = View.VISIBLE
            btn.alpha = 0f
            btn.animate()
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else if (!shouldShowPortraitButtons && btn.visibility == View.VISIBLE) {
            btn.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { btn.visibility = View.GONE }
                .start()
        }
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
