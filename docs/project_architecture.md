# FAM4K007 项目架构与技术栈文档

> 最后更新: 2026-06-15  
> 版本: 1.2.8

---

## 目录

1. [项目概述](#1-项目概述)
2. [整体架构](#2-整体架构)
3. [技术栈](#3-技术栈)
4. [分层详解](#4-分层详解)
   - [4.1 UI 层](#41-ui-层)
   - [4.2 Presentation 层](#42-presentation-层)
   - [4.3 Domain 层](#43-domain-层)
   - [4.4 Repository 层](#44-repository-层)
   - [4.5 Data 层](#45-data-层)
   - [4.6 DI 层](#46-di-层)
5. [导航系统](#5-导航系统)
6. [状态管理](#6-状态管理)
7. [Manager 模式详解](#7-manager-模式详解)
8. [双 UI 体系](#8-双-ui-体系)
9. [内存管理策略](#9-内存管理策略)
10. [功能模块详解](#10-功能模块详解)
    - [10.1 TV Box 模块](#101-tv-box-模块)
    - [10.2 Bilibili 番剧索引模块](#102-bilibili-番剧索引模块)
11. [构建与配置](#11-构建与配置)
12. [包结构总览](#12-包结构总览)

---

## 1. 项目概述

FAM4K007 是一款基于 **mpv** 播放引擎的 Android 视频播放器，专注于：

- **高性能播放**：基于 mpv 原生播放器，支持硬件解码
- **Anime4K 实时画质增强**：集成 Anime4K GLSL 着色器
- **弹幕支持**：集成 Bilibili/DanDanPlay 弹幕系统，支持多弹幕服务器管理与并行搜索
- **字幕管理**：支持多种字幕格式（ASS/SRT/VTT）和在线搜索
- **多协议支持**：本地文件、WebDAV、HTTP/HTTPS/RTSP 远程播放
- **B 站生态**：Bilibili API 集成（番剧索引、信息获取、播放地址解析、弹幕下载、二维码登录）
- **下载功能**：Bilibili 视频下载管理器
- **后台播放**：支持一键切到后台只听音频，通过前台 Service 保持进程存活
- **TV Box 支持**：支持 TV Box 数据源（多源对接、JAR 加解密、HTTP 代理、点播详情）
- **番剧索引**：B 站番剧检索（筛选条件、分页加载、链接输入直达、选集播放）

---

## 2. 整体架构

采用 **基于 Manager 模式的半 MVVM 分层架构**，共 7 层：

```
┌──────────────────────────────────────────────────┐
 │                    UI 层                          │
 │   ┌──────────────────────┬────────────────────┐   │
 │   │  传统 View (XML)      │   Compose Screens   │   │
 │   │  VideoPlayerActivity   │  25 个 Screen      │   │
 │   │  CustomMPVView         │  + TV Box 3 页面   │   │
 │   │  CustomMPVView         │  ui/screens/       │   │
 │   │  DanmakuPlayerView     │  ui/components/    │   │
 │   │  7 个扩展文件          │  ui/player/        │   │
 │   └──────────────────────┴────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │            Manager 层 (Compose 覆盖层)            │
 │   ┌──────────────────────────────────────────┐   │
 │   │  ComposeOverlayManager — 覆盖层管理      │   │
 │   │  11 个抽屉/弹窗组件 (manager/compose/)    │   │
 │   │  ChapterDrawer  SpeedDrawer  Equalizer   │   │
 │   │  DanmakuDialogs SubtitleDialogs          │   │
 │   │  SkipSettings   VideoList   FilePickers  │   │
 │   │  DolbyVisionHintDialog                   │   │
 │   ├──────────────────────────────────────────┤   │
 │   │  ScreenshotManager  SkipIntroOutroManager│   │
 │   │  ThemeManager                             │   │
 │   └──────────────────────────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │              Presentation 层                      │
 │   ┌──────────────────────────────────────────┐   │
 │   │  15 个 ViewModel (StateFlow/SharedFlow)   │   │
 │   │  PlayerViewModel  (核心, 监听 MPV 事件)   │   │
 │   │  LibraryViewModel  SettingsViewModel      │   │
 │   │  BangumiIndexViewModel  BangumiDetailVM   │   │
 │   │  BilibiliViewModel  WebDavViewModel       │   │
 │   │  ...                                      │   │
 │   └──────────────────────────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │               Domain 层                           │
 │   ┌──────────────────────────────────────────┐   │
 │   │  8 个 Manager/Engine                      │   │
 │   │  PlaybackEngine      PlayerControlsManager│   │
 │   │  DanmakuManager      GestureHandler       │   │
 │   │  Anime4KManager      SeriesManager        │   │
 │   │  FilePickerManager   PlayerDialogManager  │   │
 │   ├──────────────────────────────────────────┤   │
 │   │  辅助 Domain: sniffer/ subtitle/ webdav/  │   │
 │   └──────────────────────────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │              Service 层                           │
 │   ┌──────────────────────────────────────────┐   │
 │   │  BackgroundPlaybackService               │   │
 │   │  前台 Service，维持后台播放进程存活        │   │
 │   └──────────────────────────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │              Repository 层                        │
 │   ┌──────────────────────────────────────────┐   │
 │   │  8 个 Repository                          │   │
 │   │  PlayerRepository   VideoRepository       │   │
 │   │  BilibiliRepository BangumiRepository     │   │
 │   │  TvBoxRepository    WebDavRepository      │   │
 │   │  SubtitleRepository MediaInfoRepository   │   │
 │   └──────────────────────────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │                Data 层                            │
 │   ┌──────────┬──────────┬──────────┬──────────┐  │
 │   │  Room DB │ SharedPref│ 网络 API │ 本地 AAR │  │
 │   │  2 表     │ Preferences│ B站/弹幕 │ mpv      │  │
 │   │          │ Manager   │ TV Box  │ Danmaku   │  │
 │   │          │           │ WebDAV  │           │  │
 │   └──────────┴──────────┴──────────┴──────────┘  │
 ├──────────────────────────────────────────────────┤
 │            DI 层 (Koin)                           │
 │   ┌──────────────────────────────────────────┐   │
 │   │ AppModule  DatabaseModule  NetworkModule  │   │
 │   │ RepositoryModule  DomainModule            │   │
 │   │ PresentationModule (6 模块)               │   │
 │   └──────────────────────────────────────────┘   │
 └──────────────────────────────────────────────────┘
```

### 架构特点

| 特点 | 说明 |
|------|------|
| **Manager 模式** | 不用 UseCase，以 Manager/Engine 封装业务逻辑，职责单一、边界清晰 |
| **Koin DI** | 轻量级依赖注入，5 个模块解耦各层 |
| **StateFlow 驱动 UI** | ViewModel 暴露 StateFlow，UI 层通过 `collectAsState()` 响应 |
| **WeakReference 防泄漏** | Activity 内创建的 Manager 一律用 `WeakReference` 持有 |
| **双 UI 体系** | 播放核心用传统 View（性能稳定），其余页面全量 Compose |
| **Repository 封装** | DAO / 网络 / Preferences 统一走 Repository |
| **后台播放** | 前台 Service 维持进程存活，Activity 生命周期按需跳过暂停/销毁 |

---

## 3. 技术栈

### 开发环境

| 工具 | 版本 |
|------|------|
| Kotlin | 2.0.20 |
| AGP | 8.5.2 |
| KSP | 2.0.20-1.0.25 |
| compileSdk | 34 |
| minSdk | 26 |
| targetSdk | 34 |
| NDK | 25.2.9519653 |

### 核心依赖

| 类别 | 库 | 版本 | 用途 |
|------|-----|------|------|
| **DI** | Koin | 4.0.0 | 依赖注入 |
| **UI (Compose)** | Compose BOM | 2024.09.03 | Compose UI 框架 |
| | Navigation Compose | 2.8.2 | 类型安全导航 |
| | Material3 | BOM 管理 | Material Design 3 |
| **UI (传统)** | AppCompat | 1.7.0 | 传统 View 兼容 |
| | RecyclerView | 1.3.2 | 列表 |
| | Material | 1.12.0 | MDC 组件 |
| **播放器** | mpv-android-lib | 0.1.10 | 播放引擎 (AAR) |
| **弹幕** | DanmakuFlameMaster | - | 弹幕渲染 (AAR) |
| **数据库** | Room | 2.6.1 | 本地持久化 |
| **网络** | OkHttp | 4.12.0 | HTTP 客户端 |
| | Jsoup | 1.18.1 | HTML 解析 |
| **图片** | Coil | 2.7.0 | Compose 图片加载 |
| | Glide | 4.16.0 | View 图片加载 |
| **协程** | Kotlinx Coroutines | 1.9.0 | 异步 |
| **序列化** | Kotlinx Serialization | 1.7.3 | JSON / 导航路由 |
| **媒体信息** | MediaInfoAndroid | 1.0.0-fix | 媒体元数据 (AAR) |
| **串流嗅探** | Seeker | 2.0.1 | 视频流探测 (AAR) |
| **WebDAV** | Sardine | 1.0.2 | WebDAV 客户端 (JAR) |
| **分页** | Paging 3 | 3.3.2 | 大列表分页加载 |
| **WorkManager** | Work Runtime | 2.9.1 | 后台任务 |
| **ZXing** | Core | 3.5.3 | 二维码扫码 |
| **安全** | Security Crypto | 1.1.0-alpha06 | 敏感数据加密存储 |
| **开源许可** | AboutLibraries | 11.2.3 | 开源许可展示 |
| **Gson** | Gson | 2.11.0 | JSON 解析 (兼容) |

---

## 4. 分层详解

### 4.1 UI 层

**位置**: `ui/`, `player/`, `danmaku/`, `mediainfo/` 及顶层 Activity

UI 层分为两个子系统：

#### 传统 View 子系统

| 文件 | 职责 |
|------|------|
| `VideoPlayerActivity` | 播放器主 Activity，集成 mpv View、弹幕 View、控制栏 |
| `VideoPlayerActivity+Setup` | 初始化、Manager 创建、杜比视界检测 |
| `VideoPlayerActivity+Playback` | 播放控制、均衡器恢复 |
| `VideoPlayerActivity+UI` | UI 相关操作 |
| `VideoPlayerActivity+Observers` | MPV 事件观察 |
| `VideoPlayerActivity+Danmaku` | 弹幕操作（多服务器匹配、来源标注选择、网络弹幕加载） |
| `VideoPlayerActivity+Subtitle` | 字幕操作 |
| `VideoPlayerActivity+Orientation` | 横竖屏切换 |
| `CustomMPVView` | mpv 播放器 Surface 封装，配置解码/字幕/音视频参数/GPU Next |
| `DanmakuPlayerView` | 弹幕渲染 View |
| `CustomSeekbar` | 自定义播放进度条 |
| `DoubleTapSeekIndicator` | 双击快进指示器 |

#### Compose 子系统

| 目录 | 职责 |
|------|------|
| `ui/screens/` | 26 个全屏页面（Home、Settings、Library、DanmakuServer 等），含 `dialogs/` 子目录 |
| `ui/components/` | 11 个可复用 Compose 组件（弹窗、卡片、TopAppBar、SliderItem 等） |
| `ui/player/` | 播放器控制面板 Compose（PlayerControls） |
| `ui/theme/` | 主题（亮/暗/跟随系统）、颜色、排版 |
| `ui/webdav/` | WebDAV 相关页面 |
| `ui/viewmodels/` | UI 层 ViewModel（Compose 页面专用） |
| `manager/compose/` | 11 个 Compose 弹窗抽屉（弹幕设置、字幕设置、视频列表、章节、倍速、均衡器、片头片尾、杜比视界提示等） |

### 4.2 Presentation 层

**位置**: `presentation/`

| ViewModel | 职责 |
|-----------|------|
| `PlayerViewModel` | 播放器核心状态（播放/暂停、进度、速度、解码、弹幕、字幕等） |
| `LibraryViewModel` | 视频库列表、扫描 |
| `SettingsViewModel` | 应用设置 |
| `PlaybackSettingsViewModel` | 播放相关设置 |
| `BilibiliViewModel` | B 站用户状态、番剧搜索 |
| `BangumiIndexViewModel` | 番剧索引（筛选、分页、搜索） |
| `BangumiDetailViewModel` | 番剧详情、选集播放 |
| `WebDavViewModel` | WebDAV 文件浏览 |
| `SubtitleSearchViewModel` | 字幕在线搜索 |
| `PlaybackHistoryViewModel` | 播放历史 |
| `CacheManagementViewModel` | 缓存管理 |
| `MediaInfoViewModel` | 媒体信息 |
| `LogViewerViewModel` | 日志查看 |
| `TVBrowserViewModel` | TV 模式浏览器 |

**StateFlow 驱动模式**:

```
ViewModel
  │
  ├── val paused: StateFlow<Boolean>
  ├── val progress: StateFlow<Pair<Double, Double>>
  ├── val currentSheet: StateFlow<Sheets>
  ├── val currentPanel: StateFlow<Panels>
  ├── val playerUpdates: SharedFlow<PlayerUpdates>
  │
  └── UI 层 collectAsState()
```

**PlayerUpdates (SharedFlow)** — 一次性事件（速度变化提示、音量变化提示等），用 SharedFlow 避免重放。

### 4.3 Domain 层

**位置**: `domain/`

这是项目的核心业务逻辑层，以 Manager/Engine 模式组织。

| Manager | 职责 | 生命周期 |
|---------|------|----------|
| `PlaybackEngine` | mpv 播放控制、进度轮询、事件回调 | Activity 绑定 |
| `PlayerControlsManager` | 控制栏显隐、按钮事件、系统 UI 管理 | Activity 绑定 |
| `GestureHandler` | 手势识别（滑动/双击/长按/拖动/长按动态调速） | Activity 绑定 |
| `PlayerDialogManager` | 底部弹窗（速度/比例/字幕/音轨等） | Activity 绑定 |
| `DanmakuManager` | 弹幕加载、状态机管理、样式控制 | Activity 绑定 |
| `Anime4KManager` | Anime4K 着色器加载与切换 | 全局单例 |
| `SeriesManager` | 同系列视频识别与导航 | Activity 绑定 |
| `SubtitleManager` | 字幕解析与显示控制 | 无状态 (factory) |
| `FilePickerManager` | 字幕/弹幕文件选择 | Activity 绑定 |
| `VideoSelector` | 串流协议视频选择 | 无状态 |
| `SkipIntroOutroManager` | 片头片尾自动跳过 | Activity 绑定 |

**Manager 模式说明**:

```kotlin
// PlaybackEngine 封装了 mpv 的所有操作
class PlaybackEngine(
    private val mpvView: CustomMPVView,      // 依赖具体 View
    private val contextRef: WeakReference<Context>, // WeakReference 防泄漏
    private val eventCallback: PlaybackEventCallback // 回调
) : MPVLib.EventObserver { ... }
```

### 4.4 Repository 层

**位置**: `repository/`

| Repository | 职责 |
|------------|------|
| `PlayerRepository` | 播放历史 CRUD、播放设置读写 |
| `VideoRepository` | 视频文件扫描、缓存管理 |
| `BilibiliRepository` | B 站 API、DanDanPlay 弹幕搜索 |
| `WebDavRepository` | WebDAV 文件列表与操作 |
| `SubtitleRepository` | 字幕在线搜索（对接多个源） |
| `MediaInfoRepository` | 媒体文件元信息 |

数据流向：

```
ViewModel → Repository → DAO / Network / Preferences
                 ↓
            Domain Model (Entity / Data Class)
```

### 4.5 Data 层

**位置**: `database/`, `preferences/`, `data/`, `danmaku/`, `bilibili/`, `dandanplay/`

#### Room 数据库

```
VideoDatabase (v4)
  ├── video_cache             — 视频文件缓存记录
  │   ├── path, name, size, duration, folderName
  │   ├── folderPath, lastScanned, dateAdded
  │   ├── nameSortKey (自然排序)
  │   └── 索引: folderName, (folderPath, name), lastScanned
  │
  └── playback_history      — 播放历史
      ├── uri, fileName, position, duration
      ├── lastPlayed, folderName
      ├── danmuPath, danmuVisible, danmuOffsetTime
      └── 索引: lastPlayed DESC
```

#### SharedPreferences

`PreferencesManager` 统一管理所有设置项：
- 播放设置（快进时长、倍速、解码方式）
- UI 设置（主题模式、显示模式）
- 弹幕设置（大小、速度、透明度、行数限制、多服务器列表）
- 字幕设置（延迟、缩放、颜色、描边）
- 黑名单、更新设置、手势配置等
- 功能引导标记（如长按动态调速已使用）

#### 网络 API

- **Bilibili API**: 番剧信息、视频流解析、二维码登录
- **DanDanPlay API**: 弹幕搜索与下载
- **WebDAV**: 远程文件管理

### 4.6 Service 层

**位置**: `service/`

| 组件 | 职责 |
|------|------|
| `BackgroundPlaybackService` | 前台 Service，在"听视频"模式下维持进程存活。使用 `Notification.Builder` 构建通知，`START_STICKY` 重启策略。Activity 生命周期跳过暂停/销毁逻辑由 `isManualBackgroundPlayback` 标记控制。 |

### 4.7 DI 层

**位置**: `di/`

Koin 5 个模块：

```kotlin
val appModules = listOf(
    databaseModule,      // Room 数据库、DAO 单例
    networkModule,       // BiliBiliAuthManager、DanDanPlayApi (factory 模式，支持多服务器)
    repositoryModule,    // 所有 Repository 单例
    domainModule,        // PreferencesManager、Anime4KManager 等
    presentationModule   // 所有 ViewModel
)
```

需要 Activity 引用的 Manager（如 `PlaybackEngine`）不在 DI 中注册，而是由 `VideoPlayerActivity` 在 `initializeManagers()` 中手动构造。

---

## 5. 导航系统

**位置**: `navigation/`

使用 **Navigation Compose** + **Kotlin Serialization 类型安全路由**：

```kotlin
sealed interface AppScreen {
    @Serializable data object Home : AppScreen
    @Serializable data object Settings : AppScreen
    @Serializable data class WebDavBrowser(val accountId: String) : AppScreen
    @Serializable data class Player(val videoUri: String) : AppScreen
    @Serializable data class BangumiDetail(val seasonId: Int, val isEpId: Boolean = false) : AppScreen
    @Serializable data object BangumiIndex : AppScreen
    @Serializable data object TVBoxSearch : AppScreen
    // ...
}
```

- `AppNavGraph` — 主导航图，定义所有页面路由和转场动画
- 导航统一走 Compose，`VideoPlayerActivity` 通过 Intent 启动

---

## 6. 状态管理

### 类型选择

| 类型 | 用途 | 示例 |
|------|------|------|
| `StateFlow` | 持续性状态 | 播放/暂停、进度、速度、列表数据 |
| `SharedFlow` | 一次性事件 | 快进提示、音量变化、Toast |
| `mutableStateOf` | Compose 内部状态 | 抽屉展开/折叠、输入框内容 |

### 状态流

```
mpv 属性变化
    ↓ MPVLib.EventObserver
PlayerViewModel (StateFlow)
    ↓ collectAsState()
Compose UI 重组
```

进度更新采用 **Handler + Runnable 500ms 轮询**（非协程方式），从 mpv 读取 position/duration 后写入 StateFlow。

---

## 7. Manager 模式详解

这是项目最核心的设计模式。Manager 替代了传统 MVVM 中的 UseCase 角色。

### 分类

#### A. 全局单例 Manager（Koin single）

| Manager | 初始化 |
|---------|--------|
| `PreferencesManager` | 手动双重检查锁 + Koin 包装 |
| `Anime4KManager` | Koin single |
| `ThumbnailCacheManager` | 手动双重检查锁 + Koin 包装 |
| `ThemeManager` | Koin factory（无状态） |

#### B. Activity 绑定 Manager（手动构造 + WeakReference）

| Manager | 引用方式 | 原因 |
|---------|----------|------|
| `PlaybackEngine` | `WeakReference<Context>` + `mpvView` | 依赖 mpv View |
| `PlayerControlsManager` | `WeakReference<Activity>` | 操作系统 UI |
| `GestureHandler` | `WeakReference<Activity>` | 触摸事件 |
| `PlayerDialogManager` | `WeakReference<Activity>` + 多个 Manager | 对话框管理 |
| `DanmakuManager` | `danmakuView` | 依赖弹幕 View |
| `FilePickerManager` | `WeakReference<Activity>` | 文件选择 |
| `SkipIntroOutroManager` | 直接持有 Context | 仅 SharedPreferences 操作 |

#### C. 无状态 Manager（Koin factory）

| Manager |
|---------|
| `SubtitleManager` |
| `VideoSelector` |
| `SeriesManager` |

### 初始化流程（VideoPlayerActivity）

```
onCreate()
  │
  ├── initMPV()                    — 初始化 mpv 引擎
  ├── initViews()                  — 绑定 View
  ├── 读取用户设置 / 音量增强状态
  ├── 创建后台播放通知渠道
  ├── initializeManagers()
  │   ├── PlaybackEngine           — 播放控制
  │   ├── PlayerControlsManager    — 控制栏
  │   ├── GestureHandler           — 手势
  │   ├── ComposeOverlayManager    — Compose 弹窗
  │   ├── PlayerDialogManager      — 底部弹窗
  │   ├── FilePickerManager        — 文件选择
  │   ├── DanmakuManager           — 弹幕
  │   ├── Anime4KManager           — Anime4K
  │   ├── ScreenshotManager        — 截图
  │   ├── SkipIntroOutroManager    — 片头片尾跳过
  │   └── bindViewsToManagers()    — 绑定回调
  │
  ├── setupComposeControls()       — Compose 控制面板
  └── loadVideo()                  — 加载视频

后台播放流程（点击"听视频"后）：
  onBackgroundPlayback()
    ├── isManualBackgroundPlayback = true
    ├── 暂停弹幕
    ├── startForegroundService()   → BackgroundPlaybackService
    ├── 发送 Home Intent (Activity 切到后台)
    │
    onStop() → isManualBackgroundPlayback=true → 跳过暂停
    onDestroy() → 不销毁 MPV，保持 Service 存活
    
  返回前台：
    onStart() → endBackgroundPlayback() → 停止 Service
```

---

## 8. 双 UI 体系

| 场景 | 方案 | 原因 |
|------|------|------|
| 视频播放器主界面 | 传统 View (XML) | mpv Surface 是原生 View，控制栏图标/弹窗需精确 Layout 控制 |
| 控制面板覆盖层 | Compose | ComposeOverlayManager 动态注入 Compose 弹窗 |
| 设置/列表/浏览器 | 全量 Compose | Compose 开发效率高，Material3 适配好 |
| 弹幕渲染 | 传统 View (SurfaceView) | 弹幕引擎是 AAR 库，必须用 View 集成 |
| TV 模式 | Activity + Compose | TV 焦点导航使用传统方式 |

**ComposeOverlayManager** 是衔接两种 UI 体系的关键：

```kotlin
class ComposeOverlayManager(context, lifecycleOwner, rootView: ViewGroup) {
    fun setContent(content: @Composable () -> Unit) {
        // 在 ViewGroup 上叠加 ComposeView
    }
    fun clearContent() { ... }
}
```

播放器主界面之所以保留传统 View，是因为：
- mpv 播放 Surface 是原生 View，Compose 无法直接替代
- 控制栏按钮、弹幕 View、进度条等需要精确的 Z-order 和 Layout 控制
- Compose 作为覆盖层通过 `ComposeOverlayManager` 叠加在 View 体系之上

---

## 9. 内存管理策略

### WeakReference 模式

所有需要持有 Activity/Context 引用的 Manager 都使用 `WeakReference`：

```kotlin
class PlayerControlsManager(
    private val activityRef: WeakReference<AppCompatActivity>,
    ...
)
class PlaybackEngine(
    private val contextRef: WeakReference<Context>,
    ...
)
```

### 生命周期管理

- `initializeManagers()` — 在 `onCreate()` 中创建所有 Manager
- `releaseManagers()` — 在 `onDestroy()` 中释放
- 所有 `internal lateinit var` — 保证声明周期内必有值

### 数据库连接预热

`AppApplication.onCreate()` 中后台协程提前初始化 Room 数据库连接，减少首次查询延迟。

---

## 10. 功能模块详解

### 10.1 TV Box 模块

**位置**: `tvbox/`

支持 TV Box 数据源协议，实现多源视频资源的搜索和播放。

#### 模块结构

```
tvbox/
├── config/
│   ├── ConfigParser.kt          — 配置解析（JSON 格式数据源配置）
│   └── TvBoxConfigManager.kt    — 配置管理器（加载、缓存、多源管理）
├── crawler/
│   └── JarLoader.java           — JAR 爬虫加载器（Java 实现，支持加解密）
├── model/
│   ├── ParseBean.kt             — 解析结果 Bean
│   ├── SourceBean.kt            — 数据源配置 Bean
│   └── VodInfo.kt               — 点播/剧集信息 Bean
├── repository/
│   └── TvBoxRepository.kt       — 数据仓库
├── server/
│   └── TvBoxProxyServer.kt      — HTTP 代理服务器（本地代理转发）
├── ui/
│   ├── TvBoxSearchContainerScreen.kt — 搜索容器（含搜索入口和结果展示）
│   ├── TvBoxSearchScreen.kt     — 搜索页面
│   └── VodDetailDialog.kt       — 点播详情对话框
└── viewmodel/
    └── TvBoxSearchViewModel.kt  — ViewModel
```

#### 数据流

```
用户搜索 → TvBoxSearchScreen
    ↓
TvBoxSearchViewModel
    ↓
TvBoxRepository → ConfigParser → HTTP/JAR 爬虫
    ↓                                    ↓
VodDetailDialog                  TvBoxProxyServer (本地代理)
    ↓                                    ↓
选择剧集 → RemotePlaybackLauncher → mpv 播放器
```

#### 关键实现

- **多源支持**：通过 `TvBoxConfigManager` 管理多个数据源
- **JAR 解密**：`JarLoader.java` 处理加密封装的数据源爬虫
- **HTTP 代理**：`TvBoxProxyServer` 作为本地代理服务器，转发和处理请求
- **播放对接**：解析后的视频 URL 通过 `RemotePlaybackLauncher` 启动 mpv 播放

---

### 10.2 Bilibili 番剧索引模块

**位置**: `bilibili/` + `presentation/` + `ui/screens/`

通过 B 站 PGC Web API 实现番剧检索、详情查看和选集播放的完整功能。

#### 页面流程

```
首页 → "Bilibili" 按钮
    ↓
番剧索引页 (BangumiIndexScreen)
  ├── 筛选条件（10 行：排序/地区/风格/类型/付费/配音/版权/状态/年份/季度）
  ├── 番剧网格（3 列卡片流，无限滚动分页）
  ├── 链接输入（右上角 🔗，支持 ss/ep 链接）
  └── 点击卡片 → 番剧详情页 (BangumiDetailScreen)
                    ├── 封面 + 信息 + 简介
                    ├── 选集网格（4 列）
                    └── 点击剧集 → mpv 播放器
```

#### API 接口

| 接口 | 端点 | 说明 |
|------|------|------|
| 筛选条件 | `/pgc/season/index/condition` | 获取排序/地区/风格等筛选条件 |
| 索引结果 | `/pgc/season/index/result` | 分页获取番剧列表 |
| 番剧详情 | `/pgc/view/web/season` | 获取剧集信息和各集数据 |
| 播放地址 | `/pgc/player/web/playurl` | 获取 FLV 格式播放地址 |

#### 数据层

```kotlin
// BiliBangumiApi — 封装所有 B 站 PGC API 调用
class BiliBangumiApi(authManager: BiliBiliAuthManager) {
    suspend fun getIndexCondition(seasonType): Result<PgcIndexConditionData>
    suspend fun getIndexResult(params, page): Result<PgcIndexResultData>
    suspend fun getSeasonInfo(id, isEpId): Result<PgcInfoResult>
    suspend fun getPlayUrl(avid, bvid, cid, epId, ...): Result<PlayUrlResult>
}

// BangumiRepository — 数据仓库
class BangumiRepository(bangumiApi: BiliBangumiApi) {
    // 筛选条件、索引结果、详情、播放地址、视频URL提取
}

// BangumiIndexViewModel — 索引页状态管理
class BangumiIndexViewModel :
    filterState (10 个筛选条件的选中状态)
    indexItems  (分页加载的番剧列表)
    hasNextPage / isLoadingMore

// BangumiDetailViewModel — 详情页状态管理
class BangumiDetailViewModel :
    seasonInfo  (番剧详情数据)
    episodes    (剧集列表)
    playEpisode() → RemotePlaybackLauncher
```

#### 筛选设计

- **5 行收起 / 10 行展开**：默认显示前 5 行筛选条件
- **每行独立横向滚动**：每行筛选项在该行内横向滑动
- **默认全选**：每行第一个选项（如"全部地区"）默认选中
- **圆形药丸样式**：选中项使用 `primaryContainer` 背景 + 16dp 圆角

#### 卡片样式

```
┌─────────────┐
│ [会员]       │ ← 左上角角标
│   封面图     │
│ [全12话]     │ ← 左下角副标题
├─────────────┤
│ 番剧名称    │ ← 加粗主标题
└─────────────┘
```

#### 播放对接

- 使用 `fnval=0`（FLV 格式，音视频合并）
- 播放请求携带 `Cookie` + `Referer` 请求头
- 通过 `RemotePlaybackLauncher` 启动 mpv 播放器
- 支持 ss 链接（`season_id`）和 ep 链接（`ep_id`）

---

## 11. 构建与配置

### 版本号方案

```
versionCode = baseVersion * 10 + abiCode

abiCode:
  arm64-v8a  → 2
  armeabi-v7a → 1
```

当前: `versionCode = 28`, `versionName = "1.2.8"`

### ABI 拆分

只构建 `arm64-v8a`，不生成 universal APK：

```kotlin
splits {
    abi {
        isEnable = true
        reset()
        include("arm64-v8a")
        isUniversalApk = false
    }
}
```

### ProGuard

Release 构建启用混淆和资源压缩：

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

### 仓库镜像

使用阿里云镜像加速国内构建：

```kotlin
maven { url = uri("https://maven.aliyun.com/repository/google") }
maven { url = uri("https://maven.aliyun.com/repository/public") }
```

---

## 12. 包结构总览

```
com.fam4k007.videoplayer/
│  (26 个顶层文件)
│  VideoPlayerActivity.kt          — 播放器主 Activity
│  VideoPlayerActivity+Setup.kt    — 初始化、Manager 创建、杜比视界检测
│  VideoPlayerActivity+Playback.kt — 播放控制、均衡器恢复
│  VideoPlayerActivity+UI.kt       — UI 相关操作
│  VideoPlayerActivity+Observers.kt— MPV 事件观察
│  VideoPlayerActivity+Danmaku.kt  — 弹幕操作
│  VideoPlayerActivity+Subtitle.kt — 字幕操作
│  VideoPlayerActivity+Orientation.kt — 横竖屏
│  MainActivity.kt                 — 主页 Activity
│  AppApplication.kt               — Application 入口
│  AppConstants.kt                 — 常量定义（Preferences Key、默认值）
│  BaseActivity.kt                 — Activity 基类
│  PlaybackHistoryManager.kt       — 播放历史管理器
│  VideoFileParcelable.kt          — 视频文件 Parcelable
│  VideoFolder.kt                  — 视频文件夹模型
│  AboutComposeActivity.kt         — 关于页面
│  BiliBiliDanmakuComposeActivity.kt — B站弹幕页面
│  DownloadActivity.kt             — 下载页面
│  LicenseActivity.kt              — 开源许可
│  PlaybackHistoryComposeActivity.kt — 播放历史页面
│  PlaybackSettingsComposeActivity.kt — 播放设置页面
│  SettingsComposeActivity.kt      — 设置页面
│  SubtitleSearchActivity.kt       — 字幕搜索页面
│  UserAgreementActivity.kt        — 用户协议页面
│  VideoBrowserComposeActivity.kt  — 视频浏览页面
│  VideoListComposeActivity.kt     — 视频列表页面
│
├── di/                            — Koin 依赖注入
│   ├── AppModule.kt               — 模块入口
│   ├── DatabaseModule.kt          — Room 数据库
│   ├── NetworkModule.kt           — 网络服务
│   ├── RepositoryModule.kt        — 数据仓库
│   ├── DomainModule.kt            — 领域服务
│   └── PresentationModule.kt      — ViewModel
│
│   ├── presentation/                  — 15 个 ViewModel
│   ├── PlayerViewModel.kt         — 播放器核心状态（MPV 事件监听）
│   ├── LibraryViewModel.kt        — 视频库
│   ├── SettingsViewModel.kt       — 设置
│   ├── BilibiliViewModel.kt       — B站
│   ├── BangumiIndexViewModel.kt   — 番剧索引
│   ├── BangumiDetailViewModel.kt  — 番剧详情
│   ├── WebDavViewModel.kt         — WebDAV
│   ├── PlaybackHistoryViewModel.kt
│   ├── PlaybackSettingsViewModel.kt
│   ├── SubtitleSearchViewModel.kt
│   ├── MediaInfoViewModel.kt
│   ├── DeviceInfoViewModel.kt
│   ├── CacheManagementViewModel.kt
│   ├── LogViewerViewModel.kt
│   └── TVBrowserViewModel.kt
│
├── domain/                        — 业务逻辑
│   ├── player/                    — 播放核心
│   │   ├── PlaybackEngine.kt      — mpv 播放控制
│   │   ├── PlayerControlsManager.kt — 控制栏显隐
│   │   ├── PlayerDialogManager.kt — 底部弹窗管理
│   │   ├── GestureHandler.kt      — 手势识别
│   │   ├── Anime4KManager.kt      — Anime4K 着色器
│   │   ├── SeriesManager.kt       — 同系列识别
│   │   └── FilePickerManager.kt   — 文件选择
│   ├── danmaku/                   — 弹幕
│   │   └── DanmakuManager.kt
│   ├── subtitle/                  — 字幕
│   │   └── SubtitleManager.kt
│   ├── sniffer/                   — 串流嗅探
│   │   └── VideoSelector.kt
│   └── webdav/                    — WebDAV
│       └── WebDavClient.kt
│
├── repository/                    — 8 个 Repository
│   ├── PlayerRepository.kt
│   ├── VideoRepository.kt
│   ├── BilibiliRepository.kt
│   ├── BangumiRepository.kt      — 番剧数据仓库
│   ├── TvBoxRepository.kt        — TV Box 数据仓库
│   ├── WebDavRepository.kt
│   ├── SubtitleRepository.kt
│   └── MediaInfoRepository.kt
│
├── database/                      — Room 数据库
│   ├── VideoDatabase.kt
│   ├── VideoCacheDao.kt / Entity.kt
│   └── PlaybackHistoryDao.kt / Entity.kt
│
├── preferences/                   — 设置管理器
│   └── PreferencesManager.kt
│
├── manager/                       — 功能管理器
│   ├── ScreenshotManager.kt       — 截图
│   ├── SkipIntroOutroManager.kt   — 片头片尾跳过
│   ├── ThemeManager.kt            — 主题
│   └── compose/                   — Compose 覆盖层弹窗/抽屉 (11 文件)
│       ├── ComposeOverlayManager.kt — 覆盖层管理器（统一入口）
│       ├── ChapterDrawer.kt       — 章节抽屉
│       ├── SpeedDrawer.kt         — 倍速选择抽屉
│       ├── EqualizerDrawer.kt     — 音频均衡器抽屉
│       ├── SkipSettingsDrawer.kt  — 片头片尾设置抽屉
│       ├── VideoListDrawer.kt     — 视频列表抽屉
│       ├── DanmakuDialogs.kt      — 弹幕设置对话框
│       ├── DanmakuFilePickerDialog.kt — 弹幕文件选择
│       ├── SubtitleDialogs.kt     — 字幕设置对话框
│       ├── SubtitleFilePickerDialog.kt — 字幕文件选择
│       └── DolbyVisionHintDialog.kt — 杜比视界提示对话框
│
├── navigation/                    — 导航
│   ├── AppNavGraph.kt
│   └── AppScreen.kt
│
├── ui/                            — Compose UI
│   ├── screens/                   — 25 个全屏页面
│   │   ├── HomeScreen.kt          — 主页
│   │   ├── SettingsScreen.kt      — 设置
│   │   ├── PlaybackSettingsScreen.kt — 播放设置
│   │   ├── MediaSettingsScreen.kt — 媒体设置
│   │   ├── PlayerScreen.kt        — 播放器
│   │   ├── AboutScreen.kt         — 关于
│   │   ├── BangumiIndexScreen.kt  — 番剧索引
│   │   ├── BangumiDetailScreen.kt — 番剧详情
│   │   ├── BiliBiliLoginScreen.kt — B站登录
│   │   ├── BiliBiliDanmakuScreen.kt — B站弹幕
│   │   ├── LibraryScreen.kt       — 视频库
│   │   ├── FolderBrowserScreen.kt — 文件夹浏览
│   │   ├── VideoListScreen.kt     — 视频列表
│   │   ├── VideoListScreenPaging.kt — 视频列表（分页版）
│   │   ├── PlaybackHistoryScreen.kt — 播放历史
│   │   ├── SubtitleSearchScreen.kt — 字幕搜索
│   │   ├── DeviceInfoScreen.kt    — 设备信息
│   │   ├── CacheManagementScreen.kt — 缓存管理
│   │   ├── DanmakuServerScreen.kt — 弹幕服务器管理
│   │   ├── DownloadScreen.kt      — 下载
│   │   ├── DownloadManagerScreen.kt — 下载管理
│   │   ├── FolderBlacklistScreen.kt — 文件夹黑名单
│   │   ├── LogViewerScreen.kt     — 日志查看
│   │   ├── LicenseScreen.kt       — 开源许可
│   │   ├── UserAgreementScreen.kt — 用户协议
│   │   ├── TVBrowserScreen.kt     — TV 模式浏览
│   │   └── dialogs/               — 页面级对话框
│   │       ├── DisplayModeDialog.kt — 显示模式选择
│   │       └── ThemeDialogs.kt    — 主题选择
│   ├── components/                — 11 个可复用组件
│   │   ├── Dialogs.kt             — 公共对话框（Confirm/Info/Input/Custom/Selection）
│   │   ├── Cards.kt               — 卡片组件
│   │   ├── SliderItem.kt          — 滑动条设置项
│   │   ├── PreferenceItems.kt     — 设置项组件（TextItem/SwitchItem）
│   │   ├── ImmersiveTopAppBar.kt  — 沉浸式顶栏
│   │   ├── UpdateDialog.kt        — 更新对话框
│   │   ├── DanDanPlaySearchDialog.kt — DanDanPlay 搜索
│   │   ├── ExpandableCard.kt      — 可展开卡片
│   │   ├── FileOperationMenu.kt   — 文件操作菜单
│   │   ├── MultiSelectActionBar.kt — 多选操作栏
│   │   └── CommonStates.kt        — 通用状态
│   ├── player/                    — 播放器控件 (8 文件)
│   │   ├── PlayerControlsCompose.kt  — 横屏控制面板（含长按倍速浮层、动态调速档位条）
│   │   ├── PortraitControls.kt       — 竖屏控制面板
│   │   ├── CustomSeekbar.kt          — 自定义进度条
│   │   ├── GestureHandler.kt         — 手势 (Compose 版，含长按动态调速)
│   │   ├── SeekIndicator.kt          — 进度指示器
│   │   ├── GestureIndicators.kt      — 手势指示器
│   │   ├── SeekbarStyle.kt           — 进度条样式
│   │   └── SeekbarThumbnailPreview.kt — 进度条缩略图预览
│   ├── theme/                     — 5 个主题文件
│   │   ├── Theme.kt               — 主题定义
│   │   ├── AppTheme.kt            — 应用主题枚举
│   │   ├── ThemeController.kt     — 主题控制器
│   │   ├── Spacing.kt             — 间距定义
│   │   └── Type.kt                — 字体排版
│   ├── webdav/                    — WebDAV 页面
│   └── viewmodels/                — UI 层 ViewModel
│
├── player/                        — mpv 播放器 View
│   ├── CustomMPVView.kt
│   ├── DoubleTapSeekIndicator.kt
│   └── VideoAspect.kt
│
├── danmaku/                       — 弹幕 View + 下载
│   ├── DanmakuPlayerView.kt
│   ├── DanmakuConfig.kt
│   └── BiliBiliDanmakuDownloadManager.kt
│
├── bilibili/                      — B 站 API（4 个子目录）
│   ├── api/
│   │   └── BiliBangumiApi.kt     — 番剧 API（索引、详情、播放地址、搜索）
│   ├── auth/
│   │   └── BiliBiliAuthManager.kt — 认证管理（二维码登录、Cookie）
│   ├── model/
│   │   ├── ApiModels.kt          — 通用 API 模型
│   │   ├── BangumiDetail.kt      — 番剧详情模型（简化版）
│   │   ├── BangumiIndexModels.kt — 番剧索引模型（条件/结果/详情/播放地址）
│   │   └── BiliBiliModels.kt     — B站核心模型（用户/剧集/播放地址）
│   └── repository/
│       └── BangumiRepository.kt  — 番剧数据仓库
│
├── tvbox/                         — TV Box 模块（6 个子目录）
│   ├── config/
│   │   ├── ConfigParser.kt       — 配置解析器
│   │   └── TvBoxConfigManager.kt — TV Box 配置管理
│   ├── crawler/
│   │   └── JarLoader.java        — JAR 加载器（Java）
│   ├── model/
│   │   ├── ParseBean.kt          — 解析结果模型
│   │   ├── SourceBean.kt         — 数据源模型
│   │   └── VodInfo.kt            — 点播信息模型
│   ├── repository/
│   │   └── TvBoxRepository.kt    — TV Box 数据仓库
│   ├── server/
│   │   └── TvBoxProxyServer.kt   — HTTP 代理服务器
│   ├── ui/
│   │   ├── TvBoxSearchContainerScreen.kt — 搜索容器页面
│   │   ├── TvBoxSearchScreen.kt  — 搜索页面
│   │   └── VodDetailDialog.kt    — 点播详情对话框
│   └── viewmodel/
│       └── TvBoxSearchViewModel.kt — 搜索 ViewModel
│
├── dandanplay/                    — DanDanPlay API
│   ├── DanDanPlayApi.kt           — 弹幕 API 客户端（支持自定义 baseUrl）
│   ├── DanDanPlayModels.kt        — 数据模型（含 ServerMatchResult）
│   └── DanmakuServer.kt           — 弹幕服务器配置模型（多服务器管理）
│
├── remote/                        — 远程播放 (5 文件)
│   ├── RemotePlaybackLauncher.kt  — 远程播放启动器
│   ├── RemotePlaybackResolver.kt  — 远程播放解析器
│   ├── RemotePlaybackRequest.kt   — 远程播放请求模型
│   ├── RemotePlaybackHeaders.kt   — 远程播放请求头
│   └── RemoteUrlParser.kt         — URL 解析
│
├── download/                      — 下载管理
│   ├── BilibiliDownloadManager.kt
│   ├── BilibiliDownloadViewModel.kt
│   ├── DownloadExampleActivity.kt
│   ├── DownloadItem.kt
│   └── DownloadTaskStore.kt
│
├── subtitle/                      — 字幕下载
│   └── ... 
│
├── sniffer/                       — 视频嗅探 (3 文件)
│   ├── VideoSnifferManager.kt
│   ├── UrlDetector.kt
│   └── DetectedVideo.kt
│
├── mediainfo/                     — 媒体信息
├── paging/                        — 分页加载
├── service/                       — 后台 Service
│   └── BackgroundPlaybackService.kt
├── worker/                        — WorkManager 后台任务
├── utils/                         — 14 个工具类
│   ├── UpdateManager.kt           — 更新检查
│   ├── ThumbnailCacheManager.kt   — 缩略图缓存
│   ├── DeviceInfoDetector.kt      — 设备信息检测（HDR/杜比视界能力）
│   ├── CrashHandler.kt            — 崩溃捕获
│   ├── FileOperationManager.kt    — 文件操作
│   ├── UriUtils.kt  FormatUtils.kt  Logger.kt
│   ├── DialogUtils.kt  CookieManager.kt
│   ├── MediaInfoHelper.kt  ScanFilter.kt
│   ├── NoMediaChecker.kt  SecureStorage.kt
│   └── media/                     — 媒体库相关 (4 文件)
│       ├── MediaLibraryEvents.kt  — 媒体库事件
│       ├── MediaScanReceiver.kt   — 媒体扫描广播接收器
│       ├── NoMediaPathFilter.kt   — .nomedia 路径过滤
│       └── TreeViewScanner.kt     — 树状视图扫描器
├── tv/                            — TV 模式
└── data/                          — 数据模型
    ├── model/WebDavAccount.kt
    └── preferences/WebDavAccountDataSource.kt
```
