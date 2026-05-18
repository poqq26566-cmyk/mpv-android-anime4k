package com.fam4k007.videoplayer.download

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call

/**
 * 下载任务共享存储单例
 * 用于在多个 ViewModel/Screen 之间共享下载任务状态
 */
object DownloadTaskStore {

    /** 自增ID计数器，确保每个下载任务有唯一ID */
    private var idCounter = 0L

    /** 生成唯一ID */
    fun nextId(): String {
        return "dl_${++idCounter}_${System.currentTimeMillis()}"
    }

    private val _downloadItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadItems: StateFlow<List<DownloadItem>> = _downloadItems.asStateFlow()

    /** 协程任务映射，用于暂停/取消 */
    val downloadJobs = mutableMapOf<String, Job>()

    /** OkHttp Call 映射，用于真正中断网络请求 */
    val activeCalls = mutableMapOf<String, Call>()

    /** 当前活跃下载数 */
    var activeDownloadCount = 0
        private set

    /**
     * 尝试获取下载许可（最多同时下载1个）
     * @return true 表示可以开始下载
     */
    fun tryAcquireDownloadSlot(): Boolean {
        if (activeDownloadCount >= 1) return false
        activeDownloadCount++
        return true
    }

    /** 释放下载许可 */
    fun releaseDownloadSlot() {
        if (activeDownloadCount > 0) activeDownloadCount--
    }

    /** 是否有正在下载的任务 */
    fun hasActiveDownload(): Boolean = activeDownloadCount > 0

    fun addItem(item: DownloadItem) {
        _downloadItems.value = _downloadItems.value + item
    }

    fun updateItem(id: String, update: (DownloadItem) -> DownloadItem) {
        _downloadItems.value = _downloadItems.value.map {
            if (it.id == id) update(it) else it
        }
    }

    fun removeItem(id: String) {
        // 先取消关联的Job和Call
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        activeCalls[id]?.cancel()
        activeCalls.remove(id)
        _downloadItems.value = _downloadItems.value.filter { it.id != id }
    }

    fun updateItemProgress(id: String, progress: Int) {
        _downloadItems.value = _downloadItems.value.map {
            if (it.id == id) it.copy(progress = progress) else it
        }
    }

    fun updateItemStatus(id: String, status: String, errorMessage: String? = null) {
        _downloadItems.value = _downloadItems.value.map {
            if (it.id == id) it.copy(status = status, errorMessage = errorMessage) else it
        }
    }

    /** 暂停下载：取消协程 + 取消网络请求 */
    fun pauseDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        activeCalls[id]?.cancel()
        activeCalls.remove(id)
        updateItemStatus(id, "paused")
    }

    fun clearCompleted(deleteFiles: Boolean = false) {
        if (deleteFiles) {
            _downloadItems.value = _downloadItems.value.filter { item ->
                item.status != "completed" && item.status != "cancelled" && item.status != "failed"
            }
        } else {
            _downloadItems.value = _downloadItems.value.filter { item ->
                item.status != "completed" && item.status != "cancelled"
            }
        }
    }

    fun clearAll(deleteFiles: Boolean = false) {
        // 取消所有进行中的任务
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        activeCalls.values.forEach { it.cancel() }
        activeCalls.clear()
        activeDownloadCount = 0
        _downloadItems.value = emptyList()
    }

    fun getItem(id: String): DownloadItem? {
        return _downloadItems.value.find { it.id == id }
    }

    /** 恢复下载的回调，由 BilibiliDownloadViewModel 注册 */
    var onResumeDownload: ((item: DownloadItem) -> Unit)? = null
}
