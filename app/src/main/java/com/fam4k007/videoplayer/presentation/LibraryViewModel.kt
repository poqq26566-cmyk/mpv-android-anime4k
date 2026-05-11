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
    
    // ==================== UI State ====================
    
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    private val _folders = MutableStateFlow<List<VideoFolder>>(emptyList())
    val folders: StateFlow<List<VideoFolder>> = _folders.asStateFlow()
    
    private val _videos = MutableStateFlow<List<VideoFileParcelable>>(emptyList())
    val videos: StateFlow<List<VideoFileParcelable>> = _videos.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(VideoSortOrder.NAME_ASC)
    val sortOrder: StateFlow<VideoSortOrder> = _sortOrder.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // ==================== 视频扫描 ====================
    
    /**
     * 扫描所有本地视频文件夹
     */
    fun scanVideoFolders() {
        viewModelScope.launch {
            try {
                _uiState.value = LibraryUiState.Loading
                val folders = videoRepository.scanAllVideoFolders()
                _folders.value = folders
                _uiState.value = if (folders.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Success
                }
                Logger.d(TAG, "Scanned ${folders.size} video folders")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to scan video folders", e)
                _uiState.value = LibraryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 扫描指定文件夹的视频
     */
    fun scanVideosInFolder(folderPath: String) {
        viewModelScope.launch {
            try {
                _uiState.value = LibraryUiState.Loading
                val videos = videoRepository.scanVideosInFolder(folderPath, _sortOrder.value)
                _videos.value = videos
                _uiState.value = if (videos.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Success
                }
                Logger.d(TAG, "Scanned ${videos.size} videos in folder: $folderPath")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to scan videos in folder", e)
                _uiState.value = LibraryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 刷新当前文件夹的视频列表
     */
    fun refreshVideos(folderPath: String) {
        scanVideosInFolder(folderPath)
    }
    
    // ==================== 视频排序 ====================
    
    /**
     * 更改排序方式并重新加载
     */
    fun changeSortOrder(sortOrder: VideoSortOrder, folderPath: String?) {
        _sortOrder.value = sortOrder
        folderPath?.let { scanVideosInFolder(it) }
    }
    
    // ==================== 视频搜索 ====================
    
    /**
     * 搜索视频
     */
    fun searchVideos(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    // 清空搜索，显示所有视频
                    _uiState.value = LibraryUiState.Success
                    return@launch
                }
                
                _uiState.value = LibraryUiState.Loading
                val searchResults = videoRepository.searchVideos(query)
                _videos.value = searchResults
                _uiState.value = if (searchResults.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Success
                }
                Logger.d(TAG, "Search results for '$query': ${searchResults.size} videos")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to search videos", e)
                _uiState.value = LibraryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 清空搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 从数据库加载缓存的视频
     */
    fun loadCachedVideos(folderPath: String) {
        viewModelScope.launch {
            try {
                _uiState.value = LibraryUiState.Loading
                val cachedVideos = videoRepository.getCachedVideos(folderPath)
                if (cachedVideos.isNotEmpty()) {
                    _videos.value = cachedVideos
                    _uiState.value = LibraryUiState.Success
                    Logger.d(TAG, "Loaded ${cachedVideos.size} cached videos")
                } else {
                    // 没有缓存，触发扫描
                    scanVideosInFolder(folderPath)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load cached videos", e)
                // 加载失败，回退到扫描
                scanVideosInFolder(folderPath)
            }
        }
    }
    
    /**
     * 清除视频缓存
     */
    fun clearVideoCache() {
        viewModelScope.launch {
            try {
                videoRepository.clearVideoCache()
                _videos.value = emptyList()
                Logger.d(TAG, "Cleared video cache")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to clear video cache", e)
            }
        }
    }
    
    // ==================== 文件夹管理 ====================
    
    /**
     * 隐藏文件夹
     */
    fun hideFolder(folderPath: String) {
        viewModelScope.launch {
            try {
                videoRepository.hideFolder(folderPath)
                // 刷新文件夹列表
                scanVideoFolders()
                Logger.d(TAG, "Hidden folder: $folderPath")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to hide folder", e)
            }
        }
    }
    
    /**
     * 显示文件夹
     */
    fun showFolder(folderPath: String) {
        viewModelScope.launch {
            try {
                videoRepository.showFolder(folderPath)
                // 刷新文件夹列表
                scanVideoFolders()
                Logger.d(TAG, "Shown folder: $folderPath")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to show folder", e)
            }
        }
    }
}

/**
 * 视频库UI状态
 */
sealed class LibraryUiState {
    object Loading : LibraryUiState()
    object Success : LibraryUiState()
    object Empty : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}
