# 项目架构分析

本文档基于当前仓库源码整理，目标是帮助后续开发者快速建立对项目的整体认知，并作为后续重构、功能迭代、问题定位的参考。

## 1. 项目定位

这是一个以 `libmpv` 为播放核心的 Android 本地视频播放器，主打能力不是“单纯播放”，而是围绕“二次元/番剧/本地收藏场景”叠加了一整套增强能力：

- Anime4K 实时超分着色器链
- 本地/网络字幕导入与搜索
- 本地/网络弹幕加载、匹配与控制
- 哔哩哔哩扫码登录、番剧解析、在线播放、下载
- WebDAV 远程视频浏览与播放
- TV/WebView 视频嗅探与跳转播放
- 播放历史、记忆播放、缩略图、章节跳过等播放器增强能力

从产品形态上看，这不是一个“单模块播放器”，而是一个“播放器内核 + 媒体浏览 + 在线资源接入 + 多种增强能力”的复合型应用。

## 2. 技术栈与构建面

### 2.1 构建与语言

- 单模块 Android 工程：仅 `:app`
- Gradle Groovy DSL
- Kotlin 为主，少量 Java
- `compileSdkVersion 34`、`targetSdkVersion 34`、`minSdkVersion 26`
- Java/Kotlin 目标版本为 17

关键位置：

- 构建入口：`build.gradle`
- 模块配置：`app/build.gradle`
- 清单声明：`app/src/main/AndroidManifest.xml`

### 2.2 核心三方依赖

- `mpv-android-lib-v0.1.10.aar`：播放核心
- `DanmakuFlameMaster.aar`：弹幕渲染
- `sardine`：WebDAV 访问
- `OkHttp`：网络请求
- `Room`：本地数据库
- `WorkManager`：后台任务
- `Paging3`：大列表分页
- `Compose Material3`：新页面 UI
- `Glide` / `Coil`：图片与视频帧加载
- `ZXing`：二维码生成
- `AndroidX Security`：加密存储

### 2.3 构建特征

- 只编译 `arm64-v8a`
- 开启 `viewBinding` 与 `compose`
- 使用本地 `app/libs` 放置 AAR/JAR
- 从 `local.properties` 读取 DanDanPlay / Wyzie API 凭证
- 通过 `app/schemas/` 导出 Room schema

这说明项目当前的发布目标更偏移动端真机，而不是全 ABI 覆盖。

## 3. 总体架构视图

从实现方式看，项目整体是“单 app 模块 + 按功能分包 + 在播放器核心场景采用管理器协作”的架构。

可以把它理解为 6 层：

### 3.1 启动与全局基础层

- `AppApplication`
- 全局异常处理 `CrashHandler`
- 全局主题初始化
- Room 数据库预热

职责很轻，主要做应用级初始化，不承载业务。

### 3.2 页面与导航层

页面以 Activity 为导航单元，主要入口包括：

- `MainActivity`：首页/入口聚合
- `VideoBrowserComposeActivity`：本地文件夹浏览
- `VideoListComposeActivity`：文件夹视频列表
- `VideoPlayerActivity`：核心播放器
- `BiliBiliPlayActivity`：番剧解析与播放前选择
- `DownloadActivity`：下载管理
- `SubtitleSearchActivity`：字幕搜索下载
- `WebDavComposeActivity` / `WebDavBrowserComposeActivity`
- `TVBrowserActivity`：网页视频嗅探

项目没有使用 Navigation Compose，也没有单 Activity 架构；当前是典型的“多 Activity + 局部 Compose 页面”。

### 3.3 播放核心层

核心集中在 `VideoPlayerActivity`，这是项目最重要的业务中枢。它本身并不直接承载所有细节，而是组装多个 manager：

- `PlaybackEngine`：mpv 控制与事件监听
- `CustomMPVView`：mpv View 与初始化参数
- `PlayerControlsManager`：控制栏、锁定、时间、电量、自动隐藏
- `GestureHandler`：亮度/音量/双击/滑动进度/长按倍速
- `PlayerDialogManager`：字幕、弹幕、倍速、Anime4K、更多菜单
- `SeriesManager`：同目录/同系列上下集切换
- `SubtitleManager`：外挂字幕导入与路径处理
- `DanmakuManager`：弹幕文件发现、加载、同步和配置下发
- `Anime4KManager`：着色器准备和 shader chain 生成
- `VideoThumbnailManager` / `SeekBarThumbnailHelper`：拖动预览图
- `SkipIntroOutroManager`：跳 OP/ED/章节能力
- `ComposeOverlayManager`：在 View 播放器之上叠加 Compose 抽屉/弹层

