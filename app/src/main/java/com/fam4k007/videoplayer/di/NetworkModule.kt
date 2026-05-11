package com.fam4k007.videoplayer.di

import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.dandanplay.DanDanPlayApi
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
    
    // DanDanPlayApi单例
    single { 
        DanDanPlayApi() 
    }
}
