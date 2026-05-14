# 架构迁移状态

> 对照目标架构（Clean Architecture + MVVM），分析当前实现情况

## 目标架构要求

```
UI (Compose Screen)
    ↓
Presentation (ViewModel)
    ↓
Domain (Business Logic)
    ↓
Repository (Data Abstraction)
    ↓
Data (Room/Network/Prefs)
```

---

## 已符合架构 ✅

这些页面已实现完整分层：

| 页面 | Activity | ViewModel | Screen | 状态 |
|---|---|---|---|---|
| 下载管理 | DownloadActivity | BilibiliDownloadViewModel | DownloadScreen | ✅ 完整（已重构） |
| 设置 | SettingsComposeActivity | SettingsViewModel | SettingsScreen | ✅ 完整 |
| TV浏览 | TVBrowserActivity | TVBrowserViewModel | TVBrowserScreen | ✅ 完整 |
| 媒体库 | VideoBrowserComposeActivity | LibraryViewModel | FolderBrowserScreen | ✅ 完整 |
| 视频列表 | VideoListComposeActivity | (LibraryViewModel) | VideoListScreen | ✅ 完整 |
| B站播放 | BiliBiliPlayActivity | BiliBiliPlayViewModel | BiliBiliPlayScreen | ✅ 完整 |
| 字幕搜索 | SubtitleSearchActivity | SubtitleSearchViewModel | SubtitleSearchScreen | ✅ 完整 |
| B站弹幕下载 | BiliBiliDanmakuComposeActivity | BiliBiliDanmakuViewModel | BiliBiliDanmakuScreen | ✅ 完整 |
| 播放历史 | PlaybackHistoryComposeActivity | PlaybackHistoryViewModel | PlaybackHistoryScreen | ✅ 完整（2026-05-14 新重构） |
| 播放设置 | PlaybackSettingsComposeActivity | PlaybackSettingsViewModel | PlaybackSettingsScreen | ✅ 完整（2026-05-14 新重构） |
| 媒体信息 | MediaInfoActivity | MediaInfoViewModel | MediaInfoScreen | ✅ 完整（2026-05-14 新重构） |
| 主页 | MainActivity | - | HomeScreen | ✅ 简单页面 |
| 关于 | AboutComposeActivity | - | AboutScreen | ✅ 简单页面 |
| 开源许可 | LicenseActivity | - | LicenseScreen | ✅ 简单页面 |
| 用户协议 | UserAgreementActivity | - | UserAgreementScreen | ✅ 简单页面 |
| 反馈 | FeedbackActivity | - | FeedbackScreen | ✅ 简单页面 |

**最近重构：**
- **媒体信息**（2026-05-14）：完成架构迁移，创建MediaInfoViewModel和MediaInfoRepository，通过Repository封装MediaInfoHelper调用，所有UI状态管理从Screen移至ViewModel，符合Clean Architecture + MVVM架构规范。
- **播放设置**（2026-05-14）：完成架构迁移，创建PlaybackSettingsViewModel，通过PlayerRepository访问PreferencesManager，所有UI状态管理从Screen移至ViewModel，符合Clean Architecture + MVVM架构规范。
- **播放历史**（2026-05-14）：完成架构迁移，创建PlaybackHistoryViewModel，通过PlayerRepository访问数据，状态管理从Activity移至ViewModel，符合Clean Architecture + MVVM架构规范。
- **下载管理**（2026-05-11）：已完全重构，所有解析逻辑、状态管理（isParsing、parseError、parseResult、episodeList等）从UI层移至ViewModel，符合MVVM架构规范。

---



## 📊 剩余工作全景分析

### 当前进度总览

```
目标架构：Clean Architecture + MVVM + 全Compose + Koin + Navigation3
当前状态：阶段2（代码分层）
- **已完成**: 16/17 页面 ✅
- **需要迁移**: 1 🔄
- **进度**: 84% → 100%（还需16%）
```

---

## 🎯 剩余工作清单（按优先级）

### 📌 **一、页面层迁移（1个待迁移）** - 优先级：🔥🔥🔥

#### 1. **VideoPlayerActivity** - 最重要！
- **工作量**：⏱️ 10-15天
- **当前状态**：
  - ❌ 使用XML布局（`activity_video_player.xml`）
  - ❌ 2200+行代码，职责过重
  - ❌ 虽有PlayerViewModel但未充分使用
  - ❌ 业务逻辑分散在多个Manager中
- **需要完成**：
  1. 创建完整的 `PlayerViewModel`（整合所有Manager状态）
  2. 迁移到 Compose UI（`PlayerScreen.kt`）
  3. 将手势处理、控制逻辑移到Domain/ViewModel
  4. 重构弹幕同步逻辑
  5. 删除30+个XML对话框，改为Compose Dialog

---

### 📌 **二、UI全Compose化** - 优先级：🔥🔥

#### 当前残留XML布局（需清理）
```
✅ 16个页面已Compose化
❌ VideoPlayerActivity仍使用XML
❌ 30+个XML对话框需迁移为Compose Dialog：
   - dialog_more.xml
   - dialog_decoder.xml
   - dialog_audio_track.xml
   - dialog_chapter_item.xml
   - dialog_danmaku_settings.xml
   - dialog_anime4k_mode.xml
   - ... (共30+个)
```

**需要完成**：
1. VideoPlayerActivity 改为 Compose
2. 所有XML Dialog 改为 Compose Dialog
3. 删除 `res/layout/` 中的旧布局文件