这是项目里最明显的“控制器 + 多管理器”模式。

### 3.4 数据与状态层

数据层并未形成 Repository/UseCase 体系，而是以“DAO + Manager + Activity/ViewModel 直接调用”为主。

主要数据载体：

- Room 数据库 `VideoDatabase`
- 播放历史 `PlaybackHistoryEntity` / `PlaybackHistoryDao`
- 视频缓存 `VideoCacheEntity` / `VideoCacheDao`
- 偏好设置 `PreferencesManager`
- 主题设置 `ThemeManager`
- 敏感数据 `SecureStorage`

这层的特点是“简单直接，接入快”，代价是业务边界不够清晰。

### 3.5 在线能力层

项目把在线能力按来源拆分：

- 哔哩哔哩：`bilibili/` + `download/`
- 弹弹 play：`dandanplay/`
- Wyzie 字幕：`subtitle/`
- WebDAV：`webdav/`
- 网页嗅探：`sniffer/` + `tv/`

这些能力基本都直接从 Activity、ViewModel 或 manager 发起网络调用，没有统一 API 抽象层。

### 3.6 UI 组件层

UI 是混合式的：

- 新页面：Compose 为主
- 播放器页：传统 View 为主 + Compose 覆盖层
- 列表/弹窗：View 和 Compose 并存

因此当前项目是“过渡态 UI 架构”，不是完全 Compose 化，也不是纯传统 View。

## 4. 运行主链路

## 4.1 应用启动链路

1. `AppApplication` 初始化崩溃处理、主题和数据库预热
2. `MainActivity` 检查用户协议
3. 首页 Compose 页面展示最近播放、入口按钮等
4. 用户再进入本地浏览、在线番剧、WebDAV、TV 嗅探等功能页面

这个阶段的业务很轻，真正复杂度从播放器页开始。

### 4.2 本地媒体浏览链路

本地浏览分为两段：

1. `VideoBrowserComposeActivity`
   - 申请存储权限
   - 通过 `MediaStore` 扫描视频
   - 过滤 `.nomedia`
   - 按目录聚合成 `VideoFolder`

2. `VideoListComposeActivity`
   - 接收某个文件夹的视频列表
   - 当数量不大时，直接使用内存列表显示
   - 当数量超过 100 时，先写入 Room，再通过 `Paging3 + VideoPagingSource` 分页加载

这里体现了一个非常重要的设计取向：

- 文件夹页以“实时扫描 + 内存聚合”为主
- 大列表页通过 Room 做中间缓存，避免一次性加载过多视频导致 OOM

这套设计对于本地视频库较大的设备是有效的，但也意味着“媒体库能力”目前还不是独立子系统，而是由页面自己驱动。

### 4.3 本地视频播放链路

核心链路如下：

1. 页面跳转进入 `VideoPlayerActivity`
2. 初始化多个 manager
3. 初始化 `CustomMPVView`
4. `PlaybackEngine` 注册 mpv observer
5. Activity 解析 `Intent` 中的视频 URI、播放位置、视频列表等数据
6. `PlaybackEngine.loadVideo()` 调用 `MPVLib.command("loadfile", ...)`
7. `PlaybackEngine` 持续更新播放状态、进度、缓冲状态
8. `PlayerControlsManager` 与 `GestureHandler` 响应用户交互
9. `PlaybackHistoryManager` 写入历史记录与断点进度
10. `DanmakuManager` / `SubtitleManager` / `Anime4KManager` 在播放中动态接入

特点是：

- 播放器页负责流程编排
- manager 负责子能力
- Activity 自己仍保留了大量流程判断与状态控制

这也是后续重构时最应该优先切分的区域。

### 4.4 弹幕链路

弹幕来源有三种：

- 本地同名/旁路弹幕文件自动发现
- 手动导入弹幕文件
- DanDanPlay / B 站在线下载后加载

