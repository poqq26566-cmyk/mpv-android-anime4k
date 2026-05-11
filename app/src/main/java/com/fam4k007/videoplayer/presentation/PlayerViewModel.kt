package com.fam4k007.videoplayer.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam4k007.videoplayer.database.PlaybackHistoryEntity
import com.fam4k007.videoplayer.repository.PlayerRepository
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 播放器ViewModel
 * 管理播放器相关的UI状态，调用Repository和Domain层
 */
class PlayerViewModel(
    private val playerRepository: PlayerRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "PlayerViewModel"
    }
    
    // ==================== UI State ====================
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _playbackHistory = MutableStateFlow<PlaybackHistoryEntity?>(null)
    val playbackHistory: StateFlow<PlaybackHistoryEntity?> = _playbackHistory.asStateFlow()
    
    private val _playbackSettings = MutableStateFlow(PlaybackSettings())
    val playbackSettings: StateFlow<PlaybackSettings> = _playbackSettings.asStateFlow()
    
    // ==================== 播放状态更新 ====================
    
    fun updatePlaybackPosition(position: Long, duration: Long) {
        _playbackState.value = _playbackState.value.copy(
            position = position,
            duration = duration
        )
    }
    
    fun updatePlayingState(isPlaying: Boolean) {
        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
    }
    
    fun updateSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(speed = speed)
    }
    
    fun updateBufferingState(isBuffering: Boolean) {
        _playbackState.value = _playbackState.value.copy(isBuffering = isBuffering)
    }
    
    fun updateHardwareDecoding(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(isHardwareDecoding = enabled)
    }
    
    // ==================== 播放历史管理 ====================
    
    /**
     * 加载指定视频的播放历史
     */
    fun loadPlaybackHistory(uri: Uri) {
        viewModelScope.launch {
            try {
                val history = playerRepository.getPlaybackHistory(uri)
                _playbackHistory.value = history
                Logger.d(TAG, "Loaded playback history for $uri: position=${history?.position}")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load playback history", e)
                _playbackHistory.value = null
            }
        }
    }
    
    /**
     * 保存播放历史
     */
    fun savePlaybackHistory(
        uri: Uri,
        fileName: String,
        position: Long,
        duration: Long,
        folderName: String,
        danmuPath: String? = null,
        danmuVisible: Boolean = true,
        danmuOffsetTime: Long = 0L,
        thumbnailPath: String? = null
    ) {
        viewModelScope.launch {
            try {
                playerRepository.savePlaybackHistory(
                    uri = uri,
                    fileName = fileName,
                    position = position,
                    duration = duration,
                    folderName = folderName,
                    danmuPath = danmuPath,
                    danmuVisible = danmuVisible,
                    danmuOffsetTime = danmuOffsetTime,
                    thumbnailPath = thumbnailPath
                )
                Logger.d(TAG, "Saved playback history: $fileName at $position")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to save playback history", e)
            }
        }
    }
    
    /**
     * 删除播放历史
     */
    fun deletePlaybackHistory(uri: Uri) {
        viewModelScope.launch {
            try {
                playerRepository.deletePlaybackHistory(uri)
                _playbackHistory.value = null
                Logger.d(TAG, "Deleted playback history for $uri")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to delete playback history", e)
            }
        }
    }
    
    /**
     * 清除所有播放历史
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                playerRepository.clearAllPlaybackHistory()
                _playbackHistory.value = null
                Logger.d(TAG, "Cleared all playback history")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to clear all history", e)
            }
        }
    }
    
    // ==================== 播放器设置管理 ====================
    
    /**
     * 加载播放器设置
     */
    fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = PlaybackSettings(
                    seekTimeSeconds = playerRepository.getSeekTimeSeconds(),
                    rememberPosition = playerRepository.isRememberPositionEnabled(),
                    rememberSpeed = playerRepository.isRememberSpeedEnabled(),
                    rememberBrightness = playerRepository.isRememberBrightnessEnabled(),
                    autoLoadDanmaku = playerRepository.isAutoLoadDanmakuEnabled(),
                    hardwareDecoding = playerRepository.isHardwareDecodingEnabled(),
                    gestureControlEnabled = playerRepository.isGestureControlEnabled()
                )
                _playbackSettings.value = settings
                Logger.d(TAG, "Loaded playback settings")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load settings", e)
            }
        }
    }
    
    /**
     * 更新快进/快退时长
     */
    fun updateSeekTimeSeconds(seconds: Int) {
        viewModelScope.launch {
            try {
                playerRepository.setSeekTimeSeconds(seconds)
                _playbackSettings.value = _playbackSettings.value.copy(seekTimeSeconds = seconds)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to update seek time", e)
            }
        }
    }
    
    /**
     * 更新记忆播放位置开关
     */
    fun updateRememberPosition(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setRememberPositionEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(rememberPosition = enabled)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to update remember position", e)
            }
        }
    }
    
    /**
     * 更新记忆播放速度开关
     */
    fun updateRememberSpeed(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setRememberSpeedEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(rememberSpeed = enabled)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to update remember speed", e)
            }
        }
    }
    
    /**
     * 更新硬件解码开关
     */
    fun updateHardwareDecodingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setHardwareDecodingEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(hardwareDecoding = enabled)
                _playbackState.value = _playbackState.value.copy(isHardwareDecoding = enabled)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to update hardware decoding", e)
            }
        }
    }
    
    /**
     * 更新手势控制开关
     */
    fun updateGestureControlEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setGestureControlEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(gestureControlEnabled = enabled)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to update gesture control", e)
            }
        }
    }
}

/**
 * 播放状态
 */
data class PlaybackState(
    val position: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false,
    val speed: Float = 1.0f,
    val isBuffering: Boolean = false,
    val isHardwareDecoding: Boolean = true
)

/**
 * 播放器设置
 */
data class PlaybackSettings(
    val seekTimeSeconds: Int = 5,
    val rememberPosition: Boolean = true,
    val rememberSpeed: Boolean = true,
    val rememberBrightness: Boolean = true,
    val autoLoadDanmaku: Boolean = true,
    val hardwareDecoding: Boolean = true,
    val gestureControlEnabled: Boolean = true
)
