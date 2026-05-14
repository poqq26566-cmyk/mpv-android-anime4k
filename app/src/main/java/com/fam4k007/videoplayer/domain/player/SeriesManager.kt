package com.fam4k007.videoplayer.domain.player

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.fam4k007.videoplayer.utils.NoMediaChecker
import java.io.File

/**
 * 系列视频管理器
 * 负责识别、管理同系列的视频文件
 */
class SeriesManager {

    companion object {
        private const val TAG = "SeriesManager"
        private val VIDEO_EXTENSIONS = listOf(
            "mp4", "mkv", "avi", "mov", "flv", "wmv", 
            "webm", "m4v", "3gp", "ts", "m2ts"
        )
    }

    // 系列视频列表
    private val videoList = mutableListOf<Uri>()
    
    // 当前播放索引
    var currentIndex: Int = -1
        private set
    
    // 系列名称
    var seriesName: String = ""
        private set

    /**
     * 是否有上一集
     */
    val hasPrevious: Boolean
        get() = currentIndex > 0

    /**
     * 是否有下一集
     */
    val hasNext: Boolean
        get() = currentIndex >= 0 && currentIndex < videoList.size - 1

    /**
     * 获取视频列表
     */
    fun getVideoList(): List<Uri> = videoList.toList()

    /**
     * 设置视频列表（从外部传入）
     */
    fun setVideoList(videos: List<Uri>, currentVideoUri: Uri) {
        videoList.clear()
        videoList.addAll(videos)
        currentIndex = videoList.indexOfFirst { it.toString() == currentVideoUri.toString() }
        Log.d(TAG, "Video list set: ${videoList.size} videos, current index: $currentIndex")
    }

    /**
     * 识别系列视频
     */
    fun identifySeries(
        context: android.content.Context,
        currentUri: Uri,
        getFileNameCallback: (Uri) -> String
    ) {
        // 如果列表已经有内容，说明是从列表传入的，不需要重新扫描
        if (videoList.isNotEmpty() && currentIndex >= 0) {
            Log.d(TAG, "Video list already provided, skipping scan")
            return
        }

        val fileName = getFileNameCallback(currentUri)
        seriesName = extractSeriesName(fileName)

        Log.d(TAG, "Identifying series for: $fileName")
        Log.d(TAG, "Series name extracted: $seriesName")

        videoList.clear()

        // 获取同目录下的视频文件
        val videosInFolder = getVideosInSameFolder(context, currentUri, getFileNameCallback)
        Log.d(TAG, "Found ${videosInFolder.size} videos in folder")

        // 过滤同系列视频
        videosInFolder.forEach { video ->
            val videoFileName = getFileNameCallback(video)
            val videoSeriesName = extractSeriesName(videoFileName)
            Log.d(TAG, "Comparing: '$videoSeriesName' vs '$seriesName' (file: $videoFileName)")
            if (videoSeriesName.equals(seriesName, ignoreCase = true)) {
                videoList.add(video)
                Log.d(TAG, "✓ Added to series: $videoFileName")
            } else {
                Log.d(TAG, "✗ Not same series: $videoFileName")
            }
        }

        // 如果没找到同系列视频，至少添加当前视频
        if (videoList.isEmpty()) {
            Log.w(TAG, "No series videos found, adding current video only")
            videoList.add(currentUri)
        }

        // 按文件名自然排序
        videoList.sortWith(Comparator { uri1, uri2 ->
            compareNatural(getFileNameCallback(uri1), getFileNameCallback(uri2))
        })

        // 找到当前视频位置
        currentIndex = videoList.indexOfFirst { it.toString() == currentUri.toString() }

        Log.d(TAG, "Series identified: ${videoList.size} episodes, current index: $currentIndex")
    }

    /**
     * 获取上一集
     */
    fun getPreviousVideo(): Uri? {
        return if (hasPrevious) {
            videoList[currentIndex - 1]
        } else null
    }