运行方式：

1. `DanmakuManager` 初始化 `DanmakuPlayerView`
2. 根据视频路径寻找弹幕或接收指定弹幕路径
3. 加载 XML 文件并构建弹幕轨道
4. 在播放、暂停、seek、倍速切换时与播放器位置同步
5. 配置项由 `PreferencesManager` 持久化，运行中由 `DanmakuManager.applyAllSettings()` 下发

这是一个“渲染层独立、时间轴依赖播放器”的典型外挂轨道系统。

### 4.5 字幕链路

字幕能力分为两类：

- 本地外挂字幕导入
- 在线字幕搜索下载

本地字幕：

- `SubtitleManager` 负责 URI 转换、文件复制、路径规范化
- 最终仍交给 mpv 的字幕能力处理

在线字幕：

- `SubtitleSearchActivity` 提供 UI
- `SubtitleDownloadManager` 请求 Wyzie/TMDB 相关接口
- 下载结果保存到用户选择目录

字幕体系的好处是“播放器核心不需要理解字幕搜索逻辑”，不足是 UI 与下载逻辑仍比较紧耦合。

### 4.6 Anime4K 链路

Anime4K 的运行模式比较清晰：

1. `Anime4KManager.initialize()` 首次复制 shader 到应用私有目录
2. 根据模式 + 质量生成 shader chain
3. `VideoPlayerActivity.applyAnime4K()` 将链路下发给 mpv
4. 配置可记忆并在后续视频播放中恢复

它本质上是“mpv 图像滤镜链管理器”，独立性较高，是当前项目里相对干净的一块能力。

### 4.7 哔哩哔哩链路

哔哩哔哩能力拆成三段：

1. 登录：`BiliBiliAuthManager`
   - 扫码登录
   - Cookie 管理
   - 用户信息存储

2. 播放前解析：`BiliBiliPlayActivity`
   - 解析番剧 URL
   - 查询 season / ep 信息
   - 选择剧集
   - 跳转播放器

3. 下载：`BilibiliDownloadManager` + `BilibiliDownloadViewModel`
   - 解析 aid/bvid/epid 等标识
   - 请求播放地址
   - 视频/音频分轨下载
   - 本地 `MediaMuxer` 合并

这是一个完整的“认证 - 元数据 - 流地址 - 播放/下载”闭环，但没有形成通用网络服务层，因此复用和测试成本较高。

### 4.8 WebDAV 链路

WebDAV 能力也很直接：

1. `WebDavAccountManager` 存储账户
2. `WebDavClient` 使用 Sardine 列目录
3. `WebDavBrowserComposeActivity` 浏览文件夹
4. 选中视频后拼接 URL，跳转 `VideoPlayerActivity`

这条链路与本地播放复用播放器本身，因此在线与离线播放在后半段是统一的。

### 4.9 TV/Web 嗅探链路

TV 浏览器能力由 `TVBrowserActivity` 提供：

- 内嵌 `WebView`
- 在 `shouldInterceptRequest` 中抓取资源请求
- `VideoSnifferManager` 识别视频 URL
- 从候选视频中选最佳链接
- 跳转 `VideoPlayerActivity`

这实际上是一个独立的“网页视频提取子系统”，只是最终输出仍是统一播放器入口。

## 5. 包级职责划分

### 5.1 根包下的 Activity/入口类

承担导航与页面编排职责，但也夹带了一部分业务逻辑。

代表文件：

- `MainActivity`
- `VideoPlayerActivity`
- `VideoBrowserComposeActivity`
- `VideoListComposeActivity`
- `SubtitleSearchActivity`
- `DownloadActivity`

### 5.2 `player/`

播放器核心协作层，是最重要的业务基础设施包。

- 与 mpv 的交互
- 控制层与手势层
- 播放器弹窗与抽屉
- 系列识别与上下集切换
- 缩略图预览

### 5.3 `manager/`

偏向横切能力和播放器周边管理：

- 偏好设置
- 截图
- 字幕辅助
- 缩略图
- 主题模式

### 5.4 `compose/`

新式页面 UI 组件与 Compose 屏幕。

### 5.5 `database/` + `paging/`

本地缓存与分页支撑层。

