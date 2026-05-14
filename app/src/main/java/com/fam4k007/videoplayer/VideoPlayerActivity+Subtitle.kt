package com.fam4k007.videoplayer

import android.net.Uri
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.Logger
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "VideoPlayerActivity"

/**
 * 自动加载同文件夹下的同名字幕文件
 */
internal fun VideoPlayerActivity.autoLoadSubtitleIfExists(videoUri: android.net.Uri) {
    try {
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load subtitle start =====")
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video URI: $videoUri")

        // 检查PreferencesManager中是否已有字幕路径，如果有则跳过自动加载
        val savedSubtitlePath = preferencesManager.getExternalSubtitle(videoUri.toString())
        if (savedSubtitlePath != null && File(savedSubtitlePath).exists()) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Subtitle already exists in preferences: $savedSubtitlePath, skipping auto-load")
            return
        }

        // 获取视频文件真实路径
        val videoPath = getRealPathFromUri(videoUri)
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Real video path: $videoPath")

        if (videoPath == null) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Cannot get real path from URI")
            return
        }

        val videoFile = File(videoPath)
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video file exists: ${videoFile.exists()}, isFile: ${videoFile.isFile}")

        if (!videoFile.exists() || !videoFile.isFile) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video file not accessible")
            return
        }

        // 获取视频文件名（不含扩展名）
        val videoNameWithoutExt = videoFile.nameWithoutExtension
        val videoDir = videoFile.parentFile

        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video name without ext: $videoNameWithoutExt")
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video directory: ${videoDir?.absolutePath}")

        if (videoDir == null) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Video directory is null")
            return
        }

        // 按优先级排序：ass > srt > 其他
        val priorityExtensions = listOf("ass", "srt", "ssa", "vtt", "sub", "sbv", "json")

        // 模糊匹配：查找所有以视频文件名开头的字幕文件
        val allSubtitles = videoDir.listFiles()?.filter { file ->
            if (!file.isFile) return@filter false
            val fileName = file.name.lowercase()
            val videoName = videoNameWithoutExt.lowercase()

            // 文件名必须以视频名开头
            if (!fileName.startsWith(videoName)) return@filter false

            // 扩展名必须在支持列表中
            val ext = file.extension.lowercase()
            priorityExtensions.contains(ext)
        } ?: emptyList()

        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Found ${allSubtitles.size} potential subtitle files")

        // 如果没有找到任何字幕，直接返回
        if (allSubtitles.isEmpty()) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "No matching subtitle file found")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load subtitle end =====")
            return
        }

        // 获取系统语言偏好
        val systemLanguage = resources.configuration.locales[0].toString().lowercase()
        val isSimplifiedChinese = systemLanguage.contains("zh_cn") || systemLanguage.contains("zh-cn")
        val isTraditionalChinese = systemLanguage.contains("zh_tw") || systemLanguage.contains("zh_hk") ||
                                   systemLanguage.contains("zh-tw") || systemLanguage.contains("zh-hk")

        com.fam4k007.videoplayer.utils.Logger.d(TAG, "System language: $systemLanguage, SC: $isSimplifiedChinese, TC: $isTraditionalChinese")

        // 按优先级排序字幕文件
        val sortedSubtitles = allSubtitles.sortedWith(compareBy(
            // 1. 扩展名优先级（数字越小优先级越高）
            { file ->
                val ext = file.extension.lowercase()
                priorityExtensions.indexOf(ext).let { if (it == -1) 999 else it }
            },
            // 2. 完全匹配优先（123.ass > 123.sc.ass）
            { file ->
                val nameWithoutExt = file.nameWithoutExtension
                if (nameWithoutExt.equals(videoNameWithoutExt, ignoreCase = true)) 0 else 1
            },
            // 3. 语言标记优先级（根据系统语言）
            { file ->
                val nameLower = file.nameWithoutExtension.lowercase()
                val afterVideoName = nameLower.removePrefix(videoNameWithoutExt.lowercase())

                when {
                    // 简体中文系统优先简体标记
                    isSimplifiedChinese && (afterVideoName.contains("sc") || afterVideoName.contains("chs") ||
                                            afterVideoName.contains("简") || afterVideoName.contains("zh-cn")) -> 0
                    // 繁体中文系统优先繁体标记
                    isTraditionalChinese && (afterVideoName.contains("tc") || afterVideoName.contains("cht") ||
                                             afterVideoName.contains("繁") || afterVideoName.contains("zh-tw")) -> 0
                    else -> 1
                }
            },
            // 4. 文件名长度（越短越优先，假设越接近原始名称）
            { file -> file.nameWithoutExtension.length },
            // 5. 字母顺序
            { file -> file.name.lowercase() }
        ))

        // 打印所有找到的字幕（调试用）
        sortedSubtitles.forEachIndexed { index, file ->
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Subtitle [$index]: ${file.name}")
        }

        // 选择优先级最高的字幕文件
        val foundSubtitle = sortedSubtitles.first()

        val subtitlePath = foundSubtitle.absolutePath
        com.fam4k007.videoplayer.utils.Logger.d(TAG, "Auto-loading best match subtitle: $subtitlePath")

        try {
            MPVLib.command("sub-add", subtitlePath, "select")
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Successfully auto-loaded subtitle: ${foundSubtitle.name}")

            // 保存字幕路径到记忆中，下次进入视频时不会重复自动加载
            preferencesManager.setExternalSubtitle(videoUri.toString(), subtitlePath)
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Saved auto-loaded subtitle path to preferences")

            // 设置标志，防止 restoreSubtitlePreferences 重复加载
            viewModel.setHasAutoLoadedSubtitle(true)

            DialogUtils.showToastShort(this, "已自动加载字幕: ${foundSubtitle.name}")
        } catch (e: Exception) {
            com.fam4k007.videoplayer.utils.Logger.w(TAG, "Failed to auto-load subtitle", e)
            DialogUtils.showToastShort(this, "字幕加载失败: ${e.message}")
        }

        com.fam4k007.videoplayer.utils.Logger.d(TAG, "===== Auto-load subtitle end =====")
    } catch (e: Exception) {
        Log.e(TAG, "Error in autoLoadSubtitleIfExists", e)
    }
}

