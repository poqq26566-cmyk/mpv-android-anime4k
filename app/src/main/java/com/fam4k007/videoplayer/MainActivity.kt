package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.fam4k007.videoplayer.navigation.AppNavGraph
import com.fam4k007.videoplayer.preferences.PreferencesManager
import com.fam4k007.videoplayer.ui.components.UpdateDialog
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import com.fam4k007.videoplayer.utils.UpdateManager
import kotlinx.coroutines.launch
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : BaseActivity() {
    
    private var historyManager: PlaybackHistoryManager? = null
    
    // 更新弹窗状态
    private var showUpdateDialog by mutableStateOf(false)
    private var currentUpdateInfo: UpdateManager.UpdateInfo? by mutableStateOf(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // 检查用户是否已同意协议
        if (!UserAgreementActivity.isAgreed(this)) {
            val intent = Intent(this, UserAgreementActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
            return
        }
        
        // 使用IdleHandler在主线程空闲时初始化历史记录管理器
        android.os.Looper.myQueue().addIdleHandler {
            historyManager = PlaybackHistoryManager(this)
            com.fam4k007.videoplayer.utils.Logger.d("MainActivity", "PlaybackHistoryManager initialized in idle")
            false
        }
        
        // 延迟检查更新（5秒后，避免阻塞启动）
        val prefs = PreferencesManager.getInstance(this)
        if (prefs.isAutoCheckUpdateEnabled()) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                checkForUpdateSilently()
            }, 5000)
        }
        
        setupContent()
    }
    
    private fun setupContent() {
        val activity = this
        
        setContent {
            KoinAndroidContext {
                val themeController = ThemeController.from(activity)
                val navController = rememberNavController()
                
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    AppNavGraph(
                        navController = navController,
                        historyManager = historyManager ?: PlaybackHistoryManager(activity),
                    )
                    
                    // 更新弹窗 - 使用统一组件
                    if (showUpdateDialog && currentUpdateInfo != null) {
                        val info = currentUpdateInfo!!
                        val prefs = PreferencesManager.getInstance(activity)
                        
                        UpdateDialog(
                            updateInfo = info,
                            onDismiss = { showUpdateDialog = false },
                            onDownload = { url ->
                                UpdateManager.openDownloadPage(activity, url)
                                showUpdateDialog = false
                            },
                            onIgnore = {
                                prefs.setIgnoredUpdateVersion(info.versionName)
                                showUpdateDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 静默检查更新
     * 如果用户已忽略此版本则不弹窗
     */
    private fun checkForUpdateSilently() {
        val prefs = PreferencesManager.getInstance(this)
        val ignoredVersion = prefs.getIgnoredUpdateVersion()
        
        lifecycleScope.launch {
            try {
                val updateInfo = UpdateManager.checkForUpdate(this@MainActivity)
                if (updateInfo != null && updateInfo.versionName != ignoredVersion) {
                    currentUpdateInfo = updateInfo
                    showUpdateDialog = true
                }
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }
}
