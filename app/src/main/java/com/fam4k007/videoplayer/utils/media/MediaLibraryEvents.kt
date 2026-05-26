package com.fam4k007.videoplayer.utils.media

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 媒体库事件总线
 *
 * 当媒体文件发生变化（新增/删除/重命名）或系统 MediaScanner 扫描完成时，
 * 通过此事件总线通知所有订阅的 ViewModel 刷新数据。
 *
 * 参考自 mpvEx 的 MediaLibraryEvents.kt
 */
object MediaLibraryEvents {

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    /**
     * 通知所有订阅者媒体库已发生变化
     */
    fun notifyChanged() {
        _changes.tryEmit(Unit)
    }
}
