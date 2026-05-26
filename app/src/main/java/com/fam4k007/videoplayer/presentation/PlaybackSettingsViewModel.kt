package com.fam4k007.videoplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam4k007.videoplayer.repository.PlayerRepository
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 播放设置 ViewModel
 * 管理播放器相关的细粒度设置（进度控制、手势、音量、倍速、画质增强）
 * 
 * 职责：
 * - 进度控制设置（精确定位、快进快退时长）
 * - 手势控制设置（双击模式）
 * - 音量控制设置（音量增强）
 * - 倍速控制设置（记忆倍速、自定义倍速、长按倍速）
 * - 画质增强设置（Anime4K模式记忆）
 */
class PlaybackSettingsViewModel(
    private val playerRepository: PlayerRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "PlaybackSettingsViewModel"
    }
    
    // ==================== UI State ====================
    
    private val _playbackSettings = MutableStateFlow(PlaybackSettings())
    val playbackSettings: StateFlow<PlaybackSettings> = _playbackSettings.asStateFlow()
    
    // ==================== 初始化 ====================
    
    init {
        loadAllSettings()
    }
    
    /**
     * 加载所有播放设置
     */
    private fun loadAllSettings() {
        viewModelScope.launch {
            try {
                val settings = PlaybackSettings(
                    // 进度控制
                    preciseSeeking = playerRepository.isPreciseSeekingEnabled(),
                    seekTime = playerRepository.getSeekTime(),
                    
                    // 手势控制
                    doubleTapMode = playerRepository.getDoubleTapMode(),
                    doubleTapSeekSeconds = playerRepository.getDoubleTapSeekSeconds(),
                    
                    // 音量控制
                    volumeBoost = playerRepository.isVolumeBoostEnabled(),
                    controlSystemVolume = playerRepository.isControlSystemVolume(),
                    
                    // 倍速控制
                    rememberSpeed = playerRepository.isRememberSpeedEnabled(),
                    longPressSpeed = playerRepository.getLongPressSpeed(),
                    customSpeedPresets = playerRepository.getCustomSpeedPresets(),
                    
                    // 画质增强
                    anime4KMemory = playerRepository.isAnime4KMemoryEnabled(),

                    // 进度条样式
                    seekbarStyle = playerRepository.getSeekbarStyle(),

                    // 自动连播
                    autoPlayNext = playerRepository.isAutoPlayNextEnabled(),
                    closeAfterEOF = playerRepository.isCloseAfterEndOfVideo(),

                    // 章节控制
                    chapterBarEnabled = playerRepository.isChapterBarEnabled(),

                    // MPV 解码器预设
                    mpvProfile = playerRepository.getMpvProfile()
                )
                _playbackSettings.value = settings
                Logger.d(TAG, "Loaded playback settings")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load playback settings", e)
            }
        }
    }
    
    // ==================== 进度控制 ====================
    
    /**
     * 设置精确进度定位
     */
    fun setPreciseSeeking(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setPreciseSeekingEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(preciseSeeking = enabled)
                Logger.d(TAG, "Set precise seeking: $enabled")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set precise seeking", e)
            }
        }
    }
    
    /**
     * 设置快进/快退时长
     */
    fun setSeekTime(seconds: Int) {
        viewModelScope.launch {
            try {
                playerRepository.setSeekTime(seconds)
                _playbackSettings.value = _playbackSettings.value.copy(seekTime = seconds)
                Logger.d(TAG, "Set seek time: $seconds seconds")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set seek time", e)
            }
        }
    }
    
    // ==================== 手势控制 ====================
    
    /**
     * 设置双击手势模式
     * @param mode 0=暂停/播放, 1=快进/快退
     */
    fun setDoubleTapMode(mode: Int) {
        viewModelScope.launch {
            try {
                playerRepository.setDoubleTapMode(mode)
                _playbackSettings.value = _playbackSettings.value.copy(doubleTapMode = mode)
                Logger.d(TAG, "Set double tap mode: $mode")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set double tap mode", e)
            }
        }
    }
    
    /**
     * 设置双击跳转秒数
     */
    fun setDoubleTapSeekSeconds(seconds: Int) {
        viewModelScope.launch {
            try {
                playerRepository.setDoubleTapSeekSeconds(seconds)
                _playbackSettings.value = _playbackSettings.value.copy(doubleTapSeekSeconds = seconds)
                Logger.d(TAG, "Set double tap seek seconds: $seconds")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set double tap seek seconds", e)
            }
        }
    }
    
    // ==================== 音量控制 ====================
    
    /**
     * 设置音量增强
     */
    fun setVolumeBoost(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setVolumeBoostEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(volumeBoost = enabled)
                Logger.d(TAG, "Set volume boost: $enabled")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set volume boost", e)
            }
        }
    }
    
    /**
     * 设置控制系统音量
     */
    fun setControlSystemVolume(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setControlSystemVolume(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(controlSystemVolume = enabled)
                Logger.d(TAG, "Set control system volume: $enabled")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set control system volume", e)
            }
        }
    }
    
    // ==================== 倍速控制 ====================
    
    /**
     * 设置记忆播放倍速
     */
    fun setRememberSpeed(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setRememberSpeedEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(rememberSpeed = enabled)
                Logger.d(TAG, "Set remember speed: $enabled")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set remember speed", e)
            }
        }
    }
    
    /**
     * 设置长按倍速
     */
    fun setLongPressSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                playerRepository.setLongPressSpeed(speed)
                _playbackSettings.value = _playbackSettings.value.copy(longPressSpeed = speed)
                Logger.d(TAG, "Set long press speed: $speed")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set long press speed", e)
            }
        }
    }
    
    /**
     * 设置自定义倍速选项
     */
    fun setCustomSpeedPresets(presets: Set<String>) {
        viewModelScope.launch {
            try {
                playerRepository.setCustomSpeedPresets(presets)
                _playbackSettings.value = _playbackSettings.value.copy(customSpeedPresets = presets)
                Logger.d(TAG, "Set custom speed presets: $presets")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set custom speed presets", e)
            }
        }
    }
    
    // ==================== 画质增强 ====================
    
    /**
     * 设置Anime4K模式记忆
     */
    fun setAnime4KMemory(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setAnime4KMemoryEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(anime4KMemory = enabled)
                Logger.d(TAG, "Set Anime4K memory: $enabled")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set Anime4K memory", e)
            }
        }
    }

    // ==================== 进度条样式 ====================

    fun setSeekbarStyle(style: String) {
        viewModelScope.launch {
            try {
                playerRepository.setSeekbarStyle(style)
                _playbackSettings.value = _playbackSettings.value.copy(seekbarStyle = style)
                Logger.d(TAG, "Set seekbar style: $style")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set seekbar style", e)
            }
        }
    }

    // ==================== 章节控制 ====================

    /**
     * 设置章节进度条开关
     */
    fun setChapterBarEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setChapterBarEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(chapterBarEnabled = enabled)
                Logger.d(TAG, "Set chapter bar enabled: $enabled")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set chapter bar", e)
            }
        }
    }

    // ==================== 自动连播 ====================

    /**
     * 设置自动连播下一集（百分百复用 mpvEx 算法）
     */
    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setAutoPlayNextEnabled(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(autoPlayNext = enabled)
                Logger.d(TAG, "Set auto play next: $enabled")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set auto play next", e)
            }
        }
    }

    /**
     * 设置视频结束后关闭播放器（百分百复用 mpvEx 算法）
     */
    fun setCloseAfterEOF(enabled: Boolean) {
        viewModelScope.launch {
            try {
                playerRepository.setCloseAfterEndOfVideo(enabled)
                _playbackSettings.value = _playbackSettings.value.copy(closeAfterEOF = enabled)
                Logger.d(TAG, "Set close after EOF: $enabled")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set close after EOF", e)
            }
        }
    }

    // ==================== MPV 解码器预设 ====================

    /**
     * 设置 MPV 解码器预设
     */
    fun setMpvProfile(profile: String) {
        viewModelScope.launch {
            try {
                playerRepository.setMpvProfile(profile)
                _playbackSettings.value = _playbackSettings.value.copy(mpvProfile = profile)
                Logger.d(TAG, "Set MPV profile: $profile")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set MPV profile", e)
            }
        }
    }
}

/**
 * 播放设置状态数据类
 */
data class PlaybackSettings(
    // 进度控制
    val preciseSeeking: Boolean = false,
    val seekTime: Int = 10,
    
    // 手势控制
    val doubleTapMode: Int = 0,  // 0=暂停/播放, 1=快进/快退
    val doubleTapSeekSeconds: Int = 10,
    
    // 音量控制
    val volumeBoost: Boolean = false,
    val controlSystemVolume: Boolean = false,
    
    // 倍速控制
    val rememberSpeed: Boolean = false,
    val longPressSpeed: Float = 2.0f,
    val customSpeedPresets: Set<String> = setOf("1.0"),
    
    // 画质增强
    val anime4KMemory: Boolean = false,

    // 进度条样式
    val seekbarStyle: String = "Standard",

    // 自动连播（百分百复用 mpvEx）
    val autoPlayNext: Boolean = true,
    val closeAfterEOF: Boolean = true,

    // 章节控制
    val chapterBarEnabled: Boolean = true,

    // MPV 解码器预设
    val mpvProfile: String = "fast"
)
