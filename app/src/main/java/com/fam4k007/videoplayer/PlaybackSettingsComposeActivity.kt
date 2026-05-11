package com.fam4k007.videoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.fam4k007.videoplayer.ui.screens.PlaybackSettingsScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import org.koin.androidx.compose.KoinAndroidContext

/**
 * Compose 版本的播放设置 Activity
 */
class PlaybackSettingsComposeActivity : ComponentActivity() {

    private var themeRevision by mutableIntStateOf(0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@PlaybackSettingsComposeActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    PlaybackSettingsScreen(
                        onNavigateBack = {
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
