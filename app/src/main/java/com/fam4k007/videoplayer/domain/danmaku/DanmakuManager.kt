package com.fam4k007.videoplayer.domain.danmaku

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fam4k007.videoplayer.danmaku.DanmakuConfig
import com.fam4k007.videoplayer.danmaku.DanmakuPlayerView
import com.fam4k007.videoplayer.utils.DialogUtils
import java.io.File

/**
 * 弹幕管理器
 * 负责弹幕的加载、控制、样式管理
 */
class DanmakuManager(
    private val context: Context,
    private val danmakuView: DanmakuPlayerView
) {
    companion object {
        private const val TAG = "DanmakuManager"
    }

    private var isInitialized = false
    
    // 记录当前加载的弹幕文件路径（参考 DanDanPlay 的 mAddedTrack）
    private var currentDanmakuPath: String? = null
    
    // 播放引擎引用（用于提供播放位置）
    private var playbackEngine: Any? = null

    /**
     * 初始化弹幕配置
     */
    fun initialize() {
        if (isInitialized) {
            Log.w(TAG, "DanmakuManager already initialized")
            return
        }

        try {
            DanmakuConfig.init(context)
            isInitialized = true
            
            // 重新应用所有样式设置（因为 DanmakuPlayerView 在 DanmakuConfig.init() 之前就被创建了）
            danmakuView.updateDanmakuSize()
            danmakuView.updateDanmakuSpeed()
            danmakuView.updateDanmakuAlpha()
            danmakuView.updateDanmakuStroke()
            danmakuView.updateScrollDanmakuState()
            danmakuView.updateTopDanmakuState()
            danmakuView.updateBottomDanmakuState()
            danmakuView.updateMaxLine()
            danmakuView.updateMaxScreenNum()
            
            Log.d(TAG, "DanmakuManager initialized successfully with saved settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DanmakuManager", e)
        }
    }
    
    /**
     * 设置播放引擎（用于提供播放位置）
     * 参考 DanDanPlay 的 ControlWrapper 设计
     */
    fun setPlaybackEngine(engine: Any) {
        playbackEngine = engine
        
        // 创建 PlaybackPositionProvider 并设置到 DanmakuView
        val provider = object : DanmakuPlayerView.PlaybackPositionProvider {
            override fun getCurrentPosition(): Long {
                return try {
                    // 使用反射获取 currentPosition 属性（秒）并转换为毫秒
                    val field = engine.javaClass.getDeclaredField("currentPosition")
                    field.isAccessible = true
                    val positionInSeconds = field.getDouble(engine)
                    (positionInSeconds * 1000).toLong()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get currentPosition", e)
                    0L
                }
            }
            
            override fun isPlaying(): Boolean {
                return try {
                    val field = engine.javaClass.getDeclaredField("isPlaying")
                    field.isAccessible = true
                    field.getBoolean(engine)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get isPlaying", e)
                    false
                }
            }
        }
        
        danmakuView.setPlaybackPositionProvider(provider)
        Log.d(TAG, "PlaybackEngine set")
    }

    /**
     * 加载弹幕文件
     * 自动根据视频文件路径查找同名的 .xml 弹幕文件
     */
    fun loadDanmakuForVideo(videoPath: String, autoShow: Boolean = true, isPlaying: Boolean = false): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "DanmakuManager not initialized")
            return false
        }

        // 即使弹幕功能被禁用，也要继续加载（只是不显示）
        // 这样可以保证弹幕文件被记录，用户开启弹幕功能后可以直接使用

        // 查找同名弹幕文件
        val danmakuFile = findDanmakuFile(videoPath)
        if (danmakuFile == null) {
            Log.d(TAG, "No danmaku file found for video: $videoPath")
            return false
        }

        // 加载弹幕
        val loaded = danmakuView.loadDanmaku(danmakuFile.absolutePath)
        if (loaded) {
            currentDanmakuPath = danmakuFile.absolutePath
            // 参考 DanDanPlay: addTrack 成功后自动调用 selectTrack
            // 注意：setTrackSelected 会根据 autoShow 参数决定可见性
            danmakuView.setTrackSelected(autoShow)
            
            // 【修复】如果视频正在播放且需要自动显示，立即启动弹幕
            if (autoShow && isPlaying && isPrepared()) {
                start()
                Log.d(TAG, "Danmaku auto-started for playing video")
            }
            
            // 根据参数决定是否自动显示
            if (autoShow) {
                Log.d(TAG, "Danmaku loaded, track selected and shown: ${danmakuFile.absolutePath}")
            } else {
                Log.d(TAG, "Danmaku loaded, track selected (hidden): ${danmakuFile.absolutePath}")
            }
        }
        return loaded
    }

    /**
     * 加载指定的弹幕文件（用户手动选择或历史恢复）
     * 参考 DanDanPlay 的 addTrack 方法
     */
    fun loadDanmakuFile(danmakuPath: String, autoShow: Boolean = true, isPlaying: Boolean = false): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "DanmakuManager not initialized")
            return false
        }

        val loaded = danmakuView.loadDanmaku(danmakuPath)
        
        // 如果加载成功，记录路径并设置轨道选中状态（参考 DanDanPlay 的 addTrack → selectTrack 流程）
        if (loaded) {
            currentDanmakuPath = danmakuPath
            // 参考 DanDanPlay: addTrack 成功后自动调用 selectTrack
            // 注意：setTrackSelected 会根据 autoShow 参数决定可见性
            danmakuView.setTrackSelected(autoShow)
            
            // 【修复】如果视频正在播放且需要自动显示，立即启动弹幕
            if (autoShow && isPlaying && isPrepared()) {
                start()
                Log.d(TAG, "Danmaku auto-started for playing video")
            }
            
            Log.d(TAG, "Danmaku loaded and track selected: $danmakuPath, autoShow=$autoShow")
        }
        
        return loaded
    }

    /**
     * 导入弹幕文件（从 URI 复制到内部存储）
     * @param context Android Context
     * @param uri 弹幕文件的 URI
     * @return 复制后的弹幕文件路径，失败返回 null
     */
    fun importDanmakuFile(context: Context, uri: Uri): String? {
        return try {
            val danmakuDir = File(context.filesDir, "danmaku")
            if (!danmakuDir.exists()) {
                danmakuDir.mkdirs()
            }
            
            // 获取文件名
            var fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            } ?: "danmaku_${System.currentTimeMillis()}.xml"
            
            // 确保文件有.xml后缀（兼容弹弹play缓存的无后缀文件）
            if (!fileName.endsWith(".xml", ignoreCase = true)) {
                fileName = "$fileName.xml"
                Log.d(TAG, "Added .xml extension to danmaku file: $fileName")
            }
            
            val danmakuFile = File(danmakuDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                danmakuFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            if (danmakuFile.exists()) {
                Log.d(TAG, "Danmaku file imported: ${danmakuFile.absolutePath}")
                // 自动加载导入的弹幕
                val loaded = loadDanmakuFile(danmakuFile.absolutePath, autoShow = true)
                if (loaded) {
                    danmakuFile.absolutePath
                } else {
                    null
                }
            } else {
                Log.e(TAG, "Failed to import danmaku file")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import danmaku file", e)
            null
        }
    }

    /**
     * 查找弹幕文件
     * 规则：视频文件同目录下，同名的弹幕文件
     * 支持多种弹幕格式和命名规则
     */
    private fun findDanmakuFile(videoPath: String): File? {
        try {
            Log.d(TAG, "===== Finding danmaku file =====")
            Log.d(TAG, "Video path: $videoPath")
            
            val videoFile = File(videoPath)
            val videoDir = videoFile.parentFile
            
            if (videoDir == null) {
                Log.d(TAG, "✗ Parent directory is null")
                return null
            }
            
            if (!videoDir.exists() || !videoDir.isDirectory) {
                Log.d(TAG, "✗ Parent directory does not exist or is not a directory: ${videoDir.absolutePath}")
                return null
            }
            
            val videoNameWithoutExt = videoFile.nameWithoutExtension
            Log.d(TAG, "Video name without ext: $videoNameWithoutExt")
            Log.d(TAG, "Searching in directory: ${videoDir.absolutePath}")

            // 优先级1：查找标准同名 .xml 文件
            val danmakuFile = File(videoDir, "$videoNameWithoutExt.xml")
            if (danmakuFile.exists() && danmakuFile.isFile) {
                Log.d(TAG, "✓ Found danmaku file: ${danmakuFile.absolutePath}")
                return danmakuFile
            }

            // 优先级2：查找其他常见命名格式
            val alternativeNames = listOf(
                // B站格式
                "${videoNameWithoutExt}.danmaku.xml",
                "${videoNameWithoutExt}_danmaku.xml",
                // 弹弹play格式
                "${videoNameWithoutExt}.dandan.xml",
                "${videoNameWithoutExt}_dandan.xml",
                // AcFun格式
                "${videoNameWithoutExt}.acfun.xml",
                // 通用格式
                "danmaku.xml",
                "弹幕.xml",
                // 无扩展名（兼容弹弹play缓存）
                videoNameWithoutExt
            )

            Log.d(TAG, "Trying alternative names...")
            for (name in alternativeNames) {
                val file = File(videoDir, name)
                if (file.exists() && file.isFile) {
                    Log.d(TAG, "✓ Found alternative danmaku file: ${file.absolutePath}")
                    return file
                }
            }

            Log.d(TAG, "✗ No danmaku file found for: $videoNameWithoutExt")
            Log.d(TAG, "Directory contents:")
            videoDir.listFiles()?.take(10)?.forEach { file ->
                Log.d(TAG, "  - ${file.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding danmaku file", e)
        }

        return null
    }

    /**
     * 开始播放弹幕
     */
    fun start() {
        danmakuView.startDanmaku()
    }

    /**
     * 暂停弹幕
     */
    fun pause() {
        danmakuView.pauseDanmaku()
    }

    /**
     * 恢复弹幕
     */
    fun resume() {
        danmakuView.resumeDanmaku()
    }

    /**
     * 同步进度
     * @param timeMs 时间（毫秒）
     * @param isPlaying 是否正在播放（默认 true）
     */
    fun seekTo(timeMs: Long, isPlaying: Boolean = true) {
        danmakuView.seekDanmaku(timeMs, isPlaying)
    }

    /**
     * 切换弹幕显示/隐藏
     */
    fun toggleVisibility() {
        danmakuView.toggleDanmakuVisibility()
        Log.d(TAG, "Danmaku visibility toggled to: ${danmakuView.isShown}")
    }

    /**
     * 设置弹幕轨道选中状态（参考 DanDanPlay 的 setTrackSelected）
     * @param selected true=选中轨道并显示弹幕，false=取消选中并隐藏弹幕
     */
    fun setTrackSelected(selected: Boolean) {
        danmakuView.setTrackSelected(selected)
        Log.d(TAG, "Danmaku track selected: $selected")
    }
    
    /**
     * 获取弹幕轨道选中状态
     */
    fun getTrackSelected(): Boolean {
        return danmakuView.getTrackSelected()
    }

    /**
     * 设置弹幕显示状态
     */
    fun setVisibility(visible: Boolean) {
        if (visible) {
            danmakuView.show()
        } else {
            danmakuView.hide()
        }
        Log.d(TAG, "Danmaku visibility set to: $visible")
    }

    /**
     * 获取弹幕当前显示状态
     */
    fun isVisible(): Boolean {
        return danmakuView.isShown
    }

    /**
     * 获取当前加载的弹幕文件路径（参考 DanDanPlay 的 getAddedTrack）
     */
    fun getCurrentDanmakuPath(): String? {
        return currentDanmakuPath
    }
    
    /**
     * 弹幕是否已准备好
     */
    fun isPrepared(): Boolean {
        return danmakuView.isDanmakuPrepared()
    }

    /**
     * 释放资源
     */
    fun release() {
        currentDanmakuPath = null
        danmakuView.releaseDanmaku()
    }

    /**
     * 设置播放倍速
     */
    fun setSpeed(speed: Float) {
        danmakuView.setPlaybackSpeed(speed)
    }

    // ==================== 样式设置方法 ====================

    fun updateSize() {
        danmakuView.updateDanmakuSize()
    }

    fun updateSpeed() {
        danmakuView.updateDanmakuSpeed()
    }

    fun updateAlpha() {
        danmakuView.updateDanmakuAlpha()
    }

    fun updateStroke() {
        danmakuView.updateDanmakuStroke()
    }

    fun updateScrollDanmaku() {
        danmakuView.updateScrollDanmakuState()
    }

    fun updateTopDanmaku() {
        danmakuView.updateTopDanmakuState()
    }

    fun updateBottomDanmaku() {
        danmakuView.updateBottomDanmakuState()
    }

    fun updateMaxLine() {
        danmakuView.updateMaxLine()
    }

    fun updateMaxScreenNum() {
        danmakuView.updateMaxScreenNum()
    }

    fun updateOffsetTime() {
        danmakuView.updateOffsetTime()
    }

    /**
     * 应用所有样式设置
     */
    fun applyAllSettings() {
        updateSize()
        updateSpeed()
        updateAlpha()
        updateStroke()
        updateScrollDanmaku()
        updateTopDanmaku()
        updateBottomDanmaku()
        updateMaxLine()
        updateMaxScreenNum()
    }
}
