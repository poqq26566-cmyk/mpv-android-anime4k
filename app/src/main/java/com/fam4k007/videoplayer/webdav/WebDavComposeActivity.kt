package com.fam4k007.videoplayer.webdav

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.fam4k007.videoplayer.BaseComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * WebDAV 账户管理 Compose Activity
 */
class WebDavComposeActivity : BaseComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableEdgeToEdge()
        
        setContent {
            val themeColors = getThemeColors(this, ThemeManager.getCurrentTheme(this).themeName)
            
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = Color(0xFF212121),
                    surface = themeColors.background,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = Color(0xFF212121)
                )
            ) {
                WebDavAccountListScreen(
                    onNavigateBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onAccountSelected = { account ->
                        val intent = Intent(this, WebDavBrowserComposeActivity::class.java)
                        intent.putExtra("account_id", account.id)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                )
            }
        }
    }
}