### 5.6 `bilibili/`、`download/`、`subtitle/`、`dandanplay/`、`webdav/`

按外部能力来源组织的在线功能模块。

### 5.7 `danmaku/`

外挂弹幕系统。

### 5.8 `sniffer/` + `tv/`

网页嗅探与浏览器功能。

### 5.9 `utils/`

杂项工具类集合，覆盖日志、更新、路径、主题、缓存、媒体扫描等。

这一层很方便，但也容易持续膨胀。

## 6. 当前架构的核心优点

### 6.1 能力闭环完整

项目已经具备播放器、媒体浏览、弹幕、字幕、番剧、下载、远程播放、网页嗅探等完整闭环，产品面非常强。

### 6.2 播放器子能力已有初步拆分

虽然 `VideoPlayerActivity` 依然很大，但播放、手势、控制栏、弹窗、弹幕、缩略图等子域已经拆成 manager，说明后续继续重构是有抓手的。

### 6.3 大列表已经考虑性能

视频数量超过阈值时使用 Room + Paging3，是正确方向。

### 6.4 安全存储已覆盖敏感信息

哔哩哔哩 Cookie 与用户信息使用 `EncryptedSharedPreferences`，比普通 SharedPreferences 更稳妥。

### 6.5 UI 迁移路径现实可行

项目没有强推一次性全量 Compose 重写，而是采用“新页面 Compose，播放器保留 View”的渐进策略，工程风险更低。

## 7. 当前架构的主要问题

### 7.1 `VideoPlayerActivity` 过大

它既负责：

- 页面生命周期
- Intent 解析
- manager 组装
- 播放状态编排
- 字幕/弹幕/Anime4K/历史流程
- 上下集切换
- 返回行为与持久化

这是当前最明显的“God Activity”。后续任何播放器功能迭代，几乎都会碰到它。

### 7.2 架构风格不统一

同一项目内同时存在：

- 多 Activity 导航
- Compose 页面
- 传统 View 播放器页
- manager 模式
- Activity 直接调 DAO
- ViewModel 只在部分页面存在

这说明项目处于演进过程中，但也会提高维护门槛。

### 7.3 数据层边界不清

当前大量页面直接：

- 调 MediaStore
- 调 Room DAO
- 调网络 API
- 调 SharedPreferences

缺少统一的 Repository / UseCase 抽象，导致业务规则分散在页面、manager、ViewModel 之间。

### 7.4 包与命名空间混杂

仓库中同时存在：

- `com.fam4k007.videoplayer.*`
- `com.fanchen.fam4k007.*`

这说明部分新功能是后续拼接进来的，后续继续扩展时容易让依赖关系越来越隐式。

### 7.5 主题体系重复

当前存在两个 `ThemeManager`：

- `utils/ThemeManager.kt`
- `manager/ThemeManager.kt`

并且 `AppApplication` 与各 Activity 使用的主题入口并不完全一致。这会导致主题状态、夜间模式和页面视觉策略进一步分裂。

### 7.6 文档与实现存在不一致

源码与文档目前至少有这些差异：

- 构建脚本 `minSdkVersion` 为 26，但 README/开发文档写到 28
- README 顶部系统要求又写 Android 12 / API 31+
- 文档建议 JDK 11+，但实际构建目标已是 Java/Kotlin 17

这类不一致会直接影响后续新同学搭环境和排查构建问题。

### 7.7 安全策略偏“可用性优先”

以下实现明显偏向“为了兼容能播”：

- mpv 关闭 TLS 校验
- WebDAV 客户端信任所有证书并放过主机名校验
- WebDAV 播放时把用户名密码直接嵌入 URL
- Manifest 开启 `usesCleartextTraffic=true`
- 申请 `MANAGE_EXTERNAL_STORAGE`

这些做法在实用层面确实能减少“播不了”的情况，但在可审计性、安全性和上架合规上风险较高。

### 7.8 部分能力未闭环

当前源码中能直接看到未完成项：

- 下载任务持久化仍是 TODO
- 断点续传仍是 TODO
- Paging 模式下搜索尚未在数据库层实现，只做了 UI 提示

这说明某些能力已经有 UI 入口，但工程闭环尚未完全完成。

## 8. 后续开发建议

