# 技术栈

## 项目类型
CH Music 是一个多项目音乐系统仓库，包含三个主要工程：

- **MusicPlayer**：Electron 桌面应用，同时保留 Web 调试/构建能力。
- **AndroidMusicPlayer**：原生 Android 音乐播放器应用，基于 Kotlin、Java、Gradle 和 AndroidX。
- **MusicServer**：Java/Spring Boot REST API 服务，作为私有音乐库后端。

整体架构是“多客户端 + 单一私有后端”的模块化单体组合。客户端保留各自现有播放能力，并通过 MusicServer 的 HTTP API 获取私有账号、音乐、歌单和收藏能力。

## 核心技术

### 主要语言
- **MusicPlayer**：TypeScript、Vue 单文件组件、少量 JavaScript。
- **AndroidMusicPlayer**：Kotlin、Java、XML Android 资源。
- **MusicServer**：Java 17。

### 运行时与编译工具
- **MusicPlayer**：Electron 40、Node.js、Vite、electron-vite、electron-builder。
- **AndroidMusicPlayer**：Android Gradle Plugin 8.13.0、Kotlin 2.1.21、KSP、Java/Kotlin JVM target 21。
- **MusicServer**：JVM、Maven Wrapper、Spring Boot Maven Plugin。

### 关键依赖和框架
#### MusicPlayer
- **Vue 3**：渲染进程 UI。
- **Pinia**：客户端状态管理。
- **Naive UI**：主要桌面 UI 组件库。
- **Tailwind CSS / Sass / 全局 CSS**：样式系统。
- **vue-i18n**：多语言。
- **axios**：HTTP 请求。
- **Howler / tunajs / music-metadata**：播放、音频处理和元数据相关能力。
- **netease-cloud-music-api-alger**：本地化在线音乐 API 能力。
- **@unblockneteasemusic/server**：音乐资源解析能力。
- **electron-store / localStorage**：本地配置和客户端会话状态。

#### AndroidMusicPlayer
- **AndroidX AppCompat / Core / RecyclerView / ConstraintLayout / Preference / Palette**：基础 Android UI 与系统能力。
- **Material Components**：Material/Material You 风格 UI。
- **Navigation Component**：Fragment 导航。
- **Room**：本地数据库。
- **Lifecycle ViewModel / LiveData**：界面状态和生命周期。
- **Koin**：依赖注入。
- **Retrofit 3 / OkHttp / Gson**：MusicServer 和外部服务 HTTP 接入。
- **Kotlin Coroutines**：异步任务。
- **Glide**：图片加载。
- **Media3 ExoPlayer**：播放能力补充。
- **jaudiotagger**：音频标签处理。
- **Google Play Billing / Review / Feature Delivery**：normal 构建渠道能力。
- **NanoHTTPD / Cast Framework**：普通构建中的 Chromecast 支持。

#### MusicServer
- **Spring Boot 4.1.0**：应用运行时和依赖管理。
- **Spring Web MVC**：REST API、multipart 上传和资源响应。
- **Spring Security**：Bearer Token 鉴权。
- **Spring Data JPA / Hibernate**：关系型数据访问。
- **H2**：本地开发默认文件数据库。
- **PostgreSQL Driver**：生产部署数据库选项。
- **Flyway**：迁移能力准备。
- **Jakarta Validation**：请求校验。
- **Lombok**：减少 Java 样板代码。

## 应用架构

### 总体架构
- MusicPlayer 和 AndroidMusicPlayer 是独立客户端，各自拥有完整播放、界面和本地状态能力。
- MusicServer 是共享后端，只通过 HTTP REST API 暴露账号、音乐、歌单、收藏和头像能力。
- 客户端不得依赖 MusicServer 内部实现，只能依赖 `/api` 下的接口契约。
- MusicServer 不承担所有播放器逻辑；它只负责私有曲库、用户数据和安全访问。

