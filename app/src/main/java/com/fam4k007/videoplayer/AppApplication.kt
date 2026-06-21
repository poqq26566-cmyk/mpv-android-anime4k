package com.fam4k007.videoplayer

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.fam4k007.videoplayer.database.VideoDatabase
import com.fam4k007.videoplayer.di.appModules
import com.fam4k007.videoplayer.preferences.PreferencesManager
import com.fam4k007.videoplayer.utils.CrashHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * 应用全局Application类
 */
class AppApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化全局异常处理器（优先级最高）
        CrashHandler.init(this)
        
        // 初始化Koin依赖注入
        startKoin {
            // Koin日志（开发模式用ERROR，生产环境用NONE）
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            // 注入Android Context
            androidContext(this@AppApplication)
            // 加载所有模块
            modules(appModules)
        }
        
        // 初始化主题
        val themePrefs = PreferencesManager.getInstance(this)
        val themeModeStr = themePrefs.getThemeMode()
        val nightMode = when (themeModeStr.lowercase()) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "system" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            }
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        
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
}