下面给出的是“适合当前仓库状态”的推进顺序，而不是理想化大重写。

### 8.1 第一优先级：先给播放器主链路瘦身

建议把 `VideoPlayerActivity` 继续拆成以下角色：

- `PlayerSessionController`：播放会话总编排
- `PlayerIntentParser`：解析外部/内部播放入口
- `PlayerStateStore`：保存播放状态、UI 状态、轨道状态
- `PlaybackHistoryCoordinator`：历史记录与断点续播
- `PlayerFeatureCoordinator`：字幕/弹幕/Anime4K/截图/章节跳过统一调度

目标不是立刻做“纯净 MVVM”，而是先降低 `VideoPlayerActivity` 的修改面。

### 8.2 第二优先级：补一个稳定的数据抽象层

建议按功能逐步引入 Repository：

- `MediaLibraryRepository`
- `PlaybackHistoryRepository`
- `SubtitleRepository`
- `DanmakuRepository`
- `BilibiliRepository`
- `WebDavRepository`

这样页面和 manager 就不需要直接知道 Room、MediaStore、OkHttp 的细节。

### 8.3 第三优先级：统一设置/主题体系

建议：

- 合并两个 `ThemeManager`
- 明确“UI 主题”和“夜间模式”只有一个来源
- 把 `PreferencesManager` 逐步从“大而全 key-value 仓库”拆成多个子设置域

例如：

- `PlayerPreferences`
- `DanmakuPreferences`
- `SubtitlePreferences`
- `LibraryPreferences`
- `ThemePreferences`

### 8.4 第四优先级：整理在线模块边界

当前在线模块已经很多，但结构上还偏“能用就接”。

建议统一约定：

- 网络客户端放到独立 package
- API model 与 UI model 分离
- 认证、解析、下载分别拆层
- 对外只暴露 Repository / UseCase

尤其是哔哩哔哩模块，后续维护成本会越来越高，值得优先治理。

### 8.5 第五优先级：处理安全与合规问题

建议按风险分级推进：

1. 优先去掉 URL 中嵌入账号密码的方式
2. 为 WebDAV 增加“严格校验证书 / 兼容模式”开关
3. 梳理 `MANAGE_EXTERNAL_STORAGE` 的真实必要性
4. 把 TLS 放宽策略限制在确有需要的场景，而不是默认全局关闭

### 8.6 第六优先级：建立最小测试面

当前仓库几乎没有测试。建议至少从以下 4 类开始：

- `VideoCacheEntity.generateSortKey()` 纯函数测试
- `SeriesManager` 的自然排序和上下集识别测试
- `BilibiliDownloadManager` 的 ID 解析测试
- `UrlDetector` / `VideoSnifferManager` 的规则测试

这些都是成本低、收益高的切入点。

## 9. 面向后续开发的“入口建议”

如果后续是按需求开发，可以按下面的入口找代码：

### 9.1 想改播放行为

优先看：

- `VideoPlayerActivity`
- `player/PlaybackEngine.kt`
- `player/PlayerControlsManager.kt`
- `player/GestureHandler.kt`
- `player/PlayerDialogManager.kt`

### 9.2 想改 Anime4K 或画质策略

优先看：

- `Anime4KManager.kt`
- `app/src/main/assets/shaders/`
- `VideoPlayerActivity.applyAnime4K()`

### 9.3 想改本地媒体库 / 列表性能

优先看：

- `VideoBrowserComposeActivity.kt`
- `VideoListComposeActivity.kt`
- `database/VideoCacheDao.kt`
- `paging/VideoPagingSource.kt`

### 9.4 想改弹幕/字幕

优先看：

- `danmaku/DanmakuManager.kt`
- `subtitle/SubtitleDownloadManager.kt`
- `manager/SubtitleManager.kt`
- `SubtitleSearchActivity.kt`

### 9.5 想改哔哩哔哩播放/下载

优先看：

- `bilibili/auth/BiliBiliAuthManager.kt`
- `BiliBiliPlayActivity.kt`
- `download/BilibiliDownloadManager.kt`
- `download/BilibiliDownloadViewModel.kt`

### 9.6 想改远程播放 / WebDAV

优先看：

- `webdav/WebDavClient.kt`
- `webdav/WebDavAccountManager.kt`
- `webdav/WebDavBrowserComposeActivity.kt`

