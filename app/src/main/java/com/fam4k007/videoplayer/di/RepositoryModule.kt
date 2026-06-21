package com.fam4k007.videoplayer.di

import com.fam4k007.videoplayer.PlaybackHistoryManager
import com.fam4k007.videoplayer.data.preferences.WebDavAccountDataSource
import com.fam4k007.videoplayer.preferences.PreferencesManager
import com.fam4k007.videoplayer.repository.BilibiliRepository
import com.fam4k007.videoplayer.bilibili.repository.BangumiRepository
import com.fam4k007.videoplayer.repository.MediaInfoRepository
import com.fam4k007.videoplayer.repository.PlayerRepository
import com.fam4k007.videoplayer.repository.SubtitleRepository
import com.fam4k007.videoplayer.repository.VideoRepository
import com.fam4k007.videoplayer.repository.WebDavRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 仓储模块
 * 提供Repository层的依赖注入
 */
val repositoryModule = module {
    
    // VideoRepository - 视频扫描和数据库操作
    single { 
        VideoRepository(
            context = androidContext(),
            database = get()
        )
    }
    
    // PlayerRepository - 播放历史和设置
    single { 
        PlayerRepository(
            context = androidContext(),
            database = get(),
            preferencesManager = get()
        )
    }
    
    // BilibiliRepository - B站API调用和数据解析
    single { 
        BilibiliRepository(
            authManager = get(),
            danDanPlayApi = get()
        )
    }
    
    // BangumiRepository - 番剧数据仓库
    single { 
        BangumiRepository(
            bangumiApi = get()
        )
    }
    
    // WebDAV DataSource - 账户数据持久化
    single { 
        WebDavAccountDataSource(context = androidContext())
    }
    
    // WebDavRepository - WebDAV文件操作
    single { 
        WebDavRepository(
            accountDataSource = get()
        )
    }
    
    // SubtitleRepository - 字幕搜索和下载
    single { 
        SubtitleRepository(
            context = androidContext()
        )
    }
    
    // MediaInfoRepository - 媒体信息提取
    single { 
        MediaInfoRepository(
            context = androidContext()
        )
    }
    
    // PlaybackHistoryManager（保留向后兼容，后续逐步迁移到PlayerRepository）
    single { 
        PlaybackHistoryManager(androidContext())
    }
}

