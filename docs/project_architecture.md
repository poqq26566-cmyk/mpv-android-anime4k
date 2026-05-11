您的项目采用 **Clean Architecture + MVVM** 架构模式，具体结构如下：

## 架构层次（5层）

```
UI Layer (Compose)
    ↓
Presentation Layer (ViewModel)
    ↓
Domain Layer (Business Logic)
    ↓
Repository Layer (Data Abstraction)
    ↓
Data Layer (Room/Network/Prefs)
```

## 包结构

```
com.fam4k007.videoplayer/
├── database/          # Room数据库 + Dao + Entity
├── repository/        # 数据访问抽象层（VideoRepository, WebDavRepository等）
├── domain/            # 核心业务逻辑（保护区）
├── presentation/      # ViewModels（状态管理）
├── ui/                # Compose界面 + 组件
├── di/                # Koin依赖注入模块
├── preferences/       # DataStore配置存储
└── utils/             # 工具类
```

## 技术栈

- **依赖注入**: Koin（轻量级，无注解处理）
- **UI框架**: Jetpack Compose（全Compose化）
- **设计系统**: Material 3
- **导航**: Navigation3 + Kotlinx Serialization（类型安全）
- **构建系统**: Kotlin DSL + Version Catalog
- **数据库**: Room
- **网络**: OkHttp + Sardine（WebDAV）

## 当前状态

正处于 **Stage 2（代码分层）** 阶段，已完成：
- ✅ Koin依赖注入引入
- ✅ Repository层创建
- ✅ 部分ViewModel创建（LibraryViewModel等）
- 🔄 正在完善剩余ViewModels
- 🔄 UI层向完全架构合规迁移

核心原则：**"100%保留逻辑，只改调用方式"** - 像房屋翻新，保留优质材料（核心算法、业务逻辑），只重新设计结构和布局。