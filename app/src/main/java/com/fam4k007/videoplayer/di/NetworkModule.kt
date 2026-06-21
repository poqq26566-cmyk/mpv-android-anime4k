package com.fam4k007.videoplayer.di

import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.bilibili.api.BiliBangumiApi
import com.fam4k007.videoplayer.dandanplay.DanDanPlayApi
import com.fam4k007.videoplayer.preferences.PreferencesManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 网络模块
 * 提供网络相关服务的依赖注入
 */
val networkModule = module {
    
    // BiliBiliAuthManager单例
    single { 
        BiliBiliAuthManager(androidContext())
    }
    
    // BiliBangumiApi单例
    single { 
        BiliBangumiApi(authManager = get())
    }
    
    // DanDanPlayApi - 使用 factory，默认使用第一个启用的服务器
    factory { 
        val preferencesManager = get<PreferencesManager>()
        val firstServer = preferencesManager.getEnabledDanmakuServers().firstOrNull()
        DanDanPlayApi(
            if (firstServer == null || firstServer.isDefault) null else firstServer.url
        )
    }
}
