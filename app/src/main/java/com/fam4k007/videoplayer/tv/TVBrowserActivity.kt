package com.fam4k007.videoplayer.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.fam4k007.videoplayer.ui.screens.TVBrowserScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import org.koin.androidx.compose.KoinAndroidContext

/**
 * TV浏览器 - 带视频嗅探功能的WebView
 */
class TVBrowserActivity : ComponentActivity() {
    companion object {
        const val EXTRA_URL = "extra_url"

        fun start(context: Context, url: String = "") {
            val intent = Intent(context, TVBrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            context.startActivity(intent)
        }
    }

    private var webView: WebView? = null
    private var themeRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialUrl = intent.getStringExtra(EXTRA_URL) ?: ""

        setContent {
            val revision = themeRevision
            KoinAndroidContext {
                val themeController = ThemeController.from(this@TVBrowserActivity)

                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    TVBrowserScreen(
                        initialUrl = initialUrl,
                        onWebViewCreated = { webView = it },
                        onBackPressed = { onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        themeRevision++
        webView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }
}
