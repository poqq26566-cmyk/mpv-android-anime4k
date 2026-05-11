package com.fam4k007.videoplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam4k007.videoplayer.VideoFileParcelable
import com.fam4k007.videoplayer.VideoFolder
import com.fam4k007.videoplayer.repository.VideoRepository
import com.fam4k007.videoplayer.repository.VideoSortOrder
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 视频库ViewModel
 * 管理本地视频扫描、文件夹管理、视频列表等UI状态
 */
class LibraryViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "LibraryViewModel"
    }
    
    // ==================== 文件夹列表状态 ====================
    
    data class FolderListState(
        val folders: List<VideoFolder> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val sortType: Int = 0,  // 0: 名称升序, 1: 名称降序, 2: 视频数量
        val sortOrder: Int = 0   // 0: 升序, 1: 降序
    )
    
    private val _folderListState = MutableStateFlow(FolderListState())
    val folderListState: StateFlow<FolderListState> = _folderListState.asStateFlow()
    
    // ==================== 视频列表状态 ====================
    
    data class VideoListState(
        val videos: List<VideoFileParcelable> = emptyList(),
        val filteredVideos: List<VideoFileParcelable> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val sortType: Int = 0,  // 0: 名称, 1: 时间, 2: 大小
        val sortOrder: Int = 0,  // 0: 升序, 1: 降序
        val searchQuery: String = ""
    )
    
    private val _videoListState = MutableStateFlow(VideoListState())
    val videoListState: StateFlow<VideoListState> = _videoListState.asStateFlow()
    
    // ==================== 文件夹扫描 ====================
    
    /**
     * 扫描所有本地视频文件夹
     */
    fun scanVideoFolders() {
        viewModelScope.launch {
            try {
                _folderListState.value = _folderListState.value.copy(isLoading = true, error = null)
                val folders = videoRepository.scanAllVideoFolders()
                val sortedFolders = sortFolders(folders, _folderListState.value.sortType, _folderListState.value.sortOrder)
                _folderListState.value = _folderListState.value.copy(
                    folders = sortedFolders,
                    isLoading = false
                )
                Logger.d(TAG, "Scanned ${folders.size} video folders")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to scan video folders", e)
                _folderListState.value = _folderListState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * 刷新文件夹列表
     */
    fun refreshFolders() {
        viewModelScope.launch {
            try {
                _folderListState.value = _folderListState.value.copy(isRefreshing = true, error = null)
                val folders = videoRepository.scanAllVideoFolders()
                val sortedFolders = sortFolders(folders, _folderListState.value.sortType, _folderListState.value.sortOrder)
                _folderListState.value = _folderListState.value.copy(
                    folders = sortedFolders,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to refresh folders", e)
                _folderListState.value = _folderListState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * 文件夹排序
     */
    fun sortFolders(sortType: Int, sortOrder: Int) {
        val sorted = sortFolders(_folderListState.value.folders, sortType, sortOrder)
        _folderListState.value = _folderListState.value.copy(
            folders = sorted,
            sortType = sortType,
            sortOrder = sortOrder
        )
    }
    
    private fun sortFolders(folders: List<VideoFolder>, sortType: Int, sortOrder: Int): List<VideoFolder> {
        return when (sortType) {
            0 -> if (sortOrder == 0) folders.sortedBy { it.folderName } else folders.sortedByDescending { it.folderName }
            1 -> if (sortOrder == 0) folders.sortedByDescending { it.folderName } else folders.sortedBy { it.folderName }
            2 -> if (sortOrder == 0) folders.sortedBy { it.videoCount } else folders.sortedByDescending { it.videoCount }
            else -> folders
        }
    }
    
    // ==================== 视频扫描 ====================
    
    /**
     * 扫描指定文件夹的视频
     */
    fun scanVideosInFolder(folderPath: String) {
        viewModelScope.launch {
            try {
                _videoListState.value = _videoListState.value.copy(isLoading = true, error = null)
                val videos = videoRepository.scanVideosInFolder(folderPath, VideoSortOrder.NAME_ASC)
                val sortedVideos = sortVideos(videos, _videoListState.value.sortType, _videoListState.value.sortOrder)
                _videoListState.value = _videoListState.value.copy(
                    videos = sortedVideos,
                    filteredVideos = filterVideos(sortedVideos, _videoListState.value.searchQuery),
                    isLoading = false
                )
                Logger.d(TAG, "Scanned ${videos.size} videos in folder: $folderPath")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to scan videos in folder", e)
                _videoListState.value = _videoListState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * 刷新当前文件夹的视频列表
     */
    fun refreshVideos(folderPath: String) {
        viewModelScope.launch {
            try {
                _videoListState.value = _videoListState.value.copy(isRefreshing = true, error = null)
                val videos = videoRepository.scanVideosInFolder(folderPath, VideoSortOrder.NAME_ASC)
                val sortedVideos = sortVideos(videos, _videoListState.value.sortType, _videoListState.value.sortOrder)
                _videoListState.value = _videoListState.value.copy(
                    videos = sortedVideos,
                    filteredVideos = filterVideos(sortedVideos, _videoListState.value.searchQuery),
                    isRefreshing = false
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to refresh videos", e)
                _videoListState.value = _videoListState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    // ==================== 视频排序 ====================
    
    /**
     * 视频排序
     */
    fun sortVideos(sortType: Int, sortOrder: Int) {
        val sorted = sortVideos(_videoListState.value.videos, sortType, sortOrder)
        _videoListState.value = _videoListState.value.copy(
            videos = sorted,
            filteredVideos = filterVideos(sorted, _videoListState.value.searchQuery),
            sortType = sortType,
            sortOrder = sortOrder
        )
    }
    
    private fun sortVideos(videos: List<VideoFileParcelable>, sortType: Int, sortOrder: Int): List<VideoFileParcelable> {
        return when (sortType) {
            0 -> if (sortOrder == 0) videos.sortedBy { it.name } else videos.sortedByDescending { it.name }
            1 -> if (sortOrder == 0) videos.sortedBy { it.dateAdded } else videos.sortedByDescending { it.dateAdded }
            2 -> if (sortOrder == 0) videos.sortedBy { it.size } else videos.sortedByDescending { it.size }
            else -> videos
        }
    }
    
    // ==================== 视频搜索 ====================
    
    /**
     * 搜索视频
     */
    fun searchVideos(query: String) {
        val filtered = filterVideos(_videoListState.value.videos, query)
        _videoListState.value = _videoListState.value.copy(
            searchQuery = query,
            filteredVideos = filtered
        )
    }
    
    private fun filterVideos(videos: List<VideoFileParcelable>, query: String): List<VideoFileParcelable> {
        if (query.isBlank()) return videos
        return videos.filter { it.name.contains(query, ignoreCase = true) }
    }
    
    /**
     * 清空搜索
     */
    fun clearSearch() {
        _videoListState.value = _videoListState.value.copy(
            searchQuery = "",
            filteredVideos = _videoListState.value.videos
        )
    }
}
