package com.fam4k007.videoplayer.manager

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 视频缩略图管理器
 * 使用 MediaMetadataRetriever 提取视频帧，配合 LruCache 缓存
 * 仅支持本地视频
 */
class VideoThumbnailManager(context: Context) {
    private val contextRef = WeakReference(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 按字节数限制缓存，最大 50MB，防止低内存设备 OOM
    private val thumbnailCache = object : LruCache<Long, Bitmap>(50 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }
    private var retriever: MediaMetadataRetriever? = null
    private val retrieverLock = Any()

    private var currentVideoUri: Uri? = null
    private var videoDuration: Long = 0L
    private var isLocal: Boolean = false
    private var isInitialized = AtomicBoolean(false)

    private val thumbnailWidth = 320
    private val thumbnailHeight = 180

    private var preloadJob: Job? = null
    private val preloadRange = 10L

    companion object {
        private const val TAG = "VideoThumbnailManager"
    }

    fun initializeVideo(uri: Uri, durationMs: Long, isWebDav: Boolean = false) {
        if (isInitialized.get() && currentVideoUri == uri) return
        isLocal = !isWebDav && (uri.scheme == "file" || uri.scheme == "content")
        if (!isLocal) {
            Logger.d(TAG, "非本地视频，跳过缩略图")
            return
        }
        releaseRetriever()
        thumbnailCache.evictAll()
        currentVideoUri = uri
        videoDuration = durationMs
        isInitialized.set(true)
        scope.launch { initializeRetriever(uri) }
    }

    private suspend fun initializeRetriever(uri: Uri) = withContext(Dispatchers.IO) {
        val context = contextRef.get() ?: return@withContext
        synchronized(retrieverLock) {
            try {
                retriever?.release()
                retriever = MediaMetadataRetriever().apply { setDataSource(context, uri) }
                Logger.d(TAG, "MediaMetadataRetriever 初始化成功")
            } catch (e: Exception) {
                Logger.w(TAG, "MediaMetadataRetriever 初始化失败", e)
                retriever = null
            }
        }
    }

    fun getThumbnailAt(positionSec: Long): Bitmap? = thumbnailCache.get(positionSec)

    suspend fun extractThumbnailRealtime(positionSec: Long): Bitmap? = withContext(Dispatchers.IO) {
        thumbnailCache.get(positionSec)?.let { return@withContext it }
        val bitmap = extractFrameFast(positionSec)
        bitmap?.let { thumbnailCache.put(positionSec, it) }
        bitmap
    }

    private fun extractFrameFast(positionSec: Long): Bitmap? {
        synchronized(retrieverLock) {
            val ret = retriever ?: return null
            return try {
                val timeUs = positionSec * 1_000_000
                val frame = ret.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) scaleBitmap(frame) else null
            } catch (e: Exception) {
                Logger.w(TAG, "提取帧失败 @ ${positionSec}s", e)
                null
            }
        }
    }

    fun preloadAroundPosition(centerSec: Long) {
        preloadJob?.cancel()
        preloadJob = scope.launch {
            val startSec = maxOf(0, centerSec - preloadRange)
            val endSec = minOf(videoDuration / 1000, centerSec + preloadRange)
            val positions = mutableListOf<Long>()
            for (offset in 0..preloadRange) {
                if (centerSec + offset <= endSec) positions.add(centerSec + offset)
                if (centerSec - offset >= startSec && offset > 0) positions.add(centerSec - offset)
            }
            positions.forEach { pos ->
                if (!isActive) return@forEach
                if (thumbnailCache.get(pos) == null) {
                    extractFrameFast(pos)?.let { thumbnailCache.put(pos, it) }
                }
                delay(3)
            }
        }
    }

    private fun scaleBitmap(source: Bitmap): Bitmap {
        val sw = source.width
        val sh = source.height
        if (sw <= thumbnailWidth && sh <= thumbnailHeight) return source
        val scale = minOf(thumbnailWidth.toFloat() / sw, thumbnailHeight.toFloat() / sh)
        val targetW = (sw * scale).toInt()
        val targetH = (sh * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
        if (scaled != source) source.recycle()
        return scaled
    }

    private fun releaseRetriever() {
        synchronized(retrieverLock) {
            retriever?.let {
                try { it.release() } catch (_: Exception) {}
            }
            retriever = null
        }
    }

    fun release() {
        preloadJob?.cancel()
        scope.cancel()
        releaseRetriever()
        thumbnailCache.evictAll()
        currentVideoUri = null
        videoDuration = 0L
        isInitialized.set(false)
    }

    fun isThumbnailSupported(): Boolean = isLocal && currentVideoUri != null && videoDuration > 0
}