    /**
     * 获取下一集
     */
    fun getNextVideo(): Uri? {
        return if (hasNext) {
            videoList[currentIndex + 1]
        } else null
    }

    /**
     * 切换到指定视频
     */
    fun switchToVideo(uri: Uri) {
        currentIndex = videoList.indexOfFirst { it.toString() == uri.toString() }
        Log.d(TAG, "Switched to video at index: $currentIndex")
    }

    /**
     * 提取系列名称
     */
    private fun extractSeriesName(fileName: String): String {
        var seriesName = fileName.substringBeforeLast('.')

        // 先去除方括号和圆括号内容（字幕组、编码信息等）
        seriesName = Regex("""\[[^\]]*]""").replace(seriesName, " ")
        seriesName = Regex("""\([^)]*\)""").replace(seriesName, " ")

        // 去除分辨率
        seriesName = Regex("""\d{3,4}[pP]""").replace(seriesName, " ")
        seriesName = Regex("""[24][kK]""").replace(seriesName, " ")

        // 去除编码信息
        seriesName = Regex("""[Hh]\.?26[45]""").replace(seriesName, " ")
        seriesName = Regex("""[xX]26[45]""").replace(seriesName, " ")
        seriesName = Regex("""\d+bit""", RegexOption.IGNORE_CASE).replace(seriesName, " ")

        // 匹配并移除集数模式
        val episodePatterns = listOf(
            Regex("""[Ss]\d{1,2}[Ee]\d{1,3}"""),  // S01E02
            Regex("""[Ss]eason\s*\d+\s*[Ee]pisode\s*\d+""", RegexOption.IGNORE_CASE),
            Regex("""[_\-\s.](EP?|第)(\d{1,3})(集|话|話|v\d)?[_\-\s.\[]?""", RegexOption.IGNORE_CASE),
            Regex("""\[\d{1,3}]"""),  // [02]
            Regex("""\(\d{1,3}\)"""),  // (02)
            Regex("""_\d{1,3}_"""),    // _02_
            Regex("""\s\d{1,3}\s""")   // 空格02空格
        )

        for (pattern in episodePatterns) {
            seriesName = pattern.replace(seriesName, " ")
        }

        // 清理多余的分隔符和空格
        seriesName = seriesName.replace(Regex("""[_\-\s.]+"""), " ").trim()

        Log.d(TAG, "Extracted series name: '$seriesName' from '$fileName'")
        return seriesName
    }