### MusicPlayer 架构
- **主进程**：`src/main` 负责窗口、托盘、快捷键、下载、MPRIS、远程控制、本地服务、缓存、文件访问等系统层能力。
- **预加载脚本**：`src/preload` 负责主进程和渲染进程之间的安全桥接。
- **渲染进程**：`src/renderer` 基于 Vue 3，按 `api`、`components`、`views`、`store`、`services`、`utils` 等层组织。
- **MusicServer 集成**：渲染进程通过 `src/renderer/api/musicServer.ts` 和 Pinia store 接入后端，token 存放在 localStorage。

### AndroidMusicPlayer 架构
- Android 应用以 `app` 模块为主体，`appthemehelper` 作为主题辅助模块。
- 代码按 Activity、Fragment、Adapter、Repository、Service、DB、Lyrics、Network、MusicServer 等功能目录组织。
- MusicServer 集成集中在 `code.name.monkey.retromusic.musicserver` 包中，包括 Retrofit API、session、repository、DTO 和歌曲映射。
- `normal` 和 `fdroid` flavor 需要继续保持渠道差异，避免把 Google Play/Cast 依赖泄漏到 F-Droid 构建。

### MusicServer 架构
- Spring Boot 模块化单体，按领域组织：`auth`、`user`、`music`、`playlist`、`favorite`、`security`、`config`、`api`。
- `api` 层负责 Controller 和 DTO；领域服务负责业务规则；Repository 负责持久化。
- 认证使用服务端签发的不透明 token，请求通过 `Authorization: Bearer <token>` 或流媒体 URL token 参数访问。
- 音频文件存储在文件系统，数据库只保存元数据和归属关系。

## 数据存储
- **MusicPlayer 本地状态**：electron-store、localStorage、文件缓存和下载目录。
- **AndroidMusicPlayer 本地状态**：Room、SharedPreferences、MediaStore、本地文件和 Android 系统媒体能力。
- **MusicServer 数据库**：默认 H2 文件数据库，路径为 `MusicServer/.local/musicserver-db`；生产方向预留 PostgreSQL。
- **MusicServer 文件存储**：默认 `MusicServer/.local/music-storage`，用于上传音乐和头像等文件。
- **API 数据格式**：JSON 响应、JSON 请求体、multipart form data 上传、流媒体资源响应。

## 外部集成
- **MusicServer API**：`/api/auth`、`/api/music`、`/api/playlists`、`/api/favorites`。
- **在线音乐能力**：MusicPlayer 依赖 Netease API 兼容服务和资源解析服务。
- **Android 外部服务**：LastFM、Deezer、歌词服务、Google Play、Cast 等依赖按现有代码边界维护。
- **系统集成**：Electron 托盘、桌面歌词、全局快捷键、MPRIS；Android 小组件、锁屏控制、Android Auto、媒体服务。

## 开发环境

### 构建与启动
#### MusicPlayer
- 安装依赖：`npm install`
- 桌面开发：`npm run dev`
- Web 开发：`npm run dev:web`
- 类型检查：`npm run typecheck`
- 打包：`npm run build:win`、`npm run build:mac`、`npm run build:linux`

#### AndroidMusicPlayer
- 使用 Gradle Wrapper 构建。
- 模块：`:app`、`:appthemehelper`。
- flavor：`normal`、`fdroid`。
- MusicServer 地址通过 Gradle 属性 `MUSIC_SERVER_BASE_URL` 配置；debug 默认 `http://10.0.2.2:8080`。

#### MusicServer
- 本地启动：`MAVEN_USER_HOME=.m2 ./mvnw -s .mvn/local-settings.xml spring-boot:run`
- 默认端口：`8080`
- 默认上传限制：单文件 500MB，请求 520MB。

### 代码质量工具
- **MusicPlayer**：ESLint、Prettier、vue-tsc、TypeScript、i18n 检查、commitlint、husky、lint-staged。
- **AndroidMusicPlayer**：Android Lint、Kotlin 官方代码风格、Gradle 构建检查、ProGuard/R8 配置。
- **MusicServer**：Java 编译器、Spring Boot test starter、JUnit、Spring Security/JPA/WebMVC 测试依赖。

