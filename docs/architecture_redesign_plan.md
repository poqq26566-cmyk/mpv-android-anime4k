# FAM4K007 架构重构方案

> **核心理念**：像装修房子一样——保留所有优质材料（核心算法、业务逻辑），只重新设计结构和布局  
> **参考项目**：mpvEx-master（学习其现代化架构实践）

**日期**：2026-05-11  
**目标**：Clean Architecture + Koin + 全Compose + Navigation3 + Version Catalog

---

## 一、为什么要重构？

### 当前痛点
- **VideoPlayerActivity 2200+行** - 职责过重，难以维护
- **UI技术混乱** - XML + Compose混用
- **依赖管理混乱** - 手动单例，无法测试
- **构建配置落后** - Groovy DSL，依赖版本分散
- **代码组织混乱** - 无清晰分层，耦合严重

### 优质资产（必须100%保留）
✅ 播放器核心（MPV封装、手势处理、进度控制）  
✅ Anime4K算法（shader配置）  
✅ 弹幕系统（同步逻辑）  
✅ B站功能（登录、解析、API调用）  
✅ WebDAV实现（Sardine封装）  
✅ 数据库（所有Migration策略）  
✅ 工具类（Logger、FormatUtils等）

---

## 二、新架构方向（学习mpvEx）

### 核心技术选型

| 技术点 | 当前 | 新方向 | 原因 |
|--------|------|--------|------|
| 构建系统 | Groovy DSL | **Kotlin DSL** | 类型安全，IDE支持更好 |
| 依赖管理 | 分散 | **Version Catalog** (libs.versions.toml) | 统一管理，避免冲突 |
| 依赖注入 | 手动单例 | **Koin** | 轻量、简单、适合中型项目 |
| 导航 | Intent | **Navigation3 + Serialization** | 类型安全 |
| UI | XML+Compose | **全Compose** | 统一技术栈 |
| 架构模式 | MV? | **MVVM + Clean Architecture** | 清晰分层 |
| 主题 | 简单亮暗 | **多主题 + 动画切换** | 用户体验 |

### 目标架构分层（5层结构）

```
┌─────────────────────────────────────┐
│         UI Layer (Compose)          │  ← 纯UI展示
├─────────────────────────────────────┤
│   Presentation Layer (ViewModel)    │  ← 状态管理
├─────────────────────────────────────┤
│    Domain Layer (Business Logic)    │  ← 核心算法（保留区）
├─────────────────────────────────────┤
│  Repository Layer (Data Abstraction)│  ← 数据访问封装
├─────────────────────────────────────┤
│   Data Layer (Room/Network/Prefs)   │  ← 数据源
└─────────────────────────────────────┘

依赖方向：上层依赖下层，下层不知道上层
```

### 包结构设计（单Module + 清晰分层）

```
com.fam4k007.videoplayer/
│
├── database/               # 数据层（Room + Dao + Entity）
├── repository/             # 仓储层（统一数据访问）
├── domain/                 # 领域层（核心业务逻辑 - 保留区）
│   ├── player/            # PlaybackEngine, GestureHandler, Anime4K
│   ├── bilibili/          # BangumiParser, DanmakuLoader
│   └── subtitle/          # SubtitleManager
├── presentation/           # 展示层（ViewModel）
├── ui/                     # UI层（Compose Screen + 组件）
│   ├── theme/             # 主题系统（多主题 + Spacing）
│   ├── components/        # 可复用组件库
│   └── [feature]/         # 各功能界面
├── di/                     # 依赖注入（Koin模块）
├── preferences/            # 配置管理（DataStore封装）
└── utils/                  # 工具类
```

---

## 三、迁移路线图（渐进式，每步可验证）

### 📋 阶段 0：现代化构建（1-2天）
**方向**：升级构建系统和依赖管理
- 迁移 Groovy → Kotlin DSL (.kts)
- 创建 Version Catalog (libs.versions.toml)
- 升级核心依赖（Compose BOM、Room、OkHttp）
- 配置 Product Flavors（可选）

**验收**：项目编译通过，运行正常

---

### 🔌 阶段 1：引入Koin（1天）
**方向**：消除手动单例，统一依赖管理
- App.kt 初始化 Koin
- 创建 DI 模块（DatabaseModule、NetworkModule、RepositoryModule、DomainModule）
- 改造现有单例（只改注入方式，逻辑100%保留）

**验收**：所有单例改为Koin注入，功能正常

---

### 📦 阶段 2：代码分层（3-5天）
**方向**：按职责重组代码，保留所有逻辑

#### 2.1 database/ 整理
✅ 已基本完成，确保所有Entity、Dao、Migration在位

#### 2.2 repository/ 创建
**方向**：把分散的数据访问封装成Repository
- VideoRepository（视频扫描、数据库操作）
- PlayerRepository（播放历史、设置）
- BilibiliRepository（API调用、数据解析）
- WebDavRepository（文件操作）

#### 2.3 domain/ 整理
**方向**：核心业务逻辑集中到domain，这是**保留区**
- PlaybackEngine（播放控制） → 位置变化，逻辑不动
- GestureHandler（手势算法） → 算法100%保留
- Anime4KManager（Shader配置） → 配置保留
- DanmakuManager（弹幕同步） → 逻辑保留
- SubtitleManager（字幕处理） → 逻辑保留

