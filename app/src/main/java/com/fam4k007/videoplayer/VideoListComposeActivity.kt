package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.compose.VideoListScreen
import com.fam4k007.videoplayer.compose.VideoListScreenPaging
import com.fam4k007.videoplayer.database.VideoDatabase
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.Logger
import com.fam4k007.videoplayer.utils.NoMediaChecker
import com.fam4k007.videoplayer.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.get
import org.koin.androidx.compose.KoinAndroidContext

class VideoListComposeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "VideoListComposeActivity"
    }

    private val preferencesManager: com.fam4k007.videoplayer.manager.PreferencesManager by inject()
    private var folderPath: String = ""
    private var usePaging: Boolean = false  // 是否使用Paging3模式

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableEdgeToEdge()
        
        // 应用主题
        val currentTheme = ThemeManager.getCurrentTheme(this)
        setTheme(currentTheme.styleRes)

        val folderName = intent.getStringExtra("folder_name") ?: "视频列表"
        folderPath = intent.getStringExtra("folder_path") ?: ""
        val videos = intent.getParcelableArrayListExtra<VideoFileParcelable>("video_list") ?: arrayListOf()
        
        // 如果视频数量超过100个，使用Paging3模式
        usePaging = videos.size > 100
        Logger.d(TAG, "视频数量: ${videos.size}, 使用Paging3: $usePaging")

        setupContent(folderName, videos)
    }

    private fun setupContent(folderName: String, videos: ArrayList<VideoFileParcelable>) {
        val activity = this
        
        // 如果使用Paging3模式，需要先将视频保存到数据库
        if (usePaging && folderPath.isNotEmpty()) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        Logger.d(TAG, "开始将视频保存到数据库...")
                        val database: VideoDatabase = activity.get()
                        val entities = videos.map { video ->
                            com.fam4k007.videoplayer.database.VideoCacheEntity(
                                uri = video.uri,
                                name = video.name,
                                nameSortKey = com.fam4k007.videoplayer.database.VideoCacheEntity.generateSortKey(video.name),
                                path = video.path,
                                folderPath = folderPath,
                                folderName = folderName,
                                size = video.size,
                                duration = video.duration,
                                dateModified = video.dateAdded,
                                dateAdded = video.dateAdded,
                                lastScanned = System.currentTimeMillis()
                            )
                        }
                        database.videoCacheDao().insertVideos(entities)
                        Logger.d(TAG, "已保存 ${entities.size} 个视频到数据库")
                    } catch (e: Exception) {
                        Logger.e(TAG, "保存视频到数据库失败", e)
                    }
                }
            }
        }
        
        setContent {
            KoinAndroidContext {
                val themeColors = getThemeColors(ThemeManager.getCurrentTheme(activity).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = Color(0xFF212121),
                    surface = themeColors.background,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = Color(0xFF212121)
                )
            ) {
                // 根据视频数量选择不同的加载模式
                if (usePaging && folderPath.isNotEmpty()) {
                    // 大量视频使用Paging3防止OOM
                    VideoListScreenPaging(
                        folderName = folderName,
                        folderPath = folderPath,
                        onNavigateBack = { 
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        },
                        onOpenVideo = { video, allVideos -> 
                            openVideoPlayer(video, 0, folderName, allVideos)
                        },
                        onRescanFolder = { callback -> 
                            rescanFolderToDatabase { callback() }
                        },
                        preferencesManager = preferencesManager,
                        coroutineScope = lifecycleScope
                    )
                } else {
                    // 少量视频使用传统模式
                    VideoListScreen(
                        folderName = folderName,
                        initialVideos = videos,
                        onNavigateBack = { 
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        },
                        onOpenVideo = { video, index, allVideos -> 
                            openVideoPlayer(video, index, folderName, allVideos)
                        },
                        onRescanFolder = { callback -> rescanFolder(callback) },
                        preferencesManager = preferencesManager
                    )
                }
            }
            }
        }
    }

    private fun openVideoPlayer(
        video: VideoFileParcelable, 
        currentIndex: Int, 
        folderName: String, 
        allVideos: List<VideoFileParcelable>
    ) {
        Log.d(TAG, "播放视频: ${video.name}, 索引: $currentIndex")

        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.data = Uri.parse(video.uri)
        intent.putExtra("video_name", video.name)
        intent.putExtra("current_index", currentIndex)
        intent.putExtra("folderName", folderName)
        intent.putParcelableArrayListExtra("video_list", ArrayList(allVideos))
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    private fun rescanFolder(callback: (List<VideoFileParcelable>) -> Unit) {
        if (folderPath.isEmpty()) {
            callback(emptyList())
            return
        }
        
        lifecycleScope.launch {
            val newVideos = withContext(Dispatchers.IO) {
                scanVideosInFolder(folderPath)
            }
            callback(newVideos)
        }
    }
    
    private fun scanVideosInFolder(folderPath: String): List<VideoFileParcelable> {
        val videos = mutableListOf<VideoFileParcelable>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$folderPath%")
        // 添加明确的排序，避免系统默认限制
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        
        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn)
                    if (path.substringBeforeLast("/") == folderPath) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val dateAdded = cursor.getLong(dateColumn)
                        val uri = Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        ).toString()
                        
                        videos.add(
                            VideoFileParcelable(
                                uri = uri,
                                name = name,
                                path = path,
                                size = size,
                                duration = duration,
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rescanning folder", e)
        }
        
        return videos
    }
    
    /**
     * 重新扫描文件夹并保存到数据库（用于Paging3模式）
     */
    private fun rescanFolderToDatabase(callback: () -> Unit) {
        if (folderPath.isEmpty()) {
            callback()
            return
        }
        
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val videos = scanVideosInFolder(folderPath)
                    
                    // 保存到数据库
                    val database: VideoDatabase = get()
                    val entities = videos.map { video ->
                        com.fam4k007.videoplayer.database.VideoCacheEntity(
                            uri = video.uri,
                            name = video.name,
                            nameSortKey = com.fam4k007.videoplayer.database.VideoCacheEntity.generateSortKey(video.name),
                            path = video.path,
                            folderPath = folderPath,
                            folderName = folderPath.substringAfterLast("/"),
                            size = video.size,
                            duration = video.duration,
                            dateModified = video.dateAdded,
                            dateAdded = video.dateAdded,
                            lastScanned = System.currentTimeMillis()
                        )
                    }
                    database.videoCacheDao().insertVideos(entities)
                    Logger.d(TAG, "重新扫描完成，保存了 ${entities.size} 个视频到数据库")
                } catch (e: Exception) {
                    Logger.e(TAG, "重新扫描文件夹失败", e)
                }
            }
            callback()
        }
    }
}
