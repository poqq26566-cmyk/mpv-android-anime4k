package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.ui.screens.BiliBiliDanmakuScreen
import com.fam4k007.videoplayer.ui.screens.DownloadProgress
import com.fam4k007.videoplayer.danmaku.BiliBiliDanmakuDownloadManager
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.KoinAndroidContext

class BiliBiliDanmakuComposeActivity : BaseActivity() {

    private var savedFolderUri: Uri? = null
    private lateinit var downloadManager: BiliBiliDanmakuDownloadManager
    private var downloadProgress by mutableStateOf(DownloadProgress())
    private var isDownloading by mutableStateOf(false)
    private var themeRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        downloadManager = BiliBiliDanmakuDownloadManager(this)

        // 读取已保存的文件夹URI
        val savedUriString = getSharedPreferences("bilibili_danmaku", MODE_PRIVATE)
            .getString("save_directory_uri", null)
        savedFolderUri = savedUriString?.let { Uri.parse(it) }

        setContent {
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@BiliBiliDanmakuComposeActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    BiliBiliDanmakuScreen(
                        savedFolderUri = savedFolderUri,
                        downloadProgress = downloadProgress,
                        isDownloading = isDownloading,
                        onBack = {
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        },
                        onFolderSelected = { uri: Uri -> handleFolderSelected(uri) },
                        onDownloadDanmaku = { url: String, downloadWholeSeason: Boolean ->
                            startDownload(url, downloadWholeSeason)
                        }
                    )
                }
            }
        }
    }

    private fun handleFolderSelected(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)

            getSharedPreferences("bilibili_danmaku", MODE_PRIVATE)
                .edit()
                .putString("save_directory_uri", uri.toString())
                .apply()

            savedFolderUri = uri
            Toast.makeText(this, "文件夹设置成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startDownload(url: String, downloadWholeSeason: Boolean) {
        if (!downloadManager.isValidBilibiliUrl(url)) {
            Toast.makeText(this, "请输入有效的B站视频/番剧链接", Toast.LENGTH_SHORT).show()
            return
        }

        isDownloading = true
        downloadProgress = DownloadProgress()
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    downloadManager.downloadDanmaku(url, savedFolderUri!!, downloadWholeSeason) { current, total, epTitle, success, fail ->
                        launch(Dispatchers.Main) {
                            downloadProgress = DownloadProgress(
                                current = current,
                                total = total,
                                currentTitle = epTitle,
                                successCount = success,
                                failedCount = fail
                            )
                        }
                    }
                }

                isDownloading = false
                
                when (result) {
                    is BiliBiliDanmakuDownloadManager.DownloadResult.Success -> {
                        Toast.makeText(
                            this@BiliBiliDanmakuComposeActivity,
                            "下载成功",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is BiliBiliDanmakuDownloadManager.DownloadResult.Error -> {
                        Toast.makeText(
                            this@BiliBiliDanmakuComposeActivity,
                            "下载失败: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                isDownloading = false
                Toast.makeText(
                    this@BiliBiliDanmakuComposeActivity,
                    "下载失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        themeRevision++
    }
}
