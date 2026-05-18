package com.fam4k007.videoplayer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fam4k007.videoplayer.ui.screens.BiliBiliDanmakuScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import com.fam4k007.videoplayer.ui.viewmodels.BiliBiliDanmakuViewModel
import org.koin.androidx.compose.KoinAndroidContext

class BiliBiliDanmakuComposeActivity : BaseActivity() {

    private val viewModel: BiliBiliDanmakuViewModel by viewModels()
    private var themeRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            val revision = themeRevision
            val savedFolderUri by viewModel.savedFolderUri.collectAsStateWithLifecycle()
            val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
            val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
            val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
            val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
            val downloadWholeSeason by viewModel.downloadWholeSeason.collectAsStateWithLifecycle()

            // 处理错误消息
            LaunchedEffect(errorMessage) {
                errorMessage?.let {
                    Toast.makeText(this@BiliBiliDanmakuComposeActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearErrorMessage()
                }
            }

            // 处理成功消息
            LaunchedEffect(successMessage) {
                successMessage?.let {
                    Toast.makeText(this@BiliBiliDanmakuComposeActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccessMessage()
                }
            }

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
                        downloadWholeSeason = downloadWholeSeason,
                        onBack = {
                            finish()
                        },
                        onFolderSelected = { uri ->
                            viewModel.setFolderUri(uri, contentResolver)
                        },
                        onDownloadDanmaku = { url, wholeSeason ->
                            viewModel.startDownload(url, wholeSeason)
                        },
                        onModeChanged = { wholeSeason ->
                            viewModel.setDownloadMode(wholeSeason)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        themeRevision++
    }
}
