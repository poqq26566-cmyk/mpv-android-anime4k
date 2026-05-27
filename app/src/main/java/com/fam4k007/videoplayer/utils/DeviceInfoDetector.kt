package com.fam4k007.videoplayer.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import android.view.Display
import java.util.Locale

/**
 * 设备信息检测器
 *
 * 通过 Android 标准 API 检测设备硬件能力，包括 HDR 支持、编解码器等。
 * 所有检测结果都会被缓存，避免重复计算。
 *
 * 详细 API 参考文档见 docs/device_info_detection.md
 */
object DeviceInfoDetector {

    private const val TAG = "DeviceInfoDetector"

    // ==================== 缓存 ====================

    private var cachedVideoCodecs: List<String>? = null
    private var cachedAudioCodecs: List<String>? = null
    private var cachedHdrCapabilities: HdrCapabilities? = null
    private var cachedDolbyVisionProfiles: List<String>? = null
    private var cachedHevcMain10: Boolean? = null
    private var cachedAvcHigh10: Boolean? = null

    // ==================== 数据类 ====================

    /** HDR 能力 */
    data class HdrCapabilities(
        val hdr10: Boolean,
        val hdr10Plus: Boolean,
        val hlg: Boolean,
        val dolbyVision: Boolean,
    )

    /** 完整设备编解码器信息 */
    data class DeviceCodecInfo(
        val videoCodecs: List<String>,
        val audioCodecs: List<String>,
        val hdrCapabilities: HdrCapabilities,
        val dolbyVisionProfiles: List<String>,
        val hevcMain10: Boolean,
        val avcHigh10: Boolean,
    )

    // ==================== 公开方法 ====================

    /**
     * 获取完整的设备编解码器信息
     */
    fun getDeviceCodecInfo(context: Context): DeviceCodecInfo {
        return DeviceCodecInfo(
            videoCodecs = getVideoCodecDisplayNames(),
            audioCodecs = getAudioCodecDisplayNames(),
            hdrCapabilities = getHdrCapabilities(context),
            dolbyVisionProfiles = getDolbyVisionProfiles(),
            hevcMain10 = checkHevcMain10Support(),
            avcHigh10 = checkH264High10Support(),
        )
    }

    /**
     * 获取支持的所有视频编码器的可读名称列表
     */
    fun getVideoCodecDisplayNames(): List<String> {
        return cachedVideoCodecs ?: run {
            val mimeTypes = getSupportedVideoMimeTypes()
            val result = mimeTypes.map { mimeToDisplayName(it) }.distinct().sorted()
            cachedVideoCodecs = result
            result
        }
    }

    /**
     * 获取支持的所有音频编码器的可读名称列表
     */
    fun getAudioCodecDisplayNames(): List<String> {
        return cachedAudioCodecs ?: run {
            val result = getSupportedAudioMimeTypes()
                .map { mimeToDisplayName(it) }
                .distinct()
                .sorted()
            cachedAudioCodecs = result
            result
        }
    }

