package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fam4k007.videoplayer.ui.screens.SettingsScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import org.koin.androidx.compose.KoinAndroidContext

/**
 * Compose 版本的设置 Activity
 * 保留作为兼容入口（其他Activity可能仍通过Intent启动此页面）
 */
class SettingsComposeActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
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
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        },
                        onNavigateToPlaybackSettings = {
                            startActivity(Intent(this@SettingsComposeActivity, PlaybackSettingsComposeActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        },
                        onNavigateToPlaybackHistory = {
                            startActivity(Intent(this@SettingsComposeActivity, PlaybackHistoryComposeActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        },
                        onNavigateToAbout = {
                            startActivity(Intent(this@SettingsComposeActivity, AboutComposeActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    )
                }
            }
        }
    }
}
