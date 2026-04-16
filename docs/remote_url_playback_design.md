# 远程链接实时播放与实时超分设计

本文档面向后续开发，目标是把本项目现有的“网页嗅探 -> mpv 播放”雏形，收敛成一条稳定、可扩展、兼容浏览器请求头的远程播放主链路。

## 1. 目标

目标能力不是“下载后播放”，而是：

- 输入一个浏览器可播放的远程视频链接，直接实时播放
- 支持来自 WebView 嗅探的链接继续携带关键请求头
- 尽量像浏览器一样兼容 `mp4`、`m3u8`、跳转链、带签名参数的临时 URL
- 继续复用现有 `Anime4K`，实现在线视频实时超分

典型样例：

- `https://vdownload.hembed.com/404979-480p.mp4?secure=...`

对这类链接，只要它本身不是 DRM 流，也不依赖浏览器特有的媒体扩展，理论上都应该进入统一远程播放链路而不是特殊分支。

## 2. 当前代码现状

当前已有可复用能力：

- `app/src/main/java/com/fam4k007/videoplayer/tv/TVBrowserActivity.kt`
  - WebView 拦截请求并调用 `VideoSnifferManager.processRequest()`
- `app/src/main/java/com/fam4k007/videoplayer/sniffer/VideoSnifferManager.kt`
  - 维护已识别视频列表
- `app/src/main/java/com/fam4k007/videoplayer/sniffer/UrlDetector.kt`
  - 通过扩展名、关键词、`Content-Type` 识别视频 URL
- `app/src/main/java/com/fam4k007/videoplayer/VideoPlayerActivity.kt`
  - 已支持 `http/https` URI 走在线视频分支
- `app/src/main/java/com/fam4k007/videoplayer/player/PlaybackEngine.kt`
  - 已有 `loadVideoFromUrl()`，支持把 HTTP 头传给 mpv
- `app/src/main/java/com/fam4k007/videoplayer/Anime4KManager.kt`
  - 在线/本地都可复用同一套 shader chain

当前主要问题：

- 远程播放参数通过 `DetectedVideo.toFullUrlString()` 拼成字符串跨页面传递，不够稳。
- `VideoPlayerActivity` 里仍有“按网站打补丁”的逻辑，例如 B 站 `Referer`。
- mpv 的远程请求选项缺少一次播放一次重置的边界。
- 主页没有“直接输入 URL 播放”的入口。
- WebDAV 仍把用户名密码直接塞进 URL，不利于后续统一远程请求模型。

## 3. 目标架构

建议新增 `remote/` 包，形成如下结构：

- `RemotePlaybackRequest`
  - 统一远程播放请求模型
- `RemotePlaybackHeaders`
  - 对请求头做过滤、归一化、白名单化
- `RemotePlaybackResolver`
  - 播放前做预解析、重定向跟随、媒体类型探测
- `RemotePlaybackLauncher`
  - 从不同入口统一跳转播放器
- `RemotePlaybackError`
  - 统一错误分类

建议的数据模型：

```kotlin
@Parcelize
data class RemotePlaybackRequest(
    val url: String,
    val title: String = "",
    val sourcePageUrl: String = "",
    val headers: Map<String, String> = emptyMap(),
    val detectedContentType: String? = null,
    val isStream: Boolean = false,
    val source: Source = Source.UNKNOWN
) : Parcelable {
    enum class Source {
        DIRECT_INPUT,
        WEB_SNIFFER,
        WEBDAV,
        BILIBILI,
        UNKNOWN
    }
}
```

核心原则：

- 页面间只传结构化对象，不再传“拼接好的 URL 字符串”
- `VideoPlayerActivity` 只关心“本地播放请求”或“远程播放请求”
- 所有远程来源最终都汇聚到 `PlaybackEngine.loadRemote(request)`

## 4. 入口统一策略

### 4.1 主页直输 URL

新增一个明确的“打开链接”入口，直接接收：

- 纯 URL
- `URL + Referer`
- `URL + Cookie`
- 未来可扩展为多行高级模式

