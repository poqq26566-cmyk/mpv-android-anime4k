package com.fam4k007.videoplayer.repository

import android.content.Context
import android.net.Uri
import com.fam4k007.videoplayer.subtitle.SearchOptions
import com.fam4k007.videoplayer.subtitle.SubtitleDownloadManager
import com.fam4k007.videoplayer.subtitle.SubtitleInfo
import com.fam4k007.videoplayer.subtitle.TmdbMediaResult

/**
 * 字幕仓库
 * 封装字幕搜索和下载功能
 */
class SubtitleRepository(
    private val context: Context
) {
    private val downloadManager = SubtitleDownloadManager(context)

    /**
     * 搜索媒体（影片/剧集）
     */
    suspend fun searchMedia(query: String): Result<List<TmdbMediaResult>> {
        return try {
            when (val result = downloadManager.searchMedia(query)) {
                is SubtitleDownloadManager.MediaSearchResult.Success -> {
                    Result.success(result.media)
                }
                is SubtitleDownloadManager.MediaSearchResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据媒体ID搜索字幕
     */
    suspend fun searchSubtitlesByMediaId(
        mediaId: Int,
        searchOptions: SearchOptions
    ): Result<List<SubtitleInfo>> {
        return try {
            when (val result = downloadManager.searchSubtitlesByMediaId(mediaId, searchOptions)) {
                is SubtitleDownloadManager.SearchResult.Success -> {
                    Result.success(result.subtitles)
                }
                is SubtitleDownloadManager.SearchResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 下载字幕
     */
    suspend fun downloadSubtitle(
        subtitle: SubtitleInfo,
        saveUri: Uri,
        videoFileName: String
    ): Result<String> {
        return try {
            when (val result = downloadManager.downloadSubtitle(subtitle, saveUri, videoFileName)) {
                is SubtitleDownloadManager.DownloadResult.Success -> {
                    Result.success(result.filePath)
                }
                is SubtitleDownloadManager.DownloadResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