    /**
     * 检测 HDR 能力
     *
     * 通过 [Display.HdrCapabilities] 获取屏幕支持的 HDR 类型，
     * 并结合 [MediaCodecList] 检测编解码器对 HDR 的支持。
     */
    fun getHdrCapabilities(context: Context): HdrCapabilities {
        return cachedHdrCapabilities ?: run {
            var hdr10 = false
            var hdr10Plus = false
            var hlg = false
            var dolbyVision = false

            // 1. 通过 DisplayManager 检测屏幕 HDR 能力
            try {
                val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
                val hdrCaps = display?.hdrCapabilities
                if (hdrCaps != null) {
                    val types = hdrCaps.supportedHdrTypes
                    hdr10 = types.contains(Display.HdrCapabilities.HDR_TYPE_HDR10) || types.contains(2)
                    hdr10Plus = if (Build.VERSION.SDK_INT >= 29) {
                        types.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS) || types.contains(4)
                    } else {
                        false
                    }
                    hlg = types.contains(Display.HdrCapabilities.HDR_TYPE_HLG) || types.contains(3)
                    dolbyVision = types.contains(1) // Display.HdrCapabilities 中无常量，值为 1
                }
            } catch (e: Exception) {
                Log.w(TAG, "获取屏幕 HDR 能力失败", e)
            }

            // 2. 通过 MediaCodecList 补充检测（编解码器支持情况）
            try {
                val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                for (codec in codecInfos) {
                    if (codec.isEncoder) continue
                    val types = codec.supportedTypes

                    // Dolby Vision 检测
                    if (types.any { it.equals(MediaFormatMimeType.VIDEO_DOLBY_VISION, ignoreCase = true) }) {
                        dolbyVision = true
                    }

                    // HEVC 检测 HDR10/HDR10+ 通过 profile 级别
                    if (types.any { it.equals(MediaFormatMimeType.VIDEO_HEVC, ignoreCase = true) }) {
                        val capabilities = codec.getCapabilitiesForType(MediaFormatMimeType.VIDEO_HEVC)
                        for (pl in capabilities.profileLevels) {
                            // HEVC_Main10HDR10 = 4096, HEVC_Main10HDR10Plus = 8192 (API 29+)
                            if (pl.profile == 4096) hdr10 = true
                            if (pl.profile == 8192) hdr10Plus = true
                        }
                    }

                    // VP9 检测 HDR
                    if (types.any { it.equals(MediaFormatMimeType.VIDEO_VP9, ignoreCase = true) }) {
                        val capabilities = codec.getCapabilitiesForType(MediaFormatMimeType.VIDEO_VP9)
                        for (pl in capabilities.profileLevels) {
                            // VP9Profile2HDR / VP9Profile3HDR
                            if (pl.profile == 4096 || pl.profile == 8192) hdr10 = true
                            if (pl.profile == 16384 || pl.profile == 32768) hdr10Plus = true
                        }
                    }

                    // AV1 检测 HDR
                    if (types.any { it.equals(MediaFormatMimeType.VIDEO_AV1, ignoreCase = true) }) {
                        val capabilities = codec.getCapabilitiesForType(MediaFormatMimeType.VIDEO_AV1)
                        for (pl in capabilities.profileLevels) {
                            if (pl.profile == 4096) hdr10 = true
                            if (pl.profile == 8192) hdr10Plus = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "通过编解码器检测 HDR 失败", e)
            }

            val result = HdrCapabilities(hdr10, hdr10Plus, hlg, dolbyVision)
            cachedHdrCapabilities = result
            Log.d(TAG, "HDR 检测结果: HDR10=$hdr10, HDR10+=$hdr10Plus, HLG=$hlg, DolbyVision=$dolbyVision")
            result
        }
    }

    /**
     * 获取支持的杜比视界 Profile 列表
     */
    fun getDolbyVisionProfiles(): List<String> {
        return cachedDolbyVisionProfiles ?: run {
            val profiles = mutableListOf<String>()
            try {
                val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                for (codec in codecInfos) {
                    if (codec.isEncoder) continue
                    if (!codec.supportedTypes.any {
                            it.equals(MediaFormatMimeType.VIDEO_DOLBY_VISION, ignoreCase = true)
                        }) continue

                    val capabilities = codec.getCapabilitiesForType(MediaFormatMimeType.VIDEO_DOLBY_VISION)
                    for (pl in capabilities.profileLevels) {
                        val name = when (pl.profile) {
                            1 -> "Profile 0 (dvav.per)"
                            2 -> "Profile 1 (dvav.pen)"
                            4 -> "Profile 4 (dvhe.der)"
                            8 -> "Profile 5 (dvhe.den)"
                            16 -> "Profile 7 (dvhe.dtr)"
                            32 -> "Profile 8 (dvhe.stn)"
                            256 -> "Profile 8.1 (dvhe.st)"
                            512 -> "Profile 9 (dvav.se)"
                            else -> "Profile ${pl.profile}"
                        }
                        if (name !in profiles) {
                            profiles.add(name)
                            Log.d(TAG, "杜比视界 profile 支持: $name (${codec.name})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "获取杜比视界 Profile 失败", e)
            }
            val result = profiles.sorted()
            cachedDolbyVisionProfiles = result
            result
        }
    }

    /**
     * 检测是否支持 HEVC Main10（10-bit）
     *
     * 根据 Android API 定义，HEVC_Main10 profile 值为 2。
     */
    fun checkHevcMain10Support(): Boolean {
        return cachedHevcMain10 ?: run {
            val result = checkProfileSupport(
                MediaFormatMimeType.VIDEO_HEVC,
                2  // CodecProfileLevel.HEVC_Main10
            )
            cachedHevcMain10 = result
            result
        }
    }

    /**
     * 检测是否支持 H.264 High10（10-bit）
     */
    fun checkH264High10Support(): Boolean {
        return cachedAvcHigh10 ?: run {
            val result = checkProfileSupport(
                MediaFormatMimeType.VIDEO_AVC,
                10  // CodecProfileLevel.AVCProfileHigh10
            )
            cachedAvcHigh10 = result
            result
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 获取所有支持的视频 MIME 类型
     */
    private fun getSupportedVideoMimeTypes(): Set<String> {
        val types = mutableSetOf<String>()
        try {
            val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            for (codec in codecInfos) {
                if (codec.isEncoder) continue
                for (type in codec.supportedTypes) {
                    if (type.startsWith("video/", ignoreCase = true)) {
                        types.add(type)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "获取支持的视频编码器失败", e)
        }
        return types
    }

    /**
     * 获取所有支持的音频 MIME 类型
     */
    private fun getSupportedAudioMimeTypes(): Set<String> {
        val types = mutableSetOf<String>()
        try {
            val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            for (codec in codecInfos) {
                if (codec.isEncoder) continue
                for (type in codec.supportedTypes) {
                    if (type.startsWith("audio/", ignoreCase = true)) {
                        types.add(type)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "获取支持的音频编码器失败", e)
        }
        return types
    }

    /**
     * 检测指定 MIME 类型的某个 Profile 是否被支持
     */
    private fun checkProfileSupport(mimeType: String, targetProfile: Int): Boolean {
        try {
            val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            for (codec in codecInfos) {
                if (codec.isEncoder) continue
                if (!codec.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }) continue

                val capabilities = codec.getCapabilitiesForType(mimeType)
                if (capabilities.profileLevels.any { it.profile == targetProfile }) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "检测 Profile 支持失败: $mimeType/$targetProfile", e)
        }
        return false
    }

    /**
     * 将 MIME 类型转换为可读的显示名称
     */
    private fun mimeToDisplayName(mimeType: String): String {
        val lower = mimeType.lowercase(Locale.ROOT)
        return when {
            lower.contains("h264") || lower.contains("avc") -> "H.264 (AVC)"
            lower.contains("hevc") || lower.contains("h265") -> "H.265 (HEVC)"
            lower.contains("av1") -> "AV1"
            lower.contains("vp9") -> "VP9"
            lower.contains("vp8") -> "VP8"
            lower.contains("mpeg4") || lower.contains("mp4v") -> "MPEG-4"
            lower.contains("mpeg2") -> "MPEG-2"
            lower.contains("aac") -> "AAC"
            lower.contains("mp3") || lower.contains("mpeg") && !lower.contains("mpeg2") && !lower.contains("mpeg4") -> "MP3"
            lower.contains("opus") -> "Opus"
            lower.contains("vorbis") -> "Vorbis"
            lower.contains("flac") -> "FLAC"
            lower.contains("ac3") || lower.contains("eac3") -> "Dolby AC-3/E-AC-3"
            lower.contains("truehd") -> "Dolby TrueHD"
            lower.contains("dts") -> "DTS"
            lower.contains("wma") -> "WMA"
            lower.contains("alac") -> "ALAC"
            lower.contains("pcm") -> "PCM"
            lower.contains("dolby") || lower.contains("dv") -> "Dolby Vision"
            else -> mimeType.removePrefix("video/").removePrefix("audio/")
                .replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * MIME 类型常量
     * 避免直接依赖 MediaFormat/MimeTypes 中的常量名
     */
    private object MediaFormatMimeType {
        const val VIDEO_DOLBY_VISION = "video/dolby-vision"
        const val VIDEO_HEVC = "video/hevc"
        const val VIDEO_VP9 = "video/x-vnd.on2.vp9"
        const val VIDEO_AV1 = "video/av01"
        const val VIDEO_AVC = "video/avc"
    }
}