### 9.7 想改网页嗅探

优先看：

- `tv/TVBrowserActivity.kt`
- `sniffer/VideoSnifferManager.kt`
- `sniffer/UrlDetector.kt`

## 10. 结论

这个项目的本质不是“简单播放器”，而是一个已经具有明显产品复杂度的媒体应用。它最强的部分在于：

- 播放主链路已经跑通
- 在线与本地能力都比较完整
- 播放器增强特性很多
- 大部分功能已经能通过统一播放器页承接

它最需要治理的部分也很明确：

- 播放器核心编排过重
- 数据层与网络层边界不足
- 主题/命名/文档存在历史叠加痕迹
- 安全与合规策略偏宽松

如果后续开发目标是“继续加功能”，当前结构依然能承受一段时间；如果目标是“长期维护 + 扩展 + 降低回归风险”，那么最佳路线不是重写，而是围绕播放器主链路、数据抽象层和在线模块边界做渐进式重构。

## 11. 面向“嗅探链接实时播放 + 实时超分”的专项分析

当前仓库其实已经具备这条能力链路的雏形：

1. `TVBrowserActivity` 通过 `WebView.shouldInterceptRequest()` 拿到页面请求。
2. `VideoSnifferManager` + `UrlDetector` 从请求里筛出疑似视频链接。
3. `DetectedVideo` 把 URL 和请求头打包后跳转 `VideoPlayerActivity`。
4. `VideoPlayerActivity` 在检测到 `http/https` 后走 `PlaybackEngine.loadVideoFromUrl()`。
5. `PlaybackEngine` 把远程 URL 下发给 mpv，最终仍然复用同一套 `Anime4K` 着色器链。

这说明“输入一个浏览器里能播放的直链，然后在本项目里实时播放并叠加 Anime4K”在架构上是可行的，并不需要额外再造一套播放器内核。后续开发的重点不在“能不能播”，而在“如何把远程播放入口做成稳定、统一、兼容头信息和重定向的链路”。

### 11.1 当前实现已经具备的能力

- 已支持 `http/https` 远程 URL 进入 `VideoPlayerActivity`
- 已支持把 WebView 请求头传递给 mpv
- 已为在线视频开启缓存、重定向、HLS 相关 mpv 选项
- 已经复用同一套 `Anime4KManager`，所以在线链接理论上也能实时超分

### 11.2 当前实现距离“像浏览器一样稳定播放”还差的关键点

- 远程播放请求目前是通过 `DetectedVideo.toFullUrlString()` 把“URL + headers”硬编码成一个字符串，再由 `PlaybackEngine.parseUrlWithHeaders()` 反向解析，类型边界比较脆弱。
- `VideoPlayerActivity` 内仍保留了按场景拼补丁的逻辑，例如对 B 站单独写 `Referer`，说明远程播放入口还没有抽象成统一能力。
- `CustomMPVView` 和 `PlaybackEngine.loadVideoFromUrl()` 都会改写 mpv 的 HTTP 相关 option，但目前没有清晰的“每次播放前重置远程请求上下文”的机制，后续容易出现上一次请求头污染下一次播放的问题。
- `VideoPlayerActivity.playVideo(uri)` 对在线 URI 与本地 URI 没有统一走同一条远程播放分支，这会让后续“多链接切换 / 清晰度切换 / 播放列表”变得不稳定。
- `UrlDetector` 当前主要依赖扩展名、路径关键词和 `Content-Type` 启发式判断，足够覆盖简单 `mp4/m3u8`，但对短链、跳转链、签名 URL、无扩展名媒体接口、主清单/子清单切换的兼容性还不够。
- 主页目前没有“直接输入 URL 播放”的明确入口；现有“输入网址”更多是 TV/WebView 浏览入口，不是远程播放入口。
- WebDAV 仍通过把账号密码直接嵌入 URL 来播放，这和后续要建设的“统一远程请求模型”是冲突的。

### 11.3 对你要做的目标功能，最合理的改造方向

后续不要继续把远程播放能力散落在 `TVBrowserActivity`、`VideoPlayerActivity`、`PlaybackEngine` 的条件分支里，而是应该补一个统一的远程播放模型，建议最少包含：