    /**
     * 获取同目录下的视频文件
     */
    private fun getVideosInSameFolder(
        context: android.content.Context,
        uri: Uri,
        getFileNameCallback: (Uri) -> String
    ): List<Uri> {
        val videos = mutableListOf<Uri>()

        try {
            when (uri.scheme) {
                "content" -> {
                    // 尝试从 MediaStore 获取
                    videos.addAll(getVideosFromMediaStore(context, uri))

                    // 如果失败，尝试 DocumentFile
                    if (videos.isEmpty()) {
                        videos.addAll(getVideosFromDocumentFile(context, uri))
                    }
                }
                "file" -> {
                    videos.addAll(getVideosFromFile(uri))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting videos in folder", e)
        }

        return videos
    }

    private fun getVideosFromMediaStore(
        context: android.content.Context,
        uri: Uri
    ): List<Uri> {
        val videos = mutableListOf<Uri>()
        
        try {
            val projection = arrayOf(
                android.provider.MediaStore.Video.Media.DATA,
                android.provider.MediaStore.Video.Media.DISPLAY_NAME
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DATA)
                    if (dataIndex >= 0) {
                        val realPath = cursor.getString(dataIndex)
                        if (realPath != null) {
                            val file = File(realPath)
                            val parentDir = file.parentFile

                            if (parentDir != null && parentDir.exists() && parentDir.isDirectory) {
                                // 检查父目录是否包含 .nomedia 文件
                                if (NoMediaChecker.folderHasNoMedia(parentDir.absolutePath)) {
                                    Log.d(TAG, "父目录包含 .nomedia 文件，跳过: ${parentDir.absolutePath}")
                                    return videos
                                }
                                
                                parentDir.listFiles()?.forEach { f ->
                                    if (f.isFile && f.extension.lowercase() in VIDEO_EXTENSIONS) {
                                        // 再次检查文件路径
                                        if (!NoMediaChecker.fileInNoMediaFolder(f.absolutePath)) {
                                            videos.add(Uri.fromFile(f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query failed: ${e.message}")
        }

        return videos
    }

    private fun getVideosFromDocumentFile(
        context: android.content.Context,
        uri: Uri
    ): List<Uri> {
        val videos = mutableListOf<Uri>()

        try {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            if (documentFile != null && documentFile.exists()) {
                val parentUri = documentFile.parentFile?.uri

                if (parentUri != null) {
                    val parentFolder = DocumentFile.fromTreeUri(context, parentUri)
                        ?: DocumentFile.fromSingleUri(context, parentUri)

                    parentFolder?.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            val fileName = file.name ?: ""
                            val extension = fileName.substringAfterLast('.', "").lowercase()

                            if (extension in VIDEO_EXTENSIONS) {
                                file.uri?.let { videos.add(it) }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DocumentFile query failed: ${e.message}")
        }

        return videos
    }

    private fun getVideosFromFile(uri: Uri): List<Uri> {
        val videos = mutableListOf<Uri>()

        uri.path?.let { path ->
            val file = File(path)
            val parentDir = file.parentFile

            if (parentDir != null && parentDir.exists() && parentDir.isDirectory) {
                // 检查父目录是否包含 .nomedia 文件
                if (NoMediaChecker.folderHasNoMedia(parentDir.absolutePath)) {
                    Log.d(TAG, "父目录包含 .nomedia 文件，跳过: ${parentDir.absolutePath}")
                    return videos
                }
                
                parentDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.extension.lowercase() in VIDEO_EXTENSIONS) {
                        // 再次检查文件路径
                        if (!NoMediaChecker.fileInNoMediaFolder(f.absolutePath)) {
                            videos.add(Uri.fromFile(f))
                        }
                    }
                }
            }
        }

        return videos
    }

    /**
     * 切换到上一集
     * @return 上一集的 Uri，如果没有则返回 null
     */
    fun previous(): Uri? {
        return if (hasPrevious) {
            currentIndex--
            videoList[currentIndex]
        } else {
            null
        }
    }

    /**
     * 切换到下一集
     * @return 下一集的 Uri，如果没有则返回 null
     */
    fun next(): Uri? {
        return if (hasNext) {
            currentIndex++
            videoList[currentIndex]
        } else {
            null
        }
    }

    /**
     * 获取当前视频
     */
    fun getCurrentVideo(): Uri? {
        return if (currentIndex >= 0 && currentIndex < videoList.size) {
            videoList[currentIndex]
        } else {
            null
        }
    }
    
    /**
     * 自然排序字符串比较
     */
    private fun compareNatural(str1: String, str2: String): Int {
        val s1 = str1.lowercase()
        val s2 = str2.lowercase()
        
        var i1 = 0
        var i2 = 0
        
        while (i1 < s1.length && i2 < s2.length) {
            val c1 = s1[i1]
            val c2 = s2[i2]
            
            if (c1.isDigit() && c2.isDigit()) {
                var num1 = 0
                while (i1 < s1.length && s1[i1].isDigit()) {
                    num1 = num1 * 10 + (s1[i1] - '0')
                    i1++
                }
                
                var num2 = 0
                while (i2 < s2.length && s2[i2].isDigit()) {
                    num2 = num2 * 10 + (s2[i2] - '0')
                    i2++
                }
                
                if (num1 != num2) {
                    return num1 - num2
                }
            } else {
                if (c1 != c2) {
                    return c1 - c2
                }
                i1++
                i2++
            }
        }
        
        return s1.length - s2.length
    }
}
