package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.fam4k007.videoplayer.navigation.AppNavGraph
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
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            checkForUpdateSilently()
        }, 5000)
        
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
                    
                    // 更新弹窗
                    if (showUpdateDialog && currentUpdateInfo != null) {
                        UpdateDialog(
                            updateInfo = currentUpdateInfo!!,
                            onDismiss = { showUpdateDialog = false },
                            onDownload = { url ->
                                UpdateManager.openDownloadPage(activity, url)
                                showUpdateDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
    
    @androidx.compose.runtime.Composable
    private fun UpdateDialog(
        updateInfo: UpdateManager.UpdateInfo,
        onDismiss: () -> Unit,
        onDownload: (String) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "发现新版本 ${updateInfo.versionName}",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = if (updateInfo.releaseNotes.isNotEmpty()) {
                        "更新内容：\n${updateInfo.releaseNotes}"
                    } else {
                        "发现新版本，是否立即下载？"
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { onDownload(updateInfo.downloadUrl) }) {
                    Text("立即下载")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("稍后提醒")
                }
            }
        )
    }
    
    private fun checkForUpdateSilently() {
        lifecycleScope.launch {
            try {
                val updateInfo = UpdateManager.checkForUpdate(this@MainActivity)
                if (updateInfo != null) {
                    showUpdateDialog(updateInfo)
                }
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }
    
    private fun showUpdateDialog(updateInfo: UpdateManager.UpdateInfo) {
        currentUpdateInfo = updateInfo
        showUpdateDialog = true
    }
}
