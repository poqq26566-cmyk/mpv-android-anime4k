package com.fam4k007.videoplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fam4k007.videoplayer.ui.screens.BiliBiliPlayScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import org.koin.androidx.compose.KoinAndroidContext

/**
 * B站番剧播放页面
 */
class BiliBiliPlayActivity : ComponentActivity() {

    private var themeRevision by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // themeRevision 变化时触发重组，重新读取主题
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@BiliBiliPlayActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    BiliBiliPlayScreen(
                        onBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从设置页返回时，递增 revision 触发主题重组
        themeRevision++
    }
}
