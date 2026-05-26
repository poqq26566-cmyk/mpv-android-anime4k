package com.fam4k007.videoplayer.utils

import android.content.Context
import android.provider.MediaStore
import com.fam4k007.videoplayer.preferences.PreferencesManager
import java.io.File

/**
 * 文件扫描过滤器
 * 统一管理文件扫描时的过滤规则，读取 PreferencesManager 中的开关状态。
 * 所有扫描路径（文件夹视图、视频列表、系列识别）都应通过此工具判断。
 */
object ScanFilter {

    /**
     * 检查文件是否应该被跳过（不被扫描）
     *
     * @param context Android Context
     * @param filePath 文件的完整路径
     * @return true 表示应该跳过该文件
     */
    fun shouldSkipFile(context: Context, filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return true

        val prefs = PreferencesManager.getInstance(context)
        val folderPath = filePath.substringBeforeLast("/", "")

        // 1. 检查 .nomedia 规则
        if (prefs.isNomediaEnabled()) {
            if (NoMediaChecker.containsNoMedia(folderPath)) {
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
     *
     * @param context Android Context
     * @param folderPath 文件夹路径
     * @return true 表示应该跳过该文件夹
     */
    fun shouldSkipFolder(context: Context, folderPath: String?): Boolean {
        if (folderPath.isNullOrBlank()) return true

        val prefs = PreferencesManager.getInstance(context)

        // 1. 检查 .nomedia 规则
        if (prefs.isNomediaEnabled()) {
            if (NoMediaChecker.folderHasNoMedia(folderPath)) {
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
     * 获取视频文件时长（毫秒）
     * 先尝试 MediaStore 查询（快），失败后用 MediaMetadataRetriever 直接从文件读取（保证有结果）
     */
    fun queryDuration(context: Context, filePath: String): Long {
        // 先尝试 MediaStore
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
                    val ms = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    if (ms > 0) return ms
                }
            }
        } catch (_: Exception) { }

        // MediaStore 没有 → 用 MediaMetadataRetriever 直接读文件
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
