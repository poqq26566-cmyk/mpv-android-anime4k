package com.fam4k007.videoplayer.manager

import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.FormatUtils
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 截图管理器
 * 使用MPV的截图功能直接截取视频画面
 */
class ScreenshotManager(private val activity: Activity) {
    
    companion object {
        private const val TAG = "ScreenshotManager"
        private const val FOLDER_NAME = "FAM4K007"
    }
    
    /**
     * 截取当前视频画面
     */
    fun takeScreenshot() {
        try {
            // 立即提示用户
            DialogUtils.showToastShort(activity, "Saved")
            
            // 后台静默保存
            CoroutineScope(Dispatchers.IO).launch {
                val tempDir = activity.cacheDir
                val filename = "screenshot_${System.currentTimeMillis()}.png"
                val tempFile = File(tempDir, filename)
                
                // 使用MPV的截图命令，不包含字幕
                MPVLib.command("screenshot-to-file", tempFile.absolutePath, "video")
                
                // 等待文件生成后保存到相册
                Thread.sleep(200)
                
                if (tempFile.exists()) {
                    saveToGallery(tempFile)
                } else {
                    Log.e(TAG, "Screenshot file not created")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot failed", e)
            DialogUtils.showToastShort(activity, "Screenshot failed: ${e.message}")
        }
    }
    
    private fun saveToGallery(sourceFile: File) {
        try {
            val displayName = "FAM4K007_${FormatUtils.generateTimestamp()}.png"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER_NAME")
            }
            
            val resolver = activity.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // 删除临时文件
                sourceFile.delete()
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "Screenshot saved: $displayName")
            } else {
                Log.e(TAG, "Failed to create MediaStore URI")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to gallery", e)
        }
    }
}
