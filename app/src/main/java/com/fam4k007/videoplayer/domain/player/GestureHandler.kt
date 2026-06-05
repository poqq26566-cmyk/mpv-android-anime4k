package com.fam4k007.videoplayer.domain.player

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import `is`.xyz.mpv.MPVLib
import java.lang.ref.WeakReference

/**
 * 系统音量和亮度状态管理器
 * 手势识别已迁移到 Compose 层 (ui/player/GestureHandler.kt)，
 * 本类仅负责进入/退出播放器时保存和恢复系统音量和亮度设置。
 */
class GestureHandler(
    private val contextRef: WeakReference<Context>,
    private val windowRef: WeakReference<android.view.Window>
) {

    companion object {
        private const val TAG = "GestureHandler"

        // 音量配置
        private const val MIN_VOLUME = 0.1f
        private const val MAX_VOLUME = 300f
        private const val MAX_VOLUME_NO_BOOST = 100f
        private const val DEFAULT_VOLUME = 100f

        // 偏好设置键名
        private const val PREF_NAME = "player_preferences"
        private const val KEY_PRECISE_SEEKING = "precise_seeking"
        private const val KEY_VOLUME_BOOST_ENABLED = "volume_boost_enabled"
        private const val KEY_CONTROL_SYSTEM_VOLUME = "control_system_volume"
    }

    private var audioManager: AudioManager? = null
    private var maxSystemVolume = 0
    private var maxBrightness = 255

    // 当前值
    private var currentVolume = DEFAULT_VOLUME
    private var currentBrightness = 0.5f

    // 设置缓存
    private var usePreciseSeeking = false
    private var volumeBoostEnabled = false
    private var controlSystemVolume = false

    // 原始系统设置（退出时恢复）
    private var originalSystemVolume = -1
    private var originalSystemBrightness = -1f
    private var originalMPVVolumePercent = DEFAULT_VOLUME

    /**
     * 初始化 — 读取系统音量和亮度并保存原始值
     */
    fun initialize() {
        val context = contextRef.get() ?: return
        val window = windowRef.get() ?: return

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxSystemVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        usePreciseSeeking = prefs.getBoolean(KEY_PRECISE_SEEKING, false)
        volumeBoostEnabled = prefs.getBoolean(KEY_VOLUME_BOOST_ENABLED, false)
        controlSystemVolume = prefs.getBoolean(KEY_CONTROL_SYSTEM_VOLUME, false)

        val systemVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val systemVolumePercent = (systemVolume.toFloat() / maxSystemVolume * 100f)
            .coerceIn(MIN_VOLUME, MAX_VOLUME_NO_BOOST)

        originalSystemVolume = systemVolume
        currentVolume = systemVolumePercent
        originalMPVVolumePercent = systemVolumePercent

        try {
            MPVLib.setPropertyInt("volume", 100)
            Log.d(TAG, "Init - System: $systemVolume/$maxSystemVolume ($systemVolumePercent%), MPV: 100%, boost: $volumeBoostEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set initial MPV volume", e)
        }

        originalSystemBrightness = window.attributes.screenBrightness

        if (originalSystemBrightness < 0) {
            try {
                val systemBrightness = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                ).toFloat()
                currentBrightness = systemBrightness / maxBrightness
                originalSystemBrightness = currentBrightness
            } catch (e: Exception) {
                currentBrightness = 0.5f
                originalSystemBrightness = 0.5f
            }
        } else {
            currentBrightness = originalSystemBrightness
        }

        Log.d(TAG, "Initialized - MPV Volume: ${currentVolume.toInt()}%, Brightness: $currentBrightness, VolumeBoost: $volumeBoostEnabled")
    }

    /**
     * 重新应用音量增强设置（从后台返回时调用）
     */
    fun reapplyVolumeBoostSettings() {
        val context = contextRef.get() ?: return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        volumeBoostEnabled = prefs.getBoolean(KEY_VOLUME_BOOST_ENABLED, false)

        if (volumeBoostEnabled) {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxSystemVolume, 0)
            Log.d(TAG, "Reapplied VolumeBoost - System volume set to MAX")
        } else {
            Log.d(TAG, "Reapplied normal mode - System volume unchanged")
        }
    }

    /**
     * 获取精确进度控制设置
     */
    fun isPreciseSeekingEnabled(): Boolean {
        val context = contextRef.get() ?: return usePreciseSeeking
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PRECISE_SEEKING, false)
    }

    /**
     * 设置精确进度控制
     */
    fun setPreciseSeeking(enabled: Boolean) {
        usePreciseSeeking = enabled
        val context = contextRef.get() ?: return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PRECISE_SEEKING, enabled).apply()
        Log.d(TAG, "Precise seeking ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * 恢复原始系统设置（退出播放器时调用）
     */
    fun restoreOriginalSettings() {
        try {
            if (!controlSystemVolume) {
                try {
                    MPVLib.setPropertyInt("volume", originalMPVVolumePercent.toInt())
                    Log.d(TAG, "Restored MPV volume to original: ${originalMPVVolumePercent.toInt()}%")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore MPV volume", e)
                }

                if (originalSystemVolume >= 0) {
                    try {
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalSystemVolume, 0)
                        Log.d(TAG, "Restored system volume: $originalSystemVolume/$maxSystemVolume")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restore system volume", e)
                    }
                }
            } else {
                Log.d(TAG, "Control system volume enabled, skipping volume restore")
            }

            if (originalSystemBrightness >= 0) {
                try {
                    windowRef.get()?.let { window ->
                        val layoutParams = window.attributes
                        layoutParams.screenBrightness = originalSystemBrightness
                        window.attributes = layoutParams
                        Log.d(TAG, "Restored original brightness: $originalSystemBrightness")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore original brightness", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during restoreOriginalSettings", e)
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        audioManager = null
        Log.d(TAG, "Cleanup completed")
    }
}
