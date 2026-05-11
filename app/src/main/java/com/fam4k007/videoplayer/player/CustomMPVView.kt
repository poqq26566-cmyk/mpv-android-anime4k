package com.fam4k007.videoplayer.player

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.MPVLib

/**
 * 自定义MPV视图
 * 继承BaseMPVView,封装MPV配置
 * 
 * 重要: MPV必须在Activity的onCreate中通过 initialize(filesDir, cacheDir) 方法初始化
 * 不要在View的生命周期回调中初始化MPV
 */
class CustomMPVView(context: Context, attrs: AttributeSet) : BaseMPVView(context, attrs) {

    companion object {
        private const val TAG = "CustomMPVView"
    }

    /**
     * 初始化MPV选项
     * 会在MPVLib.create()之后、MPVLib.init()之前被调用
     */
    override fun initOptions() {
        Log.d(TAG, "Initializing MPV options")
        
        // 视频输出配置
        setVo("gpu")
        MPVLib.setOptionString("hwdec", "auto")
        MPVLib.setOptionString("ao", "audiotrack,opensles")
        
        // ========== 音量配置 ==========
        // 允许音量超过100%(最高300%)
        MPVLib.setOptionString("volume-max", "300")
        // 启用软件音量控制,允许音量超过100%
        MPVLib.setOptionString("softvol", "yes")
        // 设置音量控制模式
        MPVLib.setOptionString("audio-normalize-downmix", "no")
        
        MPVLib.setOptionString("keep-open", "yes")
        MPVLib.setOptionString("gpu-context", "android")

        // 视频适应屏幕
        MPVLib.setOptionString("keepaspect", "yes")
        MPVLib.setOptionString("panscan", "0.0")
        MPVLib.setOptionString("video-aspect-override", "-1")

        // ========== 字幕配置 ==========
        // 自动加载外部字幕文件
        MPVLib.setOptionString("sub-auto", "fuzzy")
        // 字幕文件编码
        MPVLib.setOptionString("sub-codepage", "auto")
        // 首选字幕语言
        MPVLib.setOptionString("slang", "zh,chi,zho,chs,cht,zh-CN,zh-TW,en,eng")
        
        // libass 字体配置（使用系统字体）
        val preferencesManager = com.fam4k007.videoplayer.preferences.PreferencesManager.getInstance(context)
        val systemFontName = preferencesManager.getSystemFontName()
        
        MPVLib.setOptionString("sub-font-provider", "auto")
        MPVLib.setOptionString("sub-fonts-dir", "/system/fonts")
        MPVLib.setOptionString("sub-font", systemFontName)
        MPVLib.setOptionString("embeddedfonts", "yes")
        
        Log.d(TAG, "Using system font: $systemFontName")
        
        // 字幕显示位置
        MPVLib.setOptionString("sub-use-margins", "yes")
        MPVLib.setOptionString("sub-ass-force-margins", "yes")
        MPVLib.setOptionString("blend-subtitles", "video")
        // 字幕样式
        MPVLib.setOptionString("sub-font-size", "55")
        MPVLib.setOptionString("sub-border-size", "3")

        // TLS配置 - 禁用证书验证以支持在线视频
        MPVLib.setOptionString("tls-verify", "no")
        
        // HTTP配置 - 添加User-Agent避免被服务器拒绝
        MPVLib.setOptionString("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        MPVLib.setOptionString("http-header-fields", "Accept: */*")
        
        // 流媒体配置 - 改进在线视频处理，支持HLS(m3u8)
        MPVLib.setOptionString("stream-lavf-o", "protocol_whitelist=file,http,https,tcp,tls,crypto,hls,applehttp")
        MPVLib.setOptionString("hls-bitrate", "max")  // HLS使用最高码率
        MPVLib.setOptionString("http-allow-redirect", "yes")  // 允许HTTP重定向
        
        // 缓存限制 - 针对m3u8等流媒体增大缓存
        val cacheMegs = 128  // 增大到128MB
        MPVLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
        MPVLib.setOptionString("demuxer-max-back-bytes", "${cacheMegs * 1024 * 1024}")
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("cache-secs", "180")  // 缓存180秒（3分钟）

        // 默认播放速度
        MPVLib.setOptionString("speed", "1.0")

        Log.d(TAG, "MPV options initialized")
    }

    /**
     * 在 MPV 初始化之后执行的配置
     * 会在 MPVLib.init() 之后被调用
     */
    override fun postInitOptions() {
        Log.d(TAG, "Post-init options - MPV fully initialized")
        // 这里可以添加需要在 MPV 完全初始化后才能设置的选项
    }

    /**
     * 观察MPV属性变化
     * 会在MPVLib.init()之后被调用
     */
    override fun observeProperties() {
        Log.d(TAG, "Setting up property observers")
        
        // BaseMPVView 会自动处理属性观察
        // 缓冲状态将通过 PlaybackEngine 的进度更新来检测
        
        Log.d(TAG, "Property observers initialized")
    }
}
