package com.fam4k007.videoplayer.di

import com.fam4k007.videoplayer.database.VideoDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 数据库模块
 * 提供Room数据库和DAO的依赖注入
 */
val databaseModule = module {
    
    // VideoDatabase单例
    single { 
        VideoDatabase.getDatabase(androidContext())
    }
    
    // VideoCacheDao
    single { 
        get<VideoDatabase>().videoCacheDao() 
    }
    
    // PlaybackHistoryDao
    single { 
        get<VideoDatabase>().playbackHistoryDao() 
    }
}
