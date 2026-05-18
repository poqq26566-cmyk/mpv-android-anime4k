package com.fam4k007.videoplayer.manager

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import io.framescout.FrameGrabber
import io.framescout.FrameGrabberFactory
import java.io.ByteArrayOutputStream

/**
 * 支持 Android 本地文件（content:// / file://）的 FrameGrabber 实现。
 * 直接使用 MediaMetadataRetriever.setDataSource(Context, Uri)，
 * 兼容 MmrFrameGrabber 不支持的 content URI。
 */
class LocalFrameGrabber(private val context: Context) : FrameGrabber {

    private var retriever: MediaMetadataRetriever? = null

    override fun open(url: String, headers: Map<String, String>) {
        close()
        val mmr = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(url)
            if (uri.scheme == "content" || uri.scheme == "file") {
                mmr.setDataSource(context, uri)
            } else {
                mmr.setDataSource(url, headers)
            }
            retriever = mmr
        } catch (t: Throwable) {
            runCatching { mmr.release() }
            throw t
        }
    }

    override fun grab(tsMs: Long, widthPx: Int, heightPx: Int, jpegQuality: Int): ByteArray? {
        val mmr = retriever ?: return null
        val timeUs = tsMs * 1000L

        val raw: Bitmap = try {
            mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Throwable) {
            return null
        } ?: return null

        // 缩放到目标尺寸
        val scaled: Bitmap = if (raw.width == widthPx && raw.height == heightPx) {
            raw
        } else {
            try {
                Bitmap.createScaledBitmap(raw, widthPx, heightPx, true)
            } catch (_: Throwable) {
                raw.recycle()
                return null
            }
        }
        if (scaled !== raw) raw.recycle()

        // 编码为 JPEG
        val out = ByteArrayOutputStream()
        try {
            scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
        } catch (_: Throwable) {
            scaled.recycle()
            return null
        }
        if (scaled !== raw) scaled.recycle()
        return out.toByteArray()
    }

    override fun sourceDurationMs(): Long? {
        val mmr = retriever ?: return null
        return try {
            val s = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            s?.toLongOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    override fun close() {
        retriever?.let {
            runCatching { it.release() }
            retriever = null
        }
    }

    companion object {
        fun factory(context: Context): FrameGrabberFactory = FrameGrabberFactory {
            LocalFrameGrabber(context)
        }
    }
}
