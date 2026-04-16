package com.fam4k007.videoplayer

import android.content.Context
import androidx.activity.ComponentActivity

/**
 * 基类 ComponentActivity
 * 用于 Compose Activity，强制使用英文语言
 */
abstract class BaseComponentActivity : ComponentActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppApplication.updateLocale(newBase))
    }
}
