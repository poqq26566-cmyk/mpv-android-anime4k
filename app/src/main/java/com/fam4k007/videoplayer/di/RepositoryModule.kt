package com.fam4k007.videoplayer.di

import com.fam4k007.videoplayer.PlaybackHistoryManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 仓储模块
 * 提供Repository层的依赖注入
 * 
 * 注意：目前保留现有的Manager类，后续阶段会逐步重构为标准的Repository
 */
val repositoryModule = module {
    
    // PlaybackHistoryManager（目前类似Repository的角色）
    single { 
        PlaybackHistoryManager(androidContext())
    }
}