#### 2.4 presentation/ 创建（ViewModel）
**方向**：把Activity中的状态管理抽取到ViewModel
- PlayerViewModel（调用domain层，不含业务逻辑）
- LibraryViewModel
- BilibiliViewModel
- SettingsViewModel

**验收**：每层职责清晰，编译通过

---

### 🎨 阶段 3：UI全Compose化 + 主题升级（4-6天）
**方向**：统一UI技术栈，提升用户体验

#### 3.1 主题系统升级（参考mpvEx）
- 创建 Spacing 系统（统一间距标准）
- 多主题支持（Default、Mocha、Ocean、Forest等）
- 主题切换动画（可选）
- Dynamic Color 支持（Android 12+）

#### 3.2 UI组件库
创建可复用Compose组件：
- ExpandableCard、SliderItem、ConfirmDialog等

#### 3.3 Screen界面重构
**方向**：Activity → Compose Screen，逻辑在ViewModel
- PlayerScreen（最复杂）
- LibraryScreen
- BilibiliScreen
- WebDavScreen
- SettingsScreen

**策略**：从简单的开始（Settings、Library），核心逻辑已在ViewModel和domain层

**验收**：每个Screen功能与原Activity一致

---

### 🧭 阶段 4：Navigation3迁移（2-3天）
**方向**：类型安全的导航

- 定义 Screen 接口（sealed interface + @Serializable）
- MainActivity 配置 NavBackStack
- 改造跳转逻辑（Intent → Navigation）

**验收**：所有页面跳转正常，支持参数传递

---

### 🔧 阶段 5：工具类和配置整理（1-2天）
**方向**：精简和标准化

- utils/ 保留核心工具类（Logger、FormatUtils、UriUtils）
- preferences/ 统一管理（AppPreferences、PlayerPreferences）
- 删除不需要的工具类（如DialogUtils → Compose组件）

---

### 🧪 阶段 6：测试和优化（持续）
**方向**：保证质量

- 单元测试（Repository、ViewModel、核心算法）
- 功能验证（播放器、B站、WebDAV、本地库、设置）
- 性能优化（Compose重组、内存泄漏）

---

## 四、核心原则（必须遵守）

### ✅ 代码保留原则
1. **100%保留**：
   - 所有核心算法（GestureHandler、Anime4K、弹幕同步等）
   - 所有业务逻辑（播放控制、解析规则、文件操作）
   - 所有数据结构（Entity、Dao、Migration）

2. **只改调用方式**：
   - 依赖获取：getInstance() → Koin注入
   - 状态管理：Activity变量 → ViewModel StateFlow
   - UI实现：XML → Compose
   - 导航：Intent → Navigation3

3. **可以删除**：
   - XML布局文件
   - findViewById代码
   - Handler/Runnable（改用Coroutines）
   - 回调接口（改用Flow/suspend）

### 🎯 迁移原则
- **渐进式**：每步保持可编译、可运行
- **保留逻辑**：算法一行不改，只改位置和调用
- **新旧并存**：迁移期间新旧代码共存
- **充分验证**：每个功能迁移后必须测试

### 🛡️ 风险控制
- 每阶段Git提交，可随时回滚
- 保持在当前分支操作
- 每阶段完成打tag（如 v2.0-stage1）

---

## 五、预期收益

### 代码质量
✅ 架构清晰 - 5层分离，职责明确  
✅ 易于维护 - 修改影响范围小  
✅ 易于测试 - 依赖注入，可模拟  
✅ 代码量减少 - Compose + Kotlin特性（预计减少20-30%）

### 开发体验
✅ 编译更快 - Koin无注解处理器  
✅ 热重载 - Compose支持  
✅ 类型安全 - Navigation3、Version Catalog  
✅ IDE支持更好 - Kotlin DSL

### 用户体验
✅ 统一UI风格 - 全Compose  
✅ 多主题选择 - 个性化  
✅ 流畅动画 - Material 3  

---

## 六、从哪里开始？

### 第一步：阶段0 - 现代化构建
1. 创建 `gradle/libs.versions.toml`
2. 迁移 `build.gradle` → `build.gradle.kts`
3. 迁移 `settings.gradle` → `settings.gradle.kts`
4. 升级依赖版本
5. 验证编译

**预计时间**：半天到1天

---

## 七、参考资源

### mpvEx 值得学习的点
- ✅ Kotlin DSL + Version Catalog 配置
- ✅ Koin 依赖注入的简洁使用
- ✅ 清晰的包结构分层
- ✅ 多主题系统实现
- ✅ Spacing 系统设计
- ✅ Navigation3 类型安全导航
- ✅ UI组件库的组织方式

### 关键技术文档
- Koin: https://insert-koin.io/
- Compose: https://developer.android.com/jetpack/compose
- Navigation3: androidx.navigation3
- Version Catalog: https://docs.gradle.org/current/userguide/platforms.html

---

**记住**：这不是重写项目，而是重新组织项目。就像装修房子——材料（代码逻辑）是好的，只是需要重新设计布局（架构）。