/**
 * 恢复视频的字幕偏好设置
 */
internal fun VideoPlayerActivity.restoreSubtitlePreferences(videoUri: android.net.Uri) {
    try {
        val uriString = videoUri.toString()
        Logger.d(TAG, "Restoring subtitle preferences for: $uriString")

        playbackEngine?.let { engine ->
            val assOverride = preferencesManager.isAssOverrideEnabled(uriString)
            if (assOverride) {
                lifecycleScope.launch {
                    delay(300)
                    engine.setAssOverride(assOverride)
                    Logger.d(TAG, "Restored ASS override: $assOverride")
                }
            }

            val savedScale = preferencesManager.getSubtitleScale(uriString)
            if (savedScale != 1.0) {
                engine.setSubtitleScale(savedScale)
                Logger.d(TAG, "Restored subtitle scale: $savedScale")
            }

            val savedPosition = preferencesManager.getSubtitlePosition(uriString)
            if (savedPosition != 100) {
                engine.setSubtitleVerticalPosition(savedPosition)
                Logger.d(TAG, "Restored subtitle position: $savedPosition")
            }

            val savedDelay = preferencesManager.getSubtitleDelay(uriString)
            if (savedDelay != 0.0) {
                engine.setSubtitleDelay(savedDelay)
                Logger.d(TAG, "Restored subtitle delay: $savedDelay")
            }

            val savedSubtitlePath = preferencesManager.getExternalSubtitle(uriString)
            if (savedSubtitlePath != null) {
                // 如果已经自动加载过这个字幕，就跳过（防止重复加载）
                if (viewModel.hasAutoLoadedSubtitle.value) {
                    Logger.d(TAG, "Subtitle already auto-loaded this session, skipping restore")
                } else if (File(savedSubtitlePath).exists()) {
                    try {
                        MPVLib.command("sub-add", savedSubtitlePath, "select")
                        Logger.d(TAG, "Restored external subtitle from path: $savedSubtitlePath")
                    } catch (e: Exception) {
                        Logger.w(TAG, "Failed to restore external subtitle", e)
                    }
                } else {
                    Logger.w(TAG, "Saved subtitle file not found: $savedSubtitlePath")
                }
            }

            val savedTrackId = preferencesManager.getSubtitleTrackId(uriString)
            if (savedTrackId != -1) {
                lifecycleScope.launch {
                    delay(600)
                    try {
                        engine.selectSubtitleTrack(savedTrackId)
                        Logger.d(TAG, "Restored subtitle track: $savedTrackId")
                    } catch (e: Exception) {
                        Logger.w(TAG, "Failed to restore subtitle track", e)
                    }
                }
            }

            val savedTextColor = preferencesManager.getSubtitleTextColor(uriString)
            if (savedTextColor != "#FFFFFF") {
                engine.setSubtitleTextColor(savedTextColor)
                Logger.d(TAG, "Restored subtitle text color: $savedTextColor")
            }

            val savedBorderSize = preferencesManager.getSubtitleBorderSize(uriString)
            if (savedBorderSize != 3) {
                engine.setSubtitleBorderSize(savedBorderSize)
                Logger.d(TAG, "Restored subtitle border size: $savedBorderSize")
            }

            val savedBorderColor = preferencesManager.getSubtitleBorderColor(uriString)
            if (savedBorderColor != "#000000") {
                engine.setSubtitleBorderColor(savedBorderColor)
                Logger.d(TAG, "Restored subtitle border color: $savedBorderColor")
            }

            val savedBackColor = preferencesManager.getSubtitleBackColor(uriString)
            if (savedBackColor != "#00000000") {
                engine.setSubtitleBackColor(savedBackColor)
                Logger.d(TAG, "Restored subtitle back color: $savedBackColor")
            }

            // 总是应用描边样式，即使是默认值，确保MPV状态正确
            val savedBorderStyle = preferencesManager.getSubtitleBorderStyle(uriString)
            engine.setSubtitleBorderStyle(savedBorderStyle)
            Logger.d(TAG, "Restored subtitle border style: $savedBorderStyle")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error restoring subtitle preferences", e)
        // 恢复字幕设置失败不影响播放，不提示用户
    }
}

/**
 * 从content:// URI获取真实文件路径
 */
internal fun VideoPlayerActivity.getRealPathFromUri(uri: android.net.Uri): String? {
    return when (uri.scheme) {
        "file" -> uri.path
        "content" -> {
            try {
                val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                        cursor.getString(columnIndex)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                com.fam4k007.videoplayer.utils.Logger.w(TAG, "Failed to get real path from content URI", e)
                null
            }
        }
        else -> null
    }
}

/**
 * 根据文件路径查找对应的content:// URI（从MediaStore）
 */
internal fun VideoPlayerActivity.getContentUriForFile(file: File): android.net.Uri? {
    return try {
        val projection = arrayOf(android.provider.MediaStore.Files.FileColumns._ID)
        val selection = "${android.provider.MediaStore.Files.FileColumns.DATA}=?"
        val selectionArgs = arrayOf(file.absolutePath)

        contentResolver.query(
            android.provider.MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns._ID))
                android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Files.getContentUri("external"),
                    id
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        com.fam4k007.videoplayer.utils.Logger.w(TAG, "Failed to get content URI for file: ${file.absolutePath}", e)
        null
    }
}
