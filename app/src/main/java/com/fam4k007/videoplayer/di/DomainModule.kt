package com.fam4k007.videoplayer.di

import com.fam4k007.videoplayer.domain.player.Anime4KManager
import com.fam4k007.videoplayer.preferences.PreferencesManager
import com.fam4k007.videoplayer.manager.ThemeManager
import com.fam4k007.videoplayer.utils.ThumbnailCacheManager
import com.fam4k007.videoplayer.webdav.WebDavAccountManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 领域模块
 * 提供业务逻辑层的依赖注入（核心算法、管理器等）
 */
val domainModule = module {
    
    // PreferencesManager单例
    single { 
        PreferencesManager.getInstance(androidContext())
    }
    
    // Anime4KManager单例
    single { 
        Anime4KManager(androidContext())
    }
    
    // WebDavAccountManager单例
    single { 
        WebDavAccountManager.getInstance(androidContext())
    }
    
    // ThumbnailCacheManager单例
    single { 
        ThumbnailCacheManager.getInstance(androidContext())
    }
    
    // ThemeManager（工具类，无状态）
    factory { 
        ThemeManager 
    }
}