第一阶段不需要复杂 UI，一个简单对话框即可。

### 4.2 WebView 嗅探

`TVBrowserActivity` 不再把 headers 编码到 URI 字符串里，而是：

1. 从 `DetectedVideo` 生成 `RemotePlaybackRequest`
2. 使用 `Intent.putExtra("remote_request", request)` 跳转播放器

### 4.3 WebDAV

WebDAV 不再把账号密码写入 URL，而应：

1. 生成真实文件 URL
2. 用 `Authorization` 或基础认证头放进 `RemotePlaybackRequest.headers`

## 5. 远程请求头策略

不要把 WebView 拦截到的所有 header 全量透传给 mpv，只保留真正影响播放的关键头。

建议白名单：

- `Referer`
- `Origin`
- `User-Agent`
- `Cookie`
- `Authorization`
- `Range`
- `Accept`

建议丢弃：

- `Sec-Fetch-*`
- `sec-ch-*`
- `Accept-Language`
- `Upgrade-Insecure-Requests`
- 与浏览器连接管理相关的头

原因：

- 全量透传容易把浏览器特有请求语义带给 mpv，反而降低兼容性
- 某些 header 在 mpv/OkHttp 环境下无意义，甚至会导致服务器判定异常

## 6. 播放前解析策略

为了做到“像浏览器一样兼容”，建议在真正调用 mpv 前加一个轻量解析层。

### 6.1 基本流程

1. 对输入 URL 做合法性校验
2. 使用 OkHttp 跟随重定向
3. 优先尝试 `HEAD`
4. 如服务端不支持 `HEAD`，退化为带 `Range: bytes=0-1` 的 `GET`
5. 记录：
   - 最终 URL
   - 最终响应头
   - `Content-Type`
   - `Content-Disposition`
6. 推断是否为：
   - 直链文件
   - HLS 主清单
   - HLS 子清单
   - 非媒体资源

### 6.2 这样做的收益

- 可以展开短链和 302/303/307 跳转
- 可以识别没有扩展名但 `Content-Type` 正确的媒体接口
- 可以在播放器启动前给出更准确的错误提示
- 可以把最终 URL、关键头和媒体类型统一传给 mpv

## 7. mpv 层建议

`PlaybackEngine` 不要再区分“普通在线视频字符串”和“带 headers 拼装字符串”，而是提供统一方法：

```kotlin
fun loadRemote(request: RemotePlaybackRequest, startPosition: Double = 0.0)
```

建议在该方法里明确分三步：

1. `resetRemoteOptions()`
2. `applyRemoteHeaders(request)`
3. `applyStreamingOptions(request)`

建议显式控制的 mpv 选项：

- `user-agent`
- `referrer`
- `http-header-fields`
- `cache`
- `cache-secs`
- `demuxer-max-bytes`
- `demuxer-seekable-cache`
- `stream-buffer-size`
- `http-allow-redirect`
- `hls-bitrate`

关键点：

- 每次新远程播放前都要重置一次，避免上一个站点的头信息污染下一个站点
- `User-Agent` 最好既能作为专门 option 设置，也能同步进 header 组装
- `Referer`、`Origin`、`Cookie` 需要按请求维度设置，而不是散落在 Activity 里

## 8. 实时超分策略

在线视频不需要单独做一套超分逻辑，沿用当前链路即可：

1. mpv 加载远程流
2. `VideoPlayerActivity.applyAnime4K()`
3. `PlaybackEngine.setShaderList()`
4. 着色器作用在最终视频纹理上

但建议补两个保护策略：

- 对 1080p 以上视频默认不自动开启强模式，避免性能浪费
- 对明显卡顿设备，提示用户降低 Anime4K 质量或关闭硬件/软件组合的高负载模式

建议后续做一个基于分辨率的默认策略：

- `<= 480p`：优先 `Mode.C / C_PLUS`
- `720p`：优先 `Mode.B / B_PLUS`
- `1080p`：优先 `Mode.A`
- `> 1080p`：默认关闭，仅手动开启

