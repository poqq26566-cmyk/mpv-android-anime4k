package com.fam4k007.videoplayer.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam4k007.videoplayer.database.PlaybackHistoryEntity
import com.fam4k007.videoplayer.repository.PlayerRepository
import com.fam4k007.videoplayer.utils.Logger
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 播放器ViewModel
 * 管理播放器相关的UI状态，调用Repository和Domain层
 * 
 * 阶段1.1: MPV状态直接监听（学习mpvEx）
 * - 使用MPVLib观察者模式监听属性变化
 * - 将MPV状态封装为StateFlow，自动触发UI更新
 */
class PlayerViewModel(
    private val playerRepository: PlayerRepository
) : ViewModel(), MPVLib.EventObserver {
    
    companion object {
        private const val TAG = "PlayerViewModel"
    }
    
    // ==================== MPV 属性状态（阶段1.1新增）====================
    
    // 播放状态（直接从MPV监听）
    private val _paused = MutableStateFlow<Boolean?>(null)
    val paused: StateFlow<Boolean?> = _paused.asStateFlow()
    
    // 播放位置（秒，整数）
    private val _position = MutableStateFlow(0)
    val position: StateFlow<Int> = _position.asStateFlow()
    
    // 视频时长（秒，整数）
    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()
    
    // 播放速度
    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()
    
    // 高精度进度（用于流畅的进度条，约24fps更新）
    private val _precisePosition = MutableStateFlow(0.0)
    val precisePosition: StateFlow<Double> = _precisePosition.asStateFlow()
    
    // 高精度时长
    private val _preciseDuration = MutableStateFlow(0.0)
    val preciseDuration: StateFlow<Double> = _preciseDuration.asStateFlow()
    
    // 字幕轨道列表（TODO: 从track-list解析）
    private val _subtitleTracks = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleTrack>> = _subtitleTracks.asStateFlow()
    
    // 音频轨道列表（TODO: 从track-list解析）
    private val _audioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrack>> = _audioTracks.asStateFlow()
    
    // MPV观察者是否已注册
    private var mpvObserverRegistered = false
    
    init {
        // 轮询更新高精度位置（约24fps）
        viewModelScope.launch {
            while (isActive) {
                try {
                    MPVLib.getPropertyDouble("time-pos")?.let {
                        _precisePosition.value = it
                    }
                } catch (e: Exception) {
                    // 忽略错误，可能MPV还未初始化
                }
                delay(42) // ~24fps
            }
        }
        
        // 轮询更新高精度时长
        viewModelScope.launch {
            var lastDuration = 0
            while (isActive) {
                try {
                    val intDuration = MPVLib.getPropertyInt("duration") ?: 0
                    if (intDuration != lastDuration) {
                        lastDuration = intDuration
                        MPVLib.getPropertyDouble("duration")?.let { dur ->
                            if (dur > 0) {
                                _preciseDuration.value = dur
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 忽略错误
                }
                delay(1000) // 每秒检查一次
            }
        }
    }
    
    /**
     * 注册MPV观察者
     * 应在MPV初始化后调用
     */
    fun registerMPVObservers() {
        if (mpvObserverRegistered) {
            Logger.w(TAG, "MPV observers already registered")
            return
        }
        
        try {
            // 注册事件观察者
            MPVLib.addObserver(this)
            
            // 观察属性变化
            MPVLib.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            MPVLib.observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            MPVLib.observeProperty("speed", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            
            // 观察track-list用于获取字幕/音频轨道
            MPVLib.observeProperty("track-list", MPVLib.MpvFormat.MPV_FORMAT_NODE)
            
            mpvObserverRegistered = true
            Logger.d(TAG, "MPV observers registered successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to register MPV observers", e)
        }
    }
    
    /**
     * 注销MPV观察者
     * 应在ViewModel清理时调用
     */
    fun unregisterMPVObservers() {
        if (!mpvObserverRegistered) return
        
        try {
            MPVLib.removeObserver(this)
            mpvObserverRegistered = false
            Logger.d(TAG, "MPV observers unregistered")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to unregister MPV observers", e)
        }
    }
    
    // ==================== MPVLib.EventObserver 实现 ====================
    
    override fun eventProperty(property: String) {
        // MPV属性变化事件（新版API）
        Logger.v(TAG, "MPV property changed: $property")
    }
    
    override fun eventProperty(property: String, value: Long) {
        // 整数属性变化
        Logger.v(TAG, "MPV property $property = $value")
        when (property) {
            "time-pos" -> _position.value = value.toInt()
            "duration" -> _duration.value = value.toInt()
        }
    }
    
    override fun eventProperty(property: String, value: Boolean) {
        // 布尔属性变化
        Logger.v(TAG, "MPV property $property = $value")
        when (property) {
            "pause" -> _paused.value = value
        }
    }
    
    override fun eventProperty(property: String, value: String) {
        // 字符串属性变化
        Logger.v(TAG, "MPV property $property = $value")
    }
    
    override fun eventProperty(property: String, value: Double) {
        // 浮点数属性变化
        Logger.v(TAG, "MPV property $property = $value")
        when (property) {
            "speed" -> _speed.value = value.toFloat()
        }
    }
    
    override fun eventProperty(property: String, value: MPVNode) {
        // 复杂类型属性变化（如track-list）
        Logger.v(TAG, "MPV property $property = MPVNode")
        when (property) {
            "track-list" -> parseTrackList(value)
        }
    }
    
    /**
     * 解析MPV的track-list节点
     * track-list是一个数组，每个元素是一个map包含轨道信息
     */
    private fun parseTrackList(node: MPVNode) {
        try {
            // track-list是ArrayNode类型
            if (node !is MPVNode.ArrayNode) {
                Logger.w(TAG, "track-list is not ArrayNode: ${node::class.simpleName}")
                return
            }
            
            val subtitles = mutableListOf<SubtitleTrack>()
            val audios = mutableListOf<AudioTrack>()
            
            // 遍历每个轨道
            for (trackNode in node.value) {
                if (trackNode !is MPVNode.MapNode) continue
                
                val trackMap = trackNode.value
                
                // 提取轨道信息
                val id = (trackMap["id"] as? MPVNode.IntNode)?.value?.toInt() ?: continue
                val type = (trackMap["type"] as? MPVNode.StringNode)?.value ?: continue
                val lang = (trackMap["lang"] as? MPVNode.StringNode)?.value
                val title = (trackMap["title"] as? MPVNode.StringNode)?.value
                val selected = (trackMap["selected"] as? MPVNode.BooleanNode)?.value ?: false
                val external = (trackMap["external"] as? MPVNode.BooleanNode)?.value ?: false
                
                // 根据类型分类
                when (type) {
                    "sub" -> {
                        subtitles.add(
                            SubtitleTrack(
                                id = id,
                                lang = lang,
                                title = title,
                                type = type,
                                selected = selected,
                                external = external
                            )
                        )
                    }
                    "audio" -> {
                        val codec = (trackMap["codec"] as? MPVNode.StringNode)?.value
                        audios.add(
                            AudioTrack(
                                id = id,
                                lang = lang,
                                title = title,
                                type = type,
                                selected = selected,
                                codec = codec
                            )
                        )
                    }
                }
            }
            
            // 更新StateFlow
            _subtitleTracks.value = subtitles
            _audioTracks.value = audios
            
            Logger.d(TAG, "Parsed track-list: ${subtitles.size} subtitle tracks, ${audios.size} audio tracks")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to parse track-list", e)
        }
    }
    
    override fun event(eventId: Int) {
        // MPV事件（文件加载、播放结束等）
        when (eventId) {
            21 -> { // MPV_EVENT_PLAYBACK_RESTART
                Logger.d(TAG, "MPV: Playback restarted")
            }
            6 -> { // MPV_EVENT_FILE_LOADED
                Logger.d(TAG, "MPV: File loaded")
            }
            7 -> { // MPV_EVENT_END_FILE
                Logger.d(TAG, "MPV: End of file")
            }
        }
    }
    
    // ==================== UI State（原有代码）====================
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _playbackHistory = MutableStateFlow<PlaybackHistoryEntity?>(null)
    val playbackHistory: StateFlow<PlaybackHistoryEntity?> = _playbackHistory.asStateFlow()
    
    private val _playerSettings = MutableStateFlow(PlayerSettings())
    val playerSettings: StateFlow<PlayerSettings> = _playerSettings.asStateFlow()
    
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
                val settings = PlayerSettings(
                    seekTimeSeconds = playerRepository.getSeekTimeSeconds(),
                    rememberPosition = playerRepository.isRememberPositionEnabled(),
                    rememberSpeed = playerRepository.isRememberSpeedEnabled(),
                    rememberBrightness = playerRepository.isRememberBrightnessEnabled(),
                    autoLoadDanmaku = playerRepository.isAutoLoadDanmakuEnabled(),
                    hardwareDecoding = playerRepository.isHardwareDecodingEnabled(),
                    gestureControlEnabled = playerRepository.isGestureControlEnabled()
                )
                _playerSettings.value = settings
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
                _playerSettings.value = _playerSettings.value.copy(seekTimeSeconds = seconds)
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
                _playerSettings.value = _playerSettings.value.copy(rememberPosition = enabled)
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
                _playerSettings.value = _playerSettings.value.copy(rememberSpeed = enabled)
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
                _playerSettings.value = _playerSettings.value.copy(hardwareDecoding = enabled)
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
                _playerSettings.value = _playerSettings.value.copy(gestureControlEnabled = enabled)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to update gesture control", e)
            }
        }
    }
    
    // ==================== ViewModel 生命周期 ====================
    
    /**
     * ViewModel清理时注销MPV观察者
     */
    override fun onCleared() {
        super.onCleared()
        unregisterMPVObservers()
        Logger.d(TAG, "PlayerViewModel cleared")
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
 * 播放器设置（核心播放器配置）
 */
data class PlayerSettings(
    val seekTimeSeconds: Int = 5,
    val rememberPosition: Boolean = true,
    val rememberSpeed: Boolean = true,
    val rememberBrightness: Boolean = true,
    val autoLoadDanmaku: Boolean = true,
    val hardwareDecoding: Boolean = true,
    val gestureControlEnabled: Boolean = true
)

/**
 * 字幕轨道信息（从MPV track-list解析）
 */
data class SubtitleTrack(
    val id: Int,
    val lang: String?,
    val title: String?,
    val type: String,
    val selected: Boolean = false,
    val external: Boolean = false
)

/**
 * 音频轨道信息（从MPV track-list解析）
 */
data class AudioTrack(
    val id: Int,
    val lang: String?,
    val title: String?,
    val type: String,
    val selected: Boolean = false,
    val codec: String?
)
