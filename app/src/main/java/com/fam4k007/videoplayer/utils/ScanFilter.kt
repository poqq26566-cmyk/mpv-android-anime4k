package com.fam4k007.videoplayer.utils

import android.content.Context
import android.provider.MediaStore
import com.fam4k007.videoplayer.preferences.PreferencesManager
import com.fam4k007.videoplayer.utils.media.NoMediaPathFilter
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 文件扫描过滤器
 * 统一管理文件扫描时的过滤规则，读取 PreferencesManager 中的开关状态。
 * 所有扫描路径（文件夹视图、视频列表、系列识别）都应通过此工具判断。
 *
 * 内部使用 NoMediaPathFilter 带缓存实现，避免重复遍历父目录。
 */
object ScanFilter {

    /** .nomedia 检测结果缓存，避免重复遍历父目录 */
    private val noMediaCache = ConcurrentHashMap<String, Boolean>()

    /**
     * 清除内部缓存（文件系统变化时调用）
     */
    fun clearCache() {
        noMediaCache.clear()
    }

    /**
     * 检查文件是否应该被跳过（不被扫描）
     */
    fun shouldSkipFile(context: Context, filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return true

        val prefs = PreferencesManager.getInstance(context)
        val folderPath = filePath.substringBeforeLast("/", "")

        // 1. 检查 .nomedia 规则（不扫描包含 .nomedia 的文件夹）
        if (!prefs.getIncludeNoMediaFolders()) {
            if (hasNoMediaMarker(folderPath)) {
                return true
            }
        }

        // 2. 检查隐藏文件夹
        if (!prefs.isScanHiddenFoldersEnabled()) {
            if (isHiddenFolder(folderPath)) {
                return true
            }
        }

        return false
    }

    /**
     * 检查文件夹是否应该被跳过（不扫描其中的视频）
     */
    fun shouldSkipFolder(context: Context, folderPath: String?): Boolean {
        if (folderPath.isNullOrBlank()) return true

        val prefs = PreferencesManager.getInstance(context)

        // 1. 检查 .nomedia 规则（不扫描包含 .nomedia 的文件夹）
        if (!prefs.getIncludeNoMediaFolders()) {
            if (hasNoMediaMarker(folderPath)) {
                return true
            }
        }

        // 2. 检查隐藏文件夹
        if (!prefs.isScanHiddenFoldersEnabled()) {
            if (isHiddenFolder(folderPath)) {
                return true
            }
        }

        return false
    }

    /**
     * 检查路径或其父目录是否存在 .nomedia 文件（带缓存）
     */
    private fun hasNoMediaMarker(path: String): Boolean {
        noMediaCache[path]?.let { return it }

        val result = try {
            var currentDir = File(path)
            if (currentDir.isFile) {
                currentDir = currentDir.parentFile ?: return false
            }
            var found = false
            while (currentDir.exists()) {
                if (File(currentDir, ".nomedia").exists()) {
                    found = true
                    break
                }
                currentDir = currentDir.parentFile ?: break
            }
            found
        } catch (e: Exception) {
            false
        }

        noMediaCache[path] = result
        return result
    }

    /**
     * 获取视频文件时长（毫秒）
     *
     * 优先查 MediaStore（SQLite 查询，速度快），
     * 查不到时回退到 MediaMetadataRetriever 直接读取文件（用于隐藏文件夹等 MediaStore 未索引的文件）。
     */
    fun queryDuration(context: Context, filePath: String): Long {
        // 先尝试 MediaStore（快）
        try {
            val projection = arrayOf(MediaStore.Video.Media.DURATION)
            val selection = "${MediaStore.Video.Media.DATA} = ?"
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                arrayOf(filePath),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val ms = it.getLong(col)
                    if (ms > 0) return ms
                }
            }
        } catch (_: Exception) { }

        // MediaStore 没有 → 用 MediaMetadataRetriever 直接读文件
        // 这种情况发生在隐藏文件夹或带 .nomedia 的文件夹中（MediaStore 不索引这些文件）
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 仅从 MediaStore 查询视频文件时长（毫秒），不做文件读取回退
     * 用于主扫描路径（MediaStore 已有索引的视频），速度快。
     */
    fun queryDurationFromMediaStore(context: Context, filePath: String): Long {
        return try {
            val projection = arrayOf(MediaStore.Video.Media.DURATION)
            val selection = "${MediaStore.Video.Media.DATA} = ?"
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                arrayOf(filePath),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    return it.getLong(col)
                }
            }
            0L
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * 判断路径是否在隐藏文件夹中（文件夹名以点号开头）
     */
    private fun isHiddenFolder(folderPath: String): Boolean {
        return try {
            val folder = File(folderPath)
            var current = folder
            while (current.exists() && current.parent != null) {
                if (current.name.startsWith(".")) {
                    return true
                }
                current = current.parentFile ?: break
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