## 9. 兼容性边界

### 9.1 可以重点支持的

- 公开可访问的 `mp4`
- 公开或带临时签名的 `mp4`
- 标准 `m3u8`
- 带 `Referer` / `Cookie` / `User-Agent` 的防盗链资源
- 嗅探自 WebView 同一页面上下文的媒体请求

### 9.2 明确不在第一阶段保证的

- DRM/Widevine 流
- 依赖浏览器 MediaSource Extensions 的站点播放器内部分片逻辑
- 需要持续执行页面 JavaScript 才能刷新 token 的私有协议
- 复杂 DASH + DRM 的版权平台

## 10. 分阶段实施

### 第一阶段：统一模型和入口

目标：

- 让“手输 URL”和“WebView 嗅探 URL”都能稳定走同一条链路

修改建议：

- 新增 `remote/RemotePlaybackRequest.kt`
- 新增 `remote/RemotePlaybackLauncher.kt`
- 修改 `tv/TVBrowserActivity.kt`
- 修改 `VideoPlayerActivity.kt`
- 修改 `player/PlaybackEngine.kt`

验收标准：

- 手动输入 `mp4` 直链可以直接播放
- WebView 嗅探到的 `mp4` 直链可以带关键头播放
- 在线视频可正常启用 Anime4K

### 第二阶段：兼容性增强

目标：

- 让更多“浏览器能播、直链不规整”的链接也能播

修改建议：

- 新增 `remote/RemotePlaybackResolver.kt`
- 统一处理重定向、无扩展名媒体接口、内容类型探测
- 加入失败重试与更清晰的错误提示

验收标准：

- 可播放重定向后的媒体 URL
- 可识别无扩展名但 `Content-Type` 为视频的接口
- 对无效链接能给出明确失败原因

### 第三阶段：远程源统一

目标：

- 让 WebDAV、嗅探、未来在线源统一接入

修改建议：

- 修改 `webdav/WebDavBrowserComposeActivity.kt`
- 去除 URL 内嵌认证信息
- 让 WebDAV 也走 `RemotePlaybackRequest`

验收标准：

- WebDAV 不再把密码暴露在 URL 中
- WebDAV、Web 嗅探、手输 URL 共用同一播放器入口

## 11. 建议补的测试

优先补低成本纯逻辑测试：

- `UrlDetector` 规则测试
- `RemotePlaybackHeaders` 白名单过滤测试
- `RemotePlaybackResolver` 的重定向和类型识别测试
- `PlaybackEngine` 的 header 组装测试

至少覆盖这些样例：

- 普通 `mp4`
- 普通 `m3u8`
- 带签名参数的 `mp4`
- 带 `Referer` 的防盗链 URL
- `Content-Type=video/*` 但无扩展名的 URL
- 明显非媒体资源 URL

## 12. 本项目里最值得优先修改的文件

- `app/src/main/java/com/fam4k007/videoplayer/VideoPlayerActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/player/PlaybackEngine.kt`
- `app/src/main/java/com/fam4k007/videoplayer/player/CustomMPVView.kt`
- `app/src/main/java/com/fam4k007/videoplayer/tv/TVBrowserActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/sniffer/DetectedVideo.kt`
- `app/src/main/java/com/fam4k007/videoplayer/sniffer/UrlDetector.kt`
- `app/src/main/java/com/fam4k007/videoplayer/webdav/WebDavBrowserComposeActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/compose/HomeScreen.kt`

## 13. 结论

当前项目并不缺“在线播放能力”，也不缺“实时超分能力”；真正缺的是一个统一、结构化、可扩展的远程播放入口。

只要先把这层抽出来，后续要做的：

- 手输 URL 播放
- 浏览器嗅探播放
- 远程头信息兼容
- 在线视频实时超分
- WebDAV 统一接入

都会变成同一条主链路上的增量开发，而不是继续在 `Activity` 里叠条件分支。
