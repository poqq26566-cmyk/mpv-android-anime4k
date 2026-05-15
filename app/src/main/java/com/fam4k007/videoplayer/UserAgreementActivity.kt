package com.fam4k007.videoplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.fam4k007.videoplayer.ui.screens.UserAgreementScreen

/**
 * 用户协议 Activity - 使用 Compose 实现
 * 第一次启动应用时显示，用户必须同意协议才能继续使用
 */
class UserAgreementActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "user_agreement_prefs"
        private const val KEY_AGREED = "user_agreed"

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    UserAgreementScreen(
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
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 禁用返回键，必须选择同意或拒绝
        // 不调用 super.onBackPressed()
    }
}