---

### 📌 **三、Navigation3迁移** - 优先级：🔥

#### 当前状态：
- ❌ 使用传统Intent跳转
- ❌ 参数传递不类型安全

**需要完成**：
1. 定义 Screen sealed interface + @Serializable
2. MainActivity配置NavBackStack
3. 改造所有跳转逻辑（Intent → Navigation）
4. 参数传递类型安全化

**工作量**：⏱️ 2-3天

---

### 📌 **四、Repository/Domain层完善** - 优先级：🔥

#### 当前状态：
- ✅ 已有部分Repository（VideoRepository、BilibiliRepository等）
- 🔄 Domain层逻辑分散

**需要完成（从 architecture_redesign_plan.md）**：
1. **Repository层补全**：
   - ✅ VideoRepository（已完成）
   - ✅ BilibiliRepository（已完成）
   - ✅ WebDavRepository（已完成）
   - ✅ MediaInfoRepository（已完成，2026-05-14）
   - ⚠️ PlayerRepository（可能需补充）
   - ⚠️ SubtitleRepository（可能需补充）

2. **Domain层整理**（核心业务逻辑集中）：
   - PlaybackEngine（播放控制）
   - GestureHandler（手势算法）
   - Anime4KManager（Shader配置）
   - DanmakuManager（弹幕同步）
   - SubtitleManager（字幕处理）

**工作量**：⏱️ 3-5天

---

### 📌 **五、主题系统升级** - 优先级：🔥（可选）

#### 当前状态：
- ✅ 基础Material 3主题
- ❌ 缺少多主题支持

**需要完成（参考mpvEx）**：
1. 创建Spacing系统（统一间距标准）
2. 多主题支持（Default、Mocha、Ocean、Forest等）
3. 主题切换动画（可选）
4. Dynamic Color支持（Android 12+）

**工作量**：⏱️ 2-3天

---

### 📌 **六、工具类整理** - 优先级：低

**需要完成**：
1. utils/ 精简（删除被Compose替代的工具类）
2. preferences/ 统一管理（AppPreferences、PlayerPreferences）
3. 删除DialogUtils等被Compose替代的工具

**工作量**：⏱️ 1天

---

### 📌 **七、测试和优化** - 优先级：持续

**需要完成**：
1. 单元测试（Repository、ViewModel、核心算法）
2. 功能验证（播放器、B站、WebDAV、本地库、设置）
3. 性能优化（Compose重组、内存泄漏）

**工作量**：⏱️ 持续进行

---

## 📈 剩余工作量估算

| 工作项 | 工作量 | 优先级 |
|--------|--------|--------|
| VideoPlayerActivity重构 | 10-15天 | 🔥🔥🔥 |
| XML Dialog → Compose | 3-5天 | 🔥🔥 |
| Navigation3迁移 | 2-3天 | 🔥 |
| Repository/Domain完善 | 3-5天 | 🔥 |
| 主题系统升级 | 2-3天 | 🔥（可选） |
| 工具类整理 | 1天 | 低 |
| 测试优化 | 持续 | 持续 |
| **总计** | **~21-35天** | - |

---
2个简单页面迁移 | 1.5-2天 | 🔥🔥 |
| XML Dialog → Compose | 3-5天 | 🔥🔥 |
| Navigation3迁移 | 2-3天 | 🔥 |
| Repository/Domain完善 | 3-5天 | 🔥 |
| 主题系统升级 | 2-3天 | 🔥（可选） |
| 工具类整理 | 1天 | 低 |
| 测试优化 | 持续Settings（0.5-1天）      ← 简单
   ↓
3. Repository/Domain完善（3-5天）   ← 为核心播放器打基础
   ↓
4. VideoPlayerActivity（10-15天）   ← 🔥 最重要！
   ├─ ViewModel完善
   ├─ UI Compose化
   ├─ XML Dialog迁移
   └─ 功能验证
   ↓
5. Navigation3迁移（2-3天）         ← 统一导航
   ↓
6. 主题系统（2-3天，可选）          ← 提升体验
   ↓
7. 工具类整理（1天）                ← 清理
   ↓
8. 测试优化（持续）                 ← 保证质量
```

---

## ✅ 已完成的工作（84%）

1. ✅ Koin依赖注入引入
2. ✅ 16个页面架构迁移（MVVM + Compose）
3. ✅ Repository层创建（Video、Bilibili、WebDav、Player、MediaInfo）
4. ✅ Version Catalog依赖管理
5. ✅ 基础Material 3主题
6. ✅ PlaybackHistoryViewModel创建并集成（2026-05-14）
7. ✅ PlaybackSettingsViewModel创建并集成（2026-05-14）
8. ✅ MediaInfoViewModel创建并集成（2026-05-14）

## 🎯 总结

**核心关键**：VideoPlayerActivity（2200+行）是最大的挑战  
**剩余工作**：约21-35天工作量  
**进度**：84% → 100%（还需16%）  
**建议**：优先完成VideoPlayerActivity迁移，这是最后也是最重要的页面
**核心业务代码位置**（已梳理）：
- 视频下载：`download/BilibiliDownloadManager.kt`
- 弹幕下载：`danmaku/BiliBiliDanmakuDownloadManager.kt`
- B站播放：`bilibili/auth/BiliBiliAuthManager.kt` + `repository/BilibiliRepository.kt`
- 播放器：`player/` 目录
- 字幕搜索：`subtitle/` 目录
- 数据库：`database/` 目录