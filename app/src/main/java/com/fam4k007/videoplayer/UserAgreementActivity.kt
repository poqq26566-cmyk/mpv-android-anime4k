package com.fam4k007.videoplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fam4k007.videoplayer.ui.screens.UserAgreementScreen
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme

/**
 * 用户协议 Activity
 * 第一次启动应用时显示，用户必须同意协议才能继续使用
 * 使用应用统一主题和现代 Material 3 风格
 */
class UserAgreementActivity : BaseActivity() {

    companion object {
        private const val PREFS_NAME = "user_agreement_prefs"
        private const val KEY_AGREED = "user_agreed"
        private const val EXTRA_PREVIEW_MODE = "extra_preview_mode"

        /**
         * 检查用户是否已同意协议
         */
        fun isAgreed(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AGREED, false)
        }

        /**
         * 设置用户已同意协议
         */
        fun setAgreed(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AGREED, true).apply()
        }

        /**
         * 创建预览模式的 Intent
         */
        fun previewIntent(context: Context): Intent {
            return Intent(context, UserAgreementActivity::class.java).apply {
                putExtra(EXTRA_PREVIEW_MODE, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val isPreviewMode = intent.getBooleanExtra(EXTRA_PREVIEW_MODE, false)

        setContent {
            val themeController = ThemeController.from(this)

            VideoPlayerTheme(
                appTheme = themeController.getCurrentTheme(),
                darkMode = themeController.getDarkMode(),
                amoledMode = themeController.getAmoledMode()
            ) {
                UserAgreementScreen(
                    showActions = !isPreviewMode,
                    onAgree = {
                        // 保存已同意状态
                        setAgreed(this)
                        // 启动主界面
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    },
                    onDecline = {
                        // 拒绝则直接退出应用
                        finishAffinity()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 预览模式允许返回，启动模式禁用返回键
        val isPreviewMode = intent.getBooleanExtra(EXTRA_PREVIEW_MODE, false)
        if (isPreviewMode) {
            super.onBackPressed()
        }
        // 启动模式下不调用 super.onBackPressed()，必须选择同意或拒绝
    }
}
