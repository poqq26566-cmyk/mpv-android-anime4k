package com.fam4k007.videoplayer

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import com.fam4k007.videoplayer.database.VideoDatabase
import com.fam4k007.videoplayer.manager.ThemeManager
import com.fam4k007.videoplayer.utils.CrashHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用全局Application类
 */
class AppApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(updateLocale(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化全局异常处理器（优先级最高）
        CrashHandler.init(this)
        
        // 初始化主题
        ThemeManager.initTheme(this)
        
        // 后台预热数据库连接（减少首次查询延迟）
        applicationScope.launch {
            try {
                val db = VideoDatabase.getDatabase(this@AppApplication)
                // 执行一个简单查询预热连接
                db.videoCacheDao().getVideoCount()
                com.fam4k007.videoplayer.utils.Logger.d("AppApplication", "Database warmed up")
            } catch (e: Exception) {
                // 预热失败不影响应用启动
                com.fam4k007.videoplayer.utils.Logger.e("AppApplication", "Failed to warm up database", e)
            }
        }
    }
    
    companion object {
        /**
         * 强制设置应用语言为英文
         */
        fun updateLocale(context: Context): Context {
            val locale = Locale.ENGLISH
            Locale.setDefault(locale)
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            
            return context.createConfigurationContext(config)
        }
    }
}