### 版本控制与协作
- 使用 Git 管理多项目代码。
- 改动应尽量落在对应项目边界内；跨项目功能必须先明确 API 契约。
- 对 MusicServer API 的行为变更，应同步更新 `.spec-workflow` 规格文档和客户端类型/DTO。

## 部署与分发
- **MusicPlayer**：通过 electron-builder 输出 Windows、macOS、Linux 安装包或可执行包。
- **AndroidMusicPlayer**：通过 Gradle 生成 APK/AAB，按 normal/fdroid 渠道处理依赖差异。
- **MusicServer**：以 Spring Boot 应用运行，可部署在本机、NAS、家庭服务器或云主机。
- **更新机制**：MusicPlayer 已使用 electron-updater/GitHub release 配置；Android 依赖渠道分发；MusicServer 当前以手动部署为主。

## 技术要求与约束

### 性能要求
- MusicServer 上传和流媒体接口不能把大音频文件整体读入内存。
- 客户端刷新私有曲库时应避免阻塞播放主流程。
- Android 端网络请求和文件操作必须运行在协程或后台线程。
- Electron 主进程中的文件、下载和系统集成任务需要避免阻塞窗口生命周期。

### 兼容要求
- **MusicPlayer**：目标平台为 Windows、macOS、Linux，兼顾 Web 调试能力。
- **AndroidMusicPlayer**：`minSdk 24`、`targetSdk 36`、`compileSdk 35`。
- **MusicServer**：Java 17+，本地 H2，生产方向 PostgreSQL。
- **API 兼容**：桌面端 TypeScript 类型和 Android Kotlin DTO 必须与 MusicServer 响应保持一致。

### 安全要求
- MusicServer 密码必须哈希存储。
- Access token 应以哈希形式存储在服务端，并支持过期和登出失效。
- 所有音乐、歌单、收藏、头像更新等受保护资源必须按当前用户隔离。
- 客户端存储 token 时要减少暴露面；流媒体 URL token 参数只作为播放器兼容方案使用。
- CORS 默认开发友好，生产部署应收紧 `music.cors.allowed-origins`。

### 可扩展性与可靠性
- MusicServer 当前面向个人和小规模家庭部署。
- 后续如果进入长期运行环境，应引入数据库迁移、备份策略、健康检查和更严格的存储路径校验。
- 客户端应将 MusicServer 视为可选能力：后端不可用时，本地/在线播放能力仍要可用。

## 技术决策与理由
1. **三项目保留独立工程结构**：避免一次性重写成熟客户端，让桌面、Android、后端分别按自身生态演进。
2. **REST API 作为唯一共享边界**：降低 Electron、Android 和 Spring Boot 之间的耦合。
3. **MusicServer 使用模块化单体**：当前领域规模适合单体，部署和调试成本低。
4. **H2 默认 + PostgreSQL 预留**：开发启动简单，同时保留生产持久化路径。
5. **文件系统保存音频**：避免大文件进入数据库，便于备份和迁移。
6. **不透明 token 而非 JWT**：更容易服务端撤销，适合个人后端。
7. **客户端混合曲库模型**：私有曲库能力逐步接入，不打断已有本地和在线音乐流程。

## 已知限制
- MusicServer 尚未形成完整数据库迁移体系，当前仍依赖 JPA `ddl-auto=update`。
- MusicServer 尚未提供完整 Range/转码/离线缓存策略。
- 桌面端和 Android 端的 MusicServer 功能仍处于渐进集成阶段，部分体验可能不完全一致。
- Android 项目仍保留大量 Retro Music 原始命名和渠道能力，CH Music 品牌与私有云能力需要继续整合。
- 跨设备同步目前依赖手动刷新和普通 REST 请求，还没有实时同步协议。
