package com.fam4k007.videoplayer.repository

import android.content.Context
import android.net.Uri
import com.fam4k007.videoplayer.utils.MediaInfoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 媒体信息数据仓库
 * 封装MediaInfo库的调用，提供媒体元数据提取功能
 * 
 * 职责：
 * - 提取视频文件的详细媒体信息（编码格式、分辨率、比特率、音视频流等）
 * - 生成格式化的文本输出（用于复制/分享）
 * - 提取基本元数据（快速模式，用于列表展示）
 */
class MediaInfoRepository(
    private val context: Context
) {
    
    /**
     * 提取详细的媒体信息
     * @param uri 视频文件URI
     * @param fileName 视频文件名
     * @return 媒体信息数据（包含通用信息、视频流、音频流、字幕流）
     */
    suspend fun getMediaInfo(
        uri: Uri,
        fileName: String
    ): Result<MediaInfoHelper.MediaInfoData> = withContext(Dispatchers.IO) {
        MediaInfoHelper.getMediaInfo(context, uri, fileName)
    }
    
    /**
     * 生成格式化的文本输出（用于复制/分享）
     * @param uri 视频文件URI
     * @param fileName 视频文件名
     * @return 格式化的纯文本媒体信息
     */
    suspend fun generateTextOutput(
        uri: Uri,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        MediaInfoHelper.generateTextOutput(context, uri, fileName)
    }
    
    /**
     * 快速提取基本元数据（文件大小、时长、分辨率、帧率）
     * @param uri 视频文件URI
     * @param fileName 视频文件名
     * @return 基本元数据
     */
    suspend fun extractBasicMetadata(
        uri: Uri,
        fileName: String
    ): Result<MediaInfoHelper.VideoMetadata> = withContext(Dispatchers.IO) {
        MediaInfoHelper.extractBasicMetadata(context, uri, fileName)
    }
}
