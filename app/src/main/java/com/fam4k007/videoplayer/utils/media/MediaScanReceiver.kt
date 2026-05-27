package com.fam4k007.videoplayer.utils.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import com.fam4k007.videoplayer.utils.Logger

/**
 * 系统媒体扫描广播接收器
 *
 * 监听 MediaScanner 完成广播，自动通知媒体库刷新。
 * 无需主动扫描，系统在以下情况会自动触发 MediaScanner：
 * - 文件管理器复制/移动/删除文件
 * - App 通过 MediaStore 写入新文件
 * - 系统启动完成
 * - USB 存储挂载
 *
 * 参考自 mpvEx 的 MediaScanReceiver.kt
 */
class MediaScanReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MediaScanReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MEDIA_SCANNER_FINISHED -> {
                val path = intent.data?.path ?: "unknown"
                Logger.d(TAG, "MediaScanner 扫描完成: $path")
                MediaLibraryEvents.notifyChanged()
            }
            // ACTION_MEDIA_SCANNER_SCAN_FILE 已弃用，仅保留 ACTION_MEDIA_SCANNER_FINISHED
            // 单文件扫描的完成也会通过 FINISHED 广播通知
            Intent.ACTION_MEDIA_MOUNTED -> {
                Logger.d(TAG, "存储设备挂载")
                MediaLibraryEvents.notifyChanged()
            }
        }
    }
}