- `RemotePlaybackRequest`
  - `url`
  - `title`
  - `sourcePageUrl`
  - `headers`
  - `detectedContentType`
  - `isLiveOrStream`
- `RemotePlaybackResolver`
  - 负责跳转跟随、必要的预探测、最终 URL 归一化、请求头过滤与补齐
- `RemotePlaybackLauncher`
  - 负责从 WebView 嗅探、主页手输 URL、WebDAV、后续其它在线源统一跳转到播放器
- `PlaybackEngine.loadRemote(request)`
  - 统一设置 mpv 的 `user-agent`、`referrer`、`http-header-fields`、缓存与流媒体选项

这样后续无论你是“手动输入一个 mp4 直链”、还是“从 WebView 嗅探一个带 Cookie/Referer 的链接”、还是“未来接第三方在线源”，最终都会进入同一条播放器入口。

### 11.4 为什么这条路能直接复用实时超分

本项目的实时超分本质上是 mpv 的 GLSL shader 链管理，不依赖文件来源：

- 本地文件能播时，Anime4K 作用在 mpv 输出纹理上
- 远程 URL 能播时，Anime4K 同样作用在 mpv 输出纹理上

所以在线视频不需要单独做“在线版超分”，真正要处理的是：

- 远程流的稳定解复用
- 确保请求头、重定向、缓存策略正确
- 在设备性能不足时避免“在线视频解码 + Anime4K”同时开启导致掉帧

### 11.5 后续开发建议的优先级

1. 先统一远程播放数据模型，停止继续用“URL 拼 headers 字符串”做跨页面传递。
2. 增加“直接输入 URL 播放”入口，优先验证直链 `mp4`、`m3u8`、带签名参数 URL 的主链路。
3. 为 WebView 嗅探补一个远程请求归一化层，只保留真正影响播放的关键头，如 `Referer`、`Origin`、`User-Agent`、`Cookie`、`Authorization`。
4. 再做重定向探测、短链展开、无扩展名媒体接口识别、失败重试与错误提示。
5. 最后再处理清晰度切换、播放列表、远程字幕/弹幕、WebDAV 统一接入等增强项。

专项实施设计我已经补到了新文档 `docs/remote_url_playback_design.md`，后续开发建议直接以那份文档作为任务拆分基线。

---

## 附：本次分析重点参考文件

- `app/src/main/java/com/fam4k007/videoplayer/AppApplication.kt`
- `app/src/main/java/com/fam4k007/videoplayer/MainActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/VideoBrowserComposeActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/VideoListComposeActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/VideoPlayerActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/player/PlaybackEngine.kt`
- `app/src/main/java/com/fam4k007/videoplayer/player/CustomMPVView.kt`
- `app/src/main/java/com/fam4k007/videoplayer/danmaku/DanmakuManager.kt`
- `app/src/main/java/com/fam4k007/videoplayer/Anime4KManager.kt`
- `app/src/main/java/com/fam4k007/videoplayer/PlaybackHistoryManager.kt`
- `app/src/main/java/com/fam4k007/videoplayer/database/VideoDatabase.kt`
- `app/src/main/java/com/fam4k007/videoplayer/database/VideoCacheDao.kt`
- `app/src/main/java/com/fam4k007/videoplayer/paging/VideoPagingSource.kt`
- `app/src/main/java/com/fam4k007/videoplayer/bilibili/auth/BiliBiliAuthManager.kt`
- `app/src/main/java/com/fam4k007/videoplayer/download/BilibiliDownloadManager.kt`
- `app/src/main/java/com/fam4k007/videoplayer/download/BilibiliDownloadViewModel.kt`
- `app/src/main/java/com/fam4k007/videoplayer/subtitle/SubtitleDownloadManager.kt`
- `app/src/main/java/com/fam4k007/videoplayer/webdav/WebDavClient.kt`
- `app/src/main/java/com/fam4k007/videoplayer/webdav/WebDavBrowserComposeActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/tv/TVBrowserActivity.kt`
- `app/src/main/java/com/fam4k007/videoplayer/sniffer/VideoSnifferManager.kt`
- `app/src/main/java/com/fam4k007/videoplayer/utils/SecureStorage.kt`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
