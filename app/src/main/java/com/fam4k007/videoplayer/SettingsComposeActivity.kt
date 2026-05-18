package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.fam4k007.videoplayer.ui.screens.SettingsScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import org.koin.androidx.compose.KoinAndroidContext

/**
 * Compose 版本的设置 Activity
 * 保留作为兼容入口（其他Activity可能仍通过Intent启动此页面）
 */
class SettingsComposeActivity : ComponentActivity() {

    private var themeRevision by mutableIntStateOf(0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@SettingsComposeActivity)
                
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    SettingsScreen(
                        onNavigateBack = {
                            finish()
                        },
                        onNavigateToPlaybackSettings = {
                            startActivity(Intent(this@SettingsComposeActivity, PlaybackSettingsComposeActivity::class.java))
                        },
                        onNavigateToPlaybackHistory = {
                            startActivity(Intent(this@SettingsComposeActivity, PlaybackHistoryComposeActivity::class.java))
                        },
                        onNavigateToAbout = {
                            startActivity(Intent(this@SettingsComposeActivity, AboutComposeActivity::class.java))
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
