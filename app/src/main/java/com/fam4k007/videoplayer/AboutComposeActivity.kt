package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.fam4k007.videoplayer.ui.screens.AboutScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import org.koin.androidx.compose.KoinAndroidContext

class AboutComposeActivity : BaseActivity() {

    private var themeRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }

        setContent {
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@AboutComposeActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    AboutScreen(
                        versionName = versionName,
                        onBack = {
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        },
                        onNavigateToLicense = {
                            startActivity(Intent(this@AboutComposeActivity, LicenseActivity::class.java))
                            startActivityWithDefaultTransition()
                        },
                        onNavigateToFeedback = {
                            startActivity(Intent(this@AboutComposeActivity, FeedbackActivity::class.java))
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
