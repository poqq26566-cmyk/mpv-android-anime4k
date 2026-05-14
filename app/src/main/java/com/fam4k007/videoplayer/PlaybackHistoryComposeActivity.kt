package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.fam4k007.videoplayer.presentation.PlaybackHistoryViewModel
import com.fam4k007.videoplayer.ui.screens.PlaybackHistoryScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 播放历史页面
 * 已迁移到新架构：Clean Architecture + MVVM + Koin
 */
class PlaybackHistoryComposeActivity : BaseActivity() {

    private var themeRevision by mutableIntStateOf(0)
    
    // 通过 Koin 注入 ViewModel
    private val viewModel: PlaybackHistoryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@PlaybackHistoryComposeActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    PlaybackHistoryScreen(
                        viewModel = viewModel,
                        onBack = {
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        },
                        onPlayVideo = { uri, startPosition ->
                            val intent = Intent(this@PlaybackHistoryComposeActivity, VideoPlayerActivity::class.java).apply {
                                data = uri
                                putExtra("lastPosition", startPosition)
                            }
                            startActivity(intent)
                            startActivityWithDefaultTransition()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        themeRevision++
        // 页面恢复时重新加载历史记录
        viewModel.loadHistory()
    }
}
