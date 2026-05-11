package com.fam4k007.videoplayer.di

/**
 * Koin模块总入口
 * 集中管理所有DI模块
 */
val appModules = listOf(
    databaseModule,
    networkModule,
    repositoryModule,
    domainModule
)
