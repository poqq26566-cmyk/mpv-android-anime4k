package com.fam4k007.videoplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fam4k007.videoplayer.worker.ThumbnailGenerationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * 视频缩略图缓存管理器 - WorkManager优化版
 * 提供磁盘缓存、后台任务调度、智能约束条件
 */
class ThumbnailCacheManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ThumbnailCacheManager"
        private const val CACHE_DIR_NAME = "video_thumbnails"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
        private const val THUMBNAIL_WIDTH = 384
        private const val THUMBNAIL_HEIGHT = 216
        
        @Volatile
        private var instance: ThumbnailCacheManager? = null

        fun getInstance(context: Context): ThumbnailCacheManager {
            return instance ?: synchronized(this) {
                instance ?: ThumbnailCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val cacheDir: File = run {
        // 优先使用外部缓存目录（Android/data/包名/cache/），用户可访问
        // 降级到内部缓存目录（data/data/包名/cache/），需要root
        val dir = (context.externalCacheDir ?: context.cacheDir).let { baseDir ->
            File(baseDir, CACHE_DIR_NAME)
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        Logger.d(TAG, "Thumbnail cache directory: ${dir.absolutePath}")
        dir
    }
    
    private val workManager = WorkManager.getInstance(context)

    // 使用固定线程池限制并发数量（减少并发数）
    private val executorService = Executors.newFixedThreadPool(2)

    /**
     * 获取缩略图（带缓存）
     * 1. 先从磁盘缓存查找
     * 2. 如果没有，立即同步生成缩略图（保证UI能显示）
     */
    suspend fun getThumbnail(context: Context, uri: Uri, duration: Long): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = generateCacheKey(uri)
                val cacheFile = File(cacheDir, cacheKey)

                // 1. 尝试从缓存读取
                if (cacheFile.exists()) {
                    Logger.d(TAG, "Loading thumbnail from cache: $cacheKey")
                    val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    if (bitmap != null) {
                        return@withContext bitmap
                    } else {
                        // 缓存文件损坏，删除
                        cacheFile.delete()
                    }
                }

                // 2. 缓存未命中，立即生成缩略图
                Logger.d(TAG, "Cache miss, generating thumbnail immediately: $uri")
                val bitmap = extractThumbnail(context, uri, duration)
                
                // 3. 保存到缓存
                if (bitmap != null) {
                    saveThumbnailToCache(bitmap, cacheFile)
                    checkCacheSize()
                }
                
                bitmap
                
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to get thumbnail: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * 获取指定播放位置的缩略图（带缓存）- 专为播放历史设计
     * @param uri 视频URI
     * @param positionMs 播放位置（毫秒）
     * @return 缩略图 Bitmap，缓存文件路径通过返回值传递
     */
    suspend fun getThumbnailAtPosition(
        context: Context, 
        uri: Uri, 
        positionMs: Long
    ): Pair<Bitmap?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = generateCacheKeyWithPosition(uri, positionMs)
                val cacheFile = File(cacheDir, cacheKey)

                // 1. 尝试从缓存读取
                if (cacheFile.exists()) {
                    Logger.d(TAG, "✓ Loading thumbnail from cache: $cacheKey")
                    val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    if (bitmap != null) {
                        return@withContext Pair(bitmap, cacheFile.absolutePath)
                    } else {
                        // 缓存文件损坏，删除
                        cacheFile.delete()
                    }
                }

                // 2. 缓存未命中，立即生成指定位置的缩略图
                Logger.d(TAG, "✗ Cache miss, extracting frame at ${positionMs}ms: $uri")
                val bitmap = extractThumbnailAtPosition(context, uri, positionMs)
                
                // 3. 保存到缓存
                if (bitmap != null) {
                    saveThumbnailToCache(bitmap, cacheFile)
                    checkCacheSize()
                    Logger.d(TAG, "✓ Thumbnail cached: ${cacheFile.name}")
                    return@withContext Pair(bitmap, cacheFile.absolutePath)
                }
                
                Pair(null, null)
                
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to get thumbnail at position: ${e.message}", e)
                Pair(null, null)
            }
        }
    }
    
    /**
     * 调度后台缩略图生成任务（已禁用，改为立即同步生成）
     */
    @Deprecated("不再使用后台任务，改为立即同步生成")
    private fun scheduleBackgroundGeneration(uri: Uri, durationMs: Long) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        
        val inputData = workDataOf(
            ThumbnailGenerationWorker.KEY_VIDEO_URI to uri.toString(),
            ThumbnailGenerationWorker.KEY_DURATION_MS to durationMs,
            ThumbnailGenerationWorker.KEY_PRIORITY to "low"
        )
        
        val workRequest = OneTimeWorkRequestBuilder<ThumbnailGenerationWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()
        
        val workName = "thumbnail_cache_${uri.hashCode()}"
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.KEEP,  // 已存在则保持，避免重复
            workRequest
        )
        
        Logger.d(TAG, "Scheduled background thumbnail generation: $workName")
    }

    /**
     * 提取视频缩略图（同步方法，仅用于立即需要的场景）
```
     */
    private fun extractThumbnail(context: Context, uri: Uri, duration: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            // 计算中间帧位置（微秒）
            val frameTimeMicros = if (duration > 0) {
                (duration * 1000L) / 2
            } else {
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val videoDuration = durationStr?.toLongOrNull() ?: 0L
                if (videoDuration > 0) {
                    (videoDuration * 1000L) / 2
                } else {
                    5000000L // 默认5秒
                }
            }

            // 提取指定位置的帧
            // 注意：getFrameAtTime 返回的 bitmap 已经是正确的显示方向
            val bitmap = retriever.getFrameAtTime(
                frameTimeMicros,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return null

            // 使用实际bitmap的宽高
            val srcWidth = bitmap.width
            val srcHeight = bitmap.height
            
            val targetRatio = THUMBNAIL_WIDTH.toFloat() / THUMBNAIL_HEIGHT
            val srcRatio = if (srcHeight != 0) srcWidth.toFloat() / srcHeight else 1f
            
            // 核心思路：等比缩放到至少填满目标区域，然后居中裁剪
            val scale: Float
            val scaledWidth: Int
            val scaledHeight: Int
            
            if (srcRatio > targetRatio) {
                // 源更宽（横屏），按高度缩放，保证高度填满，宽度超出后裁剪
                scale = THUMBNAIL_HEIGHT.toFloat() / srcHeight
                scaledWidth = (srcWidth * scale).toInt()
                scaledHeight = THUMBNAIL_HEIGHT
            } else {
                // 源更高（竖屏），按宽度缩放，保证宽度填满，高度超出后裁剪
                scale = THUMBNAIL_WIDTH.toFloat() / srcWidth
                scaledWidth = THUMBNAIL_WIDTH
                scaledHeight = (srcHeight * scale).toInt()
            }
            
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            
            // 居中裁剪到目标尺寸
            val x = ((scaledWidth - THUMBNAIL_WIDTH) / 2).coerceAtLeast(0)
            val y = ((scaledHeight - THUMBNAIL_HEIGHT) / 2).coerceAtLeast(0)
            val finalBitmap = Bitmap.createBitmap(scaledBitmap, x, y, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            
            return finalBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thumbnail", e)
            return null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release retriever", e)
            }
        }
    }
    
    /**
     * 提取指定播放位置的视频缩略图
     * @param positionMs 播放位置（毫秒）
     */
    private fun extractThumbnailAtPosition(context: Context, uri: Uri, positionMs: Long): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            
            // 设置数据源
            when {
                uri.scheme == "content" -> {
                    retriever.setDataSource(context, uri)
                }
                uri.scheme == "file" -> {
                    retriever.setDataSource(uri.path)
                }
                else -> {
                    retriever.setDataSource(uri.toString())
                }
            }

            // 转换为微秒，使用 OPTION_CLOSEST 获得精确帧
            val frameTimeMicros = positionMs * 1000L
            val bitmap = retriever.getFrameAtTime(
                frameTimeMicros,
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: return null

            // 缩放和裁剪（与extractThumbnail使用相同的逻辑）
            val srcWidth = bitmap.width
            val srcHeight = bitmap.height
            
            val targetRatio = THUMBNAIL_WIDTH.toFloat() / THUMBNAIL_HEIGHT
            val srcRatio = if (srcHeight != 0) srcWidth.toFloat() / srcHeight else 1f
            
            val scale: Float
            val scaledWidth: Int
            val scaledHeight: Int
            
            if (srcRatio > targetRatio) {
                scale = THUMBNAIL_HEIGHT.toFloat() / srcHeight
                scaledWidth = (srcWidth * scale).toInt()
                scaledHeight = THUMBNAIL_HEIGHT
            } else {
                scale = THUMBNAIL_WIDTH.toFloat() / srcWidth
                scaledWidth = THUMBNAIL_WIDTH
                scaledHeight = (srcHeight * scale).toInt()
            }
            
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            val x = ((scaledWidth - THUMBNAIL_WIDTH) / 2).coerceAtLeast(0)
            val y = ((scaledHeight - THUMBNAIL_HEIGHT) / 2).coerceAtLeast(0)
            val finalBitmap = Bitmap.createBitmap(scaledBitmap, x, y, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            
            // 释放中间bitmap
            if (bitmap != finalBitmap) bitmap.recycle()
            if (scaledBitmap != finalBitmap) scaledBitmap.recycle()
            
            return finalBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thumbnail at position ${positionMs}ms", e)
            return null
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release retriever", e)
            }
        }
    }

    /**
     * 保存缩略图到缓存
     */
    private fun saveThumbnailToCache(bitmap: Bitmap, cacheFile: File) {
        try {
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            Log.d(TAG, "Thumbnail saved to cache: ${cacheFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save thumbnail to cache", e)
        }
    }

    /**
     * 生成缓存键（基于URI的MD5）
     */
    private fun generateCacheKey(uri: Uri): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(uri.toString().toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // 降级方案：使用URI的哈希码
            uri.toString().hashCode().toString()
        }
    }
    
    /**
     * 生成带播放位置的缓存键（基于URI+Position的MD5）
     */
    private fun generateCacheKeyWithPosition(uri: Uri, positionMs: Long): String {
        return try {
            // 将位置四舍五入到最近的30秒，减少缓存数量
            val roundedPosition = (positionMs / 30000) * 30000
            val key = "${uri}_$roundedPosition"
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(key.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // 降级方案：使用组合哈希码
            "${uri.toString().hashCode()}_${positionMs / 30000}"
        }
    }

    /**
     * 检查缓存大小，超过限制则清理
     */
    private fun checkCacheSize() {
        try {
            val files = cacheDir.listFiles() ?: return
            var totalSize = files.sumOf { it.length() }

            if (totalSize > MAX_CACHE_SIZE) {
                Log.d(TAG, "Cache size exceeded, cleaning up...")
                
                // 按最后修改时间排序，删除最旧的文件
                val sortedFiles = files.sortedBy { it.lastModified() }
                
                for (file in sortedFiles) {
                    if (totalSize <= MAX_CACHE_SIZE * 0.8) { // 清理到80%
                        break
                    }
                    val fileSize = file.length()
                    if (file.delete()) {
                        totalSize -= fileSize
                        Log.d(TAG, "Deleted cache file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check cache size", e)
        }
    }

    /**
     * 清空所有缓存
     */
    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }

    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 清理资源
     */
    fun shutdown() {
        executorService.shutdown()
    }
}
