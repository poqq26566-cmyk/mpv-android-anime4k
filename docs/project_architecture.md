# FAM4K007 项目架构与技术栈文档

> 最后更新: 2026-05-22  
> 版本: 1.2.5

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
10. [构建与配置](#10-构建与配置)
11. [包结构总览](#11-包结构总览)

---

## 1. 项目概述

FAM4K007 是一款基于 **mpv** 播放引擎的 Android 视频播放器，专注于：

- **高性能播放**：基于 mpv 原生播放器，支持硬件解码
- **Anime4K 实时画质增强**：集成 Anime4K GLSL 着色器
- **弹幕支持**：集成 Bilibili/DanDanPlay 弹幕系统
- **字幕管理**：支持多种字幕格式（ASS/SRT/VTT）和在线搜索
- **多协议支持**：本地文件、WebDAV、HTTP/HTTPS/RTSP 远程播放
- **B 站生态**：Bilibili API 集成（番剧信息、弹幕下载、二维码登录）
- **下载功能**：Bilibili 视频下载管理器

---

## 2. 整体架构

采用 **基于 Manager 模式的半 MVVM 分层架构**，共 6 层：

```
┌──────────────────────────────────────────────────┐
 │                    UI 层                          │
 │   ┌──────────────────────┬────────────────────┐   │
 │   │  传统 View (XML)      │   Compose Screens   │   │
 │   │  VideoPlayerActivity   │  HomeScreen         │   │
 │   │  CustomMPVView         │  SettingsScreen     │   │
 │   │  DanmakuPlayerView     │  ...                │   │
 │   └──────────────────────┴────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │              Presentation 层                      │
 │   ┌──────────────────────────────────────────┐   │
 │   │  ViewModel (StateFlow / SharedFlow)       │   │
 │   │  PlayerViewModel                          │   │
 │   │  LibraryViewModel                         │   │
 │   │  SettingsViewModel                        │   │
 │   │  ...                                      │   │
 │   └──────────────────────────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │               Domain 层                           │
 │   ┌──────────────────────────────────────────┐   │
 │   │  Manager / Engine 模式                     │   │
 │   │  PlaybackEngine      PlayerControlsManager│   │
 │   │  DanmakuManager      GestureHandler       │   │
 │   │  Anime4KManager      SeriesManager        │   │
 │   │  SubtitleManager     FilePickerManager    │   │
 │   │  PlayerDialogManager ...                  │   │
 │   └──────────────────────────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │              Repository 层                        │
 │   ┌──────────────────────────────────────────┐   │
 │   │  PlayerRepository   VideoRepository       │   │
 │   │  BilibiliRepository WebDavRepository      │   │
 │   │  SubtitleRepository MediaInfoRepository   │   │
 │   └──────────────────────────────────────────┘   │
 ├──────────────────────────────────────────────────┤
 │                Data 层                            │
 │   ┌──────────┬──────────┬──────────┬──────────┐  │
 │   │  Room DB │ SharedPref│ 网络 API │ 本地 AAR │  │
 │   └──────────┴──────────┴──────────┴──────────┘  │
 ├──────────────────────────────────────────────────┤
 │            DI 层 (Koin)                           │
 │   ┌──────────────────────────────────────────┐   │
 │   │ databaseModule  networkModule             │   │
 │   │ repositoryModule domainModule             │   │
 │   │ presentationModule                        │   │
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
| `CustomMPVView` | mpv 播放器 Surface 封装，配置解码/字幕/音视频参数 |
| `DanmakuPlayerView` | 弹幕渲染 View |
| `CustomSeekbar` | 自定义播放进度条 |
| `DoubleTapSeekIndicator` | 双击快进指示器 |

#### Compose 子系统

| 目录 | 职责 |
|------|------|
| `ui/screens/` | 所有全屏页面（Home、Settings、Library 等） |
| `ui/components/` | 可复用 Compose 组件（弹窗、卡片、TopAppBar 等） |
| `ui/player/` | 播放器控制面板 Compose（PlayerControls） |
| `ui/theme/` | 主题（亮/暗/跟随系统）、颜色、排版 |
| `ui/webdav/` | WebDAV 相关页面 |
| `ui/viewmodels/` | UI 层 ViewModel（Compose 页面专用） |
| `manager/compose/` | Compose 弹窗抽屉（弹幕设置、字幕设置、视频列表等） |

### 4.2 Presentation 层

**位置**: `presentation/`

| ViewModel | 职责 |
|-----------|------|
| `PlayerViewModel` | 播放器核心状态（播放/暂停、进度、速度、解码、弹幕、字幕等） |
| `LibraryViewModel` | 视频库列表、扫描 |
| `SettingsViewModel` | 应用设置 |
| `PlaybackSettingsViewModel` | 播放相关设置 |
| `BilibiliViewModel` | B 站视频信息、弹幕列表 |
| `BiliBiliPlayViewModel` | B 站番剧播放 |
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
| `GestureHandler` | 手势识别（滑动/双击/长按/拖动） | Activity 绑定 |
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
- 弹幕设置（大小、速度、透明度、行数限制）
- 字幕设置（延迟、缩放、颜色、描边）
- 黑名单、更新设置、手势配置等

#### 网络 API

- **Bilibili API**: 番剧信息、视频流解析、二维码登录
- **DanDanPlay API**: 弹幕搜索与下载
- **WebDAV**: 远程文件管理

### 4.6 DI 层

**位置**: `di/`

Koin 5 个模块：

```kotlin
val appModules = listOf(
    databaseModule,      // Room 数据库、DAO 单例
    networkModule,       // BiliBiliAuthManager、DanDanPlayApi
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

## 10. 构建与配置

### 版本号方案

```
versionCode = baseVersion * 10 + abiCode

abiCode:
  arm64-v8a  → 2
  armeabi-v7a → 1
```

当前: `versionCode = 24`, `versionName = "1.2.5"`

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

## 11. 包结构总览

```
com.fam4k007.videoplayer/
├── di/                          — Koin 依赖注入
│   ├── AppModule.kt             — 模块入口
│   ├── DatabaseModule.kt        — Room 数据库
│   ├── NetworkModule.kt         — 网络服务
│   ├── RepositoryModule.kt      — 数据仓库
│   ├── DomainModule.kt          — 领域服务
│   └── PresentationModule.kt    — ViewModel
│
├── presentation/                — ViewModel
│   ├── PlayerViewModel.kt       — 播放器核心状态
│   ├── LibraryViewModel.kt      — 视频库
│   ├── SettingsViewModel.kt     — 设置
│   ├── BilibiliViewModel.kt     — B站
│   ├── WebDavViewModel.kt       — WebDAV
│   ├── PlaybackHistoryViewModel.kt
│   ├── SubtitleSearchViewModel.kt
│   └── ...
│
├── domain/                      — 业务逻辑
│   ├── player/                  — 播放相关
│   │   ├── PlaybackEngine.kt
│   │   ├── PlayerControlsManager.kt
│   │   ├── PlayerDialogManager.kt
│   │   ├── GestureHandler.kt
│   │   ├── Anime4KManager.kt
│   │   ├── SeriesManager.kt
│   │   └── FilePickerManager.kt
│   ├── danmaku/                 — 弹幕
│   │   └── DanmakuManager.kt
│   ├── subtitle/                — 字幕
│   │   └── SubtitleManager.kt
│   ├── sniffer/                 — 串流嗅探
│   │   └── VideoSelector.kt
│   └── webdav/                  — WebDAV
│       ├── WebDavClient.kt
│       └── WebDavConfig.kt
│
├── repository/                  — 数据仓库
│   ├── PlayerRepository.kt
│   ├── VideoRepository.kt
│   ├── BilibiliRepository.kt
│   ├── WebDavRepository.kt
│   ├── SubtitleRepository.kt
│   └── MediaInfoRepository.kt
│
├── database/                    — Room 数据库
│   ├── VideoDatabase.kt
│   ├── VideoCacheEntity.kt
│   ├── VideoCacheDao.kt
│   ├── PlaybackHistoryEntity.kt
│   └── PlaybackHistoryDao.kt
│
├── preferences/                 — 设置管理
│   └── PreferencesManager.kt
│
├── manager/                     — 管理器
│   ├── ScreenshotManager.kt
│   ├── SkipIntroOutroManager.kt
│   ├── ThemeManager.kt
│   └── compose/                 — Compose 弹窗抽屉
│       ├── ComposeOverlayManager.kt
│       ├── DanmakuDialogs.kt
│       ├── DanmakuFilePickerDialog.kt
│       ├── SkipSettingsDrawer.kt
│       ├── SubtitleDialogs.kt
│       ├── SubtitleFilePickerDialog.kt
│       └── VideoListDrawer.kt
│
├── navigation/                  — 导航
│   ├── AppNavGraph.kt
│   └── AppScreen.kt
│
├── ui/                          — Compose UI
│   ├── screens/                 — 全屏页面
│   ├── components/              — 可复用组件
│   ├── player/                  — 播放器控制面板
│   ├── theme/                   — 主题
│   ├── webdav/                  — WebDAV 页面
│   └── viewmodels/              — 页面专用 ViewModel
│
├── player/                      — mpv 播放器 View
│   ├── CustomMPVView.kt
│   ├── DoubleTapSeekIndicator.kt
│   └── VideoAspect.kt
│
├── danmaku/                     — 弹幕 View
│   ├── DanmakuPlayerView.kt
│   ├── DanmakuConfig.kt
│   └── BiliBiliDanmakuDownloadManager.kt
│
├── bilibili/                    — B 站 API
│   ├── auth/
│   └── model/
│
├── dandanplay/                  — DanDanPlay API
│   ├── DanDanPlayApi.kt
│   └── DanDanPlayModels.kt
│
├── remote/                      — 远程播放
│   ├── RemotePlaybackLauncher.kt
│   ├── RemotePlaybackResolver.kt
│   ├── RemotePlaybackRequest.kt
│   └── RemoteUrlParser.kt
│
├── download/                    — 下载管理
│   ├── BilibiliDownloadManager.kt
│   ├── BilibiliDownloadViewModel.kt
│   ├── DownloadTaskStore.kt
│   └── ...
│
├── subtitle/                    — 字幕下载
│   ├── SubtitleDownloadManager.kt
│   └── SubtitleModels.kt
│
├── sniffer/                     — 视频嗅探
│   ├── VideoSnifferManager.kt
│   ├── UrlDetector.kt
│   └── DetectedVideo.kt
│
├── mediainfo/                   — 媒体信息
├── paging/                      — 分页
├── data/                        — 数据模型
├── worker/                      — WorkManager 后台任务
├── utils/                       — 工具类
└── tv/                          — TV 模式
```
