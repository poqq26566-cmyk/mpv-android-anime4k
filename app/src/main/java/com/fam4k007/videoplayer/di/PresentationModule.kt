package com.fam4k007.videoplayer.di

import com.fam4k007.videoplayer.presentation.BiliBiliPlayViewModel
import com.fam4k007.videoplayer.presentation.BilibiliViewModel
import com.fam4k007.videoplayer.presentation.LibraryViewModel
import com.fam4k007.videoplayer.presentation.PlayerViewModel
import com.fam4k007.videoplayer.presentation.SettingsViewModel
import com.fam4k007.videoplayer.presentation.SubtitleSearchViewModel
import com.fam4k007.videoplayer.presentation.TVBrowserViewModel
import com.fam4k007.videoplayer.presentation.WebDavViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Presentation层DI模块
 * 提供所有ViewModel的依赖注入
 */
val presentationModule = module {
    
    // 播放器ViewModel
    viewModel { PlayerViewModel(get()) }
    
    // 视频库ViewModel
    viewModel { LibraryViewModel(get()) }
    
    // B站功能ViewModel
    viewModel { BilibiliViewModel(get()) }
    
    // 设置ViewModel
    viewModel { SettingsViewModel(get()) }
    
    // TV浏览器ViewModel
    viewModel { TVBrowserViewModel() }
    
    // B站番剧播放ViewModel
    viewModel { BiliBiliPlayViewModel(get()) }

    // WebDAV ViewModel
    viewModel { WebDavViewModel(get(), get()) }
    
    // 字幕搜索ViewModel
    viewModel { SubtitleSearchViewModel(get(), androidContext()) }
}
