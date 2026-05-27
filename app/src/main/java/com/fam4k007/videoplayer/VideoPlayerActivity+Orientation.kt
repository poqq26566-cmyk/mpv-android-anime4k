package com.fam4k007.videoplayer

import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.view.View

private const val TAG = "VideoPlayerActivity"
internal const val EXTRA_PORTRAIT_UI = "portrait_ui"
internal const val EXTRA_AUTO_ROTATE = "auto_rotate"

internal fun VideoPlayerActivity.applyPortraitUiEnabled(enabled: Boolean) {
    if (!intent.getBooleanExtra(EXTRA_AUTO_ROTATE, false)) {
        requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
}

internal fun VideoPlayerActivity.syncPortraitUiWithConfiguration(configuration: Configuration) {
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    intent.putExtra(EXTRA_PORTRAIT_UI, isPortrait)
    applyPortraitUiEnabled(isPortrait)
}

internal fun VideoPlayerActivity.refreshVideoLayoutAfterOrientationToggle() {
    mpvView.post {
        playbackEngine.changeVideoAspect(currentVideoAspect)
        listOf(
            R.id.surfaceView,
            R.id.danmakuView,
            R.id.clickArea
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
