package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.fam4k007.videoplayer.ui.screens.PlaybackHistoryScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import org.koin.androidx.compose.KoinAndroidContext

class PlaybackHistoryComposeActivity : BaseActivity() {

    private var themeRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val historyManager = PlaybackHistoryManager(this)

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
                        historyManager = historyManager,
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
    }
}
