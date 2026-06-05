package com.fam4k007.videoplayer.utils.media

import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * .nomedia 路径过滤器（带缓存）
 *
 * 反编译参考项目 live.alist.mpv 同名类的 Kotlin 移植版。
 * 与 NoMediaChecker 功能重叠，但增加 ConcurrentHashMap 缓存，
 * 避免每次检查都遍历父目录。
 *
 * 使用方式：
 *   val filter = NoMediaPathFilter(includeNoMediaFolders = false)
 *   if (filter.shouldExcludeDirectory(someDir)) { ... }
 */
class NoMediaPathFilter(
    /** true = 包含 .nomedia 文件夹（不跳过），false = 跳过 */
    val includeNoMediaFolders: Boolean
) {
    companion object {
        private const val TAG = "NoMediaPathFilter"
    }

    /** 路径 → 是否应排除 的缓存 */
    private val exclusionCache = ConcurrentHashMap<String, Boolean>()

    /** 排除 .nomedia 文件夹？（与 includeNoMediaFolders 相反） */
    private val excludeNoMediaFolders: Boolean get() = !includeNoMediaFolders

    // ==================== 公开方法 ====================

    /**
     * 目录是否应被排除（跳过扫描）
     */
    fun shouldExcludeDirectory(directory: File?): Boolean {
        if (!excludeNoMediaFolders || directory == null) return false
        return hasNoMediaMarkerInPath(directory)
    }

    /**
     * 文件是否应被排除（跳过扫描）
     * 由其所在目录决定
     */
    fun shouldExcludeFile(file: File): Boolean {
        return shouldExcludeDirectory(file.parentFile)
    }

    /**
     * 清除缓存（当文件系统变化时调用）
     */
    fun clearCache() {
        exclusionCache.clear()
    }

    // ==================== 内部方法 ====================

    /**
     * 递归检查目录或其任何父目录是否包含 .nomedia 文件
     * 结果缓存在 exclusionCache 中
     */
    private fun hasNoMediaMarkerInPath(directory: File): Boolean {
        val path = try {
            directory.absolutePath
        } catch (e: Exception) {
            return false
        }

        // 查缓存
        exclusionCache[path]?.let { return it }

        val result = try {
            // 检查当前目录是否有 .nomedia
            if (File(directory, ".nomedia").exists()) {
                true
            } else {
                // 递归检查父目录
                val parent = directory.parentFile
                parent != null && hasNoMediaMarkerInPath(parent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "检查 .nomedia 失败: $path", e)
            false
        }

        exclusionCache[path] = result
        return result
    }
}
