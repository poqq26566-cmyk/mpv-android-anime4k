package com.fam4k007.videoplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.compose.FolderBrowserScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import com.fam4k007.videoplayer.utils.NoMediaChecker
import com.fam4k007.videoplayer.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext

class VideoBrowserComposeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "VideoBrowserCompose"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private val preferencesManager: com.fam4k007.videoplayer.manager.PreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示，让内容可以绘制到状态栏区域
        enableEdgeToEdge()
        
        // 应用主题
        val currentTheme = ThemeManager.getCurrentTheme(this)
        setTheme(currentTheme.styleRes)

        setupContent()
    }

    private fun setupContent() {
        val activity = this
        
        setContent {
            KoinAndroidContext {
                val themeController = ThemeController.from(activity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    FolderBrowserScreen(
                        hasPermission = checkPermissions(),
                        onRequestPermission = { requestStoragePermission() },
                        onScanVideos = { callback -> scanVideoFiles(callback) },
                        onNavigateBack = { 
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        },
                        onOpenFolder = { folder -> openVideoList(folder) },
                        preferencesManager = preferencesManager
                    )
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            setupContent()
        }
    }

    override fun onResume() {
        super.onResume()
        setupContent()
    }

    private fun scanVideoFiles(callback: (List<VideoFolder>) -> Unit) {
        lifecycleScope.launch {
            try {
                val scannedFolders = withContext(Dispatchers.IO) {
                    val projection = arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Video.Media.DATE_ADDED
                    )

                    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

                    val cursor = contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                    )

                    val folderMap = mutableMapOf<String, MutableList<VideoFile>>()

                    cursor?.use {
                        val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                        val pathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                        val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                        val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                        val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                        while (it.moveToNext()) {
                            val id = it.getLong(idColumn)
                            val name = it.getString(nameColumn)
                            val path = it.getString(pathColumn)
                            val size = it.getLong(sizeColumn)
                            val duration = it.getLong(durationColumn)
                            val dateAdded = it.getLong(dateColumn)

                            if (NoMediaChecker.fileInNoMediaFolder(path)) {
                                continue
                            }

                            val file = java.io.File(path)
                            if (!file.exists()) {
                                continue
                            }

                            val uri = Uri.withAppendedPath(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                            val folderPath = path.substringBeforeLast("/")

                            val videoFile = VideoFile(
                                uri = uri.toString(),
                                name = name,
                                path = path,
                                size = size,
                                duration = duration,
                                dateAdded = dateAdded
                            )

                            folderMap.getOrPut(folderPath) { mutableListOf() }.add(videoFile)
                        }
                    }

                    val folders = mutableListOf<VideoFolder>()
                    folderMap.forEach { (path, videos) ->
                        val folderName = path.substringAfterLast("/")
                        folders.add(
                            VideoFolder(
                                folderPath = path,
                                folderName = folderName.ifEmpty { "根目录" },
                                videoCount = videos.size,
                                videos = videos
                            )
                        )
                    }

                    folders.sortByDescending { it.videoCount }

                    Log.d(TAG, "扫描完成，找到 ${folders.size} 个文件夹")
                    
                    folders
                }
                
                callback(scannedFolders)
                
            } catch (e: Exception) {
                Log.e(TAG, "扫描视频文件失败", e)
                callback(emptyList())
            }
        }
    }

    private fun openVideoList(folder: VideoFolder) {
        val intent = Intent(this, VideoListComposeActivity::class.java)
        intent.putExtra("folder_name", folder.folderName)
        intent.putExtra("folder_path", folder.folderPath)
        intent.putParcelableArrayListExtra("video_list", ArrayList(folder.videos.map {
            VideoFileParcelable(it.uri, it.name, it.path, it.size, it.duration, it.dateAdded)
        }))
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
