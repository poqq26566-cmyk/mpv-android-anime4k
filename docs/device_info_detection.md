# 设备信息检测

设备信息页面通过 Android 标准 API 检测当前设备的硬件能力，不依赖任何第三方开源库。

## 检测项说明

### HDR 支持

检测屏幕和编解码器支持的 HDR 格式。

| 检测项 | 说明 |
|--------|------|
| HDR10 | 基础 HDR 格式，大部分支持 HDR 的安卓设备都有 |
| HDR10+ | HDR10 增强版，带动态元数据 |
| HLG | 广播电视用 HDR 标准 |
| 杜比视界 | 杜比公司的 HDR 格式 |

**API**: `Display.getHdrCapabilities().getSupportedHdrTypes()`
- 文档: https://developer.android.com/reference/android/view/Display.HdrCapabilities

同时通过 `MediaCodecList` 遍历编解码器的 Profile 级别进行补充检测。

### 10-bit 支持

检测设备是否支持 10-bit 色深的视频硬解。

- **HEVC Main10**: HEVC (H.265) 10-bit Profile
- **AVC High10**: AVC (H.264) 10-bit Profile

**API**: `MediaCodecInfo.CodecProfileLevel`
- HEVC_Main10 = 2
- AVCProfileHigh10 = 10
- 文档: https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel

### 杜比视界 Profile

列出设备硬件支持的所有杜比视界 Profile。

| Profile | 标识 |
|---------|------|
| Profile 0 | dvav.per |
| Profile 1 | dvav.pen |
| Profile 4 | dvhe.der |
| Profile 5 | dvhe.den |
| Profile 7 | dvhe.dtr |
| Profile 8 | dvhe.stn |
| Profile 8.1 | dvhe.st |
| Profile 9 | dvav.se |

**API**: `MediaCodecList.getCodecInfos()` + `MediaCodecInfo.getCapabilitiesForType("video/dolby-vision")`
- 文档: https://developer.android.com/reference/android/media/MediaCodecList

### 视频编码器

列出设备硬件支持（能硬解）的所有视频编码格式，如 H.264 (AVC)、H.265 (HEVC)、AV1、VP9 等。

**API**: `MediaCodecList` 遍历所有解码器，筛选 `video/` 开头的 MIME 类型
- 文档: https://developer.android.com/reference/android/media/MediaCodecList

### 音频编码器

列出设备硬件支持的所有音频编码格式，如 AAC、FLAC、Opus、AC-3 等。

**API**: 同上，筛选 `audio/` 开头的 MIME 类型

### 基础设备信息

| 信息项 | 来源 |
|--------|------|
| 应用版本 | `PackageManager.getPackageInfo()` |
| Android 版本 | `Build.VERSION.RELEASE` + `Build.VERSION.SDK_INT` |
| 设备品牌/制造商/型号 | `Build.BRAND`、`Build.MANUFACTURER`、`Build.MODEL` |
| MPV/FFmpeg/libplacebo 版本 | `is.xyz.mpv.Utils.VERSIONS` |

**API**: 
- https://developer.android.com/reference/android/os/Build
- https://developer.android.com/reference/android/content/pm/PackageManager

## 核心源码

- 检测逻辑: `app/.../ui/utils/DeviceInfoDetector.kt`
- 界面展示: `app/.../ui/screens/DeviceInfoScreen.kt`
- 状态管理: `app/.../presentation/DeviceInfoViewModel.kt`
