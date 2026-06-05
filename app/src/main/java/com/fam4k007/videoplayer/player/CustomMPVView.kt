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

        // 应用用户选择的解码器预设（控制缩放算法、渲染质量等）
        val prefsManager = com.fam4k007.videoplayer.preferences.PreferencesManager.getInstance(context)
        val mpvProfile = prefsManager.getMpvProfile()
        MPVLib.setOptionString("profile", mpvProfile)
        Log.d(TAG, "Applied MPV profile: $mpvProfile")
        
        // 视频输出配置
        setVo("gpu")
        // HW解码降级链: 优先硬解(HW+)→ 硬解复制模式(HW) → 软解(no)，避免auto在某些设备上卡死
        MPVLib.setOptionString("hwdec", "mediacodec,mediacodec-copy,no")
        MPVLib.setOptionString("hwdec-codecs", "all")
        MPVLib.setOptionString("ao", "audiotrack,opensles")
        
        // ========== HDR/杜比视界色彩处理配置 ==========
        // 目标色彩空间：auto让MPV自动检测屏幕色彩空间（SDR屏幕用bt.709，HDR屏幕用bt.2020）
        MPVLib.setOptionString("target-colorspace-hint", "yes")
        MPVLib.setOptionString("target-prim", "auto")
        MPVLib.setOptionString("target-trc", "auto")
        
        // HDR tone-mapping 算法：mobius 是最平衡的算法，适合大多数内容
        // 可选: hable(电影感), reinhard(柔和), bt.2390(标准), mobius(平衡)
        MPVLib.setOptionString("tone-mapping", "mobius")
        
        // 动态计算 HDR 峰值亮度，提升杜比视界/HDR10 显示效果
        MPVLib.setOptionString("hdr-compute-peak", "yes")
        
        // HDR 峰值亮度检测：基于整个视频场景分析
        MPVLib.setOptionString("hdr-peak-percentile", "99.995")
        
        // tone-mapping 参数调整（mobius 算法的过渡点）
        MPVLib.setOptionString("tone-mapping-param", "0.3")
        
        // 色彩管理：启用 ICC 配置文件自动检测（Android 8.0+ 支持）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            MPVLib.setOptionString("icc-profile-auto", "yes")
        }
        
        Log.d(TAG, "HDR/Dolby Vision color processing configured")
        
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
        
        // 缓存限制 - 根据Android版本动态调整，兼顾性能和内存
        val cacheMegs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) 128 else 64
        MPVLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
        // 后向缓存（用于回退）可以小一些，节省内存
        MPVLib.setOptionString("demuxer-max-back-bytes", "${(cacheMegs / 2) * 1024 * 1024}")
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("cache-secs", "180")  // 缓存180秒（3分钟）

        // 默认播放速度
        MPVLib.setOptionString("speed", "1.0")
        
        // 胶片颗粒用CPU处理，避免GPU渲染开销（省电+流畅）
        MPVLib.setOptionString("vd-lavc-film-grain", "cpu")
        
        // 精确跳转设置与用户偏好同步
        // Seek时：精确模式用absolute，非精确用absolute+keyframes（已在PlaybackEngine中实现）
        // 这里设置MPV全局默认值，让hr-seek行为与用户偏好一致
        val preciseSeek = preferencesManager.isPreciseSeekingEnabled()
        MPVLib.setOptionString("hr-seek", if (preciseSeek) "yes" else "no")
        MPVLib.setOptionString("hr-seek-framedrop", if (preciseSeek) "no" else "yes")

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
