# 项目结构

## 目录组织

```text
ch-music/
├── README.md                         # 仓库级入口说明
├── .spec-workflow/                   # spec-workflow 指导文件、规格和审批数据
│   ├── steering/                     # 项目级指导文件：product、tech、structure
│   ├── specs/                        # 功能规格文档
│   ├── templates/                    # 默认模板
│   └── user-templates/               # 用户自定义模板
├── MusicPlayer/                      # Electron/Vue 桌面端
│   ├── src/
│   │   ├── main/                     # Electron 主进程
│   │   ├── preload/                  # 预加载脚本和主/渲染桥接类型
│   │   ├── renderer/                 # Vue 渲染进程
│   │   ├── i18n/                     # 多语言资源和入口
│   │   └── shared/                   # 主进程/渲染进程共享类型或常量
│   ├── resources/                    # 打包资源
│   ├── build/                        # 安装包和平台构建配置
│   ├── docs/                         # 桌面端文档和图片
│   └── scripts/                      # 构建、检查、维护脚本
├── AndroidMusicPlayer/               # Android 客户端
│   ├── app/                          # Android 应用主模块
│   │   └── src/
│   │       ├── main/                 # 主源码、资源、Manifest
│   │       ├── debug/                # debug 专用资源
│   │       ├── normal/               # normal flavor 专用代码和资源
│   │       └── fdroid/               # fdroid flavor 专用代码和资源
│   ├── appthemehelper/               # Android 主题辅助模块
│   ├── gradle/                       # Gradle Wrapper 和版本目录
│   ├── fastlane/                     # 应用商店元数据
│   ├── screenshots/                  # 截图素材
│   └── assets/                       # README/发布素材
└── MusicServer/                      # Spring Boot 私有音乐后端
    ├── src/main/java/com/chmusic/musicserver/
    │   ├── api/                      # Controller、当前用户解析、DTO
    │   ├── auth/                     # 注册、登录、token
    │   ├── config/                   # Spring 配置和属性
    │   ├── favorite/                 # 收藏领域
    │   ├── music/                    # 音乐上传、存储、流媒体
    │   ├── playlist/                 # 歌单和歌单歌曲
    │   ├── security/                 # Bearer token 过滤器
    │   └── user/                     # 用户和头像
    ├── src/main/resources/           # application.properties、迁移、静态资源
    ├── src/test/                     # 测试
    └── .local/                       # 本地运行数据，不能提交
```

## MusicPlayer 结构规则

### 主进程
- `src/main/index.ts` 是主进程入口。
- `src/main/modules/` 放置系统层模块，例如窗口、托盘、下载、快捷键、远程控制、缓存、字体、本地扫描等。
- `src/main/server.ts` 和 `src/main/unblockMusic.ts` 保持本地服务和资源解析相关逻辑。
- 主进程代码可以使用 Node/Electron 能力，不应直接引入渲染进程组件。

### 预加载脚本
- `src/preload/index.ts` 暴露安全桥接能力。
- `src/preload/index.d.ts` 维护渲染进程可见的类型声明。
- 新增主/渲染通信能力时，应同步更新实现和类型。

### 渲染进程
- `src/renderer/api/` 放 HTTP 请求封装；MusicServer 请求集中在 `musicServer.ts`。
- `src/renderer/store/modules/` 放 Pinia store；跨页面共享状态放这里，不放在单个 view 中。
- `src/renderer/views/` 放路由级页面。
- `src/renderer/components/` 放可复用 UI 组件，按功能分组。
- `src/renderer/services/` 放较复杂的客户端服务、播放器控制、worker、翻译引擎等。
- `src/renderer/types/` 放共享 TypeScript 类型。
- `src/renderer/utils/` 放纯工具函数和映射逻辑。
- `src/renderer/router/` 只维护路由定义和路由组织。

### 多语言
- `src/i18n/lang/<locale>/` 下按功能拆分文案。
- 新增用户可见文案时，应补齐所有现有语言文件，至少保持 key 一致。
- i18n 检查失败时先修语言包，不要删除缺失 key 来规避检查。

## AndroidMusicPlayer 结构规则

### 模块和 source set
- `app` 是主应用模块。
- `appthemehelper` 只放主题辅助和相关通用视图能力。
- `app/src/main/` 放所有渠道共享的代码和资源。
- `app/src/normal/` 放 Google Play、Cast、Billing 等 normal 渠道专属能力。
- `app/src/fdroid/` 放 F-Droid 渠道替代实现，避免依赖 Google Play 专有能力。
- `app/src/debug/` 只放调试资源或调试配置。

### 主包分层
- `activities/`：Activity 和 Activity 基类。
- `fragments/`：页面和功能分区 Fragment。
- `adapter/`：RecyclerView/List 适配器。
- `repository/`：本地媒体、歌单、搜索、统计等数据读取逻辑。
- `service/`：播放服务、通知、后台播放相关能力。
- `model/`：本地领域模型和智能歌单模型。
- `db/`：Room 数据库相关代码。
- `network/`：外部网络服务。
- `musicserver/`：MusicServer API、DTO、session、repository、远程歌曲映射。
- `lyrics/`：歌词解析和视图。
- `glide/`：图片加载和封面扩展。
- `helper/`、`util/`、`extensions/`：工具、扩展函数和辅助逻辑。
- `views/`、`preferences/`、`dialogs/`：自定义 UI、设置项和弹窗。

### Android 资源
- XML 布局放在 `res/layout*`，尺寸/主题/颜色放在对应 `values*`。
- 多语言文案放在各 `values-xx` 目录。
- 渠道资源只放在对应 flavor 的 `res` 目录。
- 新增布局时遵循现有 Fragment/Adapter 命名，而不是引入完全不同的 UI 分层。

## MusicServer 结构规则

### 包职责
- `api/`：REST Controller、DTO、异常响应、当前用户注入。Controller 保持薄层，只做请求/响应转换。
- `auth/`：注册、登录、登出、token 签发、token 持久化和校验。
- `user/`：用户实体、用户仓储、头像存储。
- `music/`：音乐实体、文件存储、上传、流媒体资源定位。
- `playlist/`：歌单实体、歌单歌曲、歌单服务和相关迁移辅助。
- `favorite/`：收藏实体、收藏服务和仓储。
- `security/`：认证过滤器和安全上下文写入。
- `config/`：Spring Security、CORS、应用属性等跨领域配置。

### 分层原则
- Controller 依赖 service 和 DTO。
- Service 依赖 repository、领域对象和必要的其他 service。
- Repository 不依赖 Controller 或 Web 层。
- JPA Entity 不直接作为外部 API 契约；响应使用 DTO。
- 任何查询或变更用户资源的 service 方法，都必须接收或解析当前用户并校验归属。

### 资源与配置
- `src/main/resources/application.properties` 保存默认本地开发配置。
- `src/main/resources/db/migration/` 用于未来稳定迁移脚本。
- `.local/` 是运行时数据目录，包含数据库和上传文件，不能提交。

## 命名约定

### 文件与目录
- **MusicPlayer Vue 组件**：PascalCase，例如 `MusicList.vue`。
- **MusicPlayer 普通 TS 模块**：遵循现有 camelCase 或 kebab-case；新增文件优先跟随同目录风格。
- **MusicPlayer store 模块**：功能名小写或 camelCase，例如 `musicServer.ts`、`localMusic.ts`。
- **Android Kotlin/Java 类**：PascalCase，按 Android 生态命名。
- **Android XML 资源**：snake_case，遵循现有 `fragment_*`、`item_*`、`pref_*` 等模式。
- **MusicServer Controller**：`*Controller.java`。
- **MusicServer Service**：`*Service.java`。
- **MusicServer Repository**：`*Repository.java`。
- **MusicServer DTO**：`*Request.java`、`*Response.java`。
- **MusicServer 测试**：`*Tests.java`。

### 代码
- **TypeScript/Vue**：类型、组件名使用 PascalCase；变量和函数使用 camelCase；常量按现有文件风格，跨模块常量可使用 UPPER_SNAKE_CASE。
- **Kotlin/Java Android**：类使用 PascalCase；函数和属性使用 camelCase；常量使用 UPPER_SNAKE_CASE 或 companion object 中的 const val。
- **Java 后端**：类使用 PascalCase；方法和字段使用 camelCase；常量使用 UPPER_SNAKE_CASE。

## 导入模式

### MusicPlayer
1. 第三方依赖。
2. `@/`、`@renderer/`、`@i18n/` 等别名导入。
3. 相对路径导入。
4. 样式导入。

ESLint 已启用 `simple-import-sort`，新增或修改文件后应保持导入排序。

### AndroidMusicPlayer
1. Android/AndroidX/Kotlin 标准库。
2. 第三方库。
3. `code.name.monkey.retromusic` 内部包。

尽量保持 Android Studio/ktlint 默认可读排序，不手动制造跨层循环依赖。

### MusicServer
1. Java/Jakarta 标准 API。
2. Spring 和第三方库。
3. `com.chmusic.musicserver` 项目包。

## 代码组织模式

### 文件内部组织
- 先导入依赖。
- 再声明常量、类型或 DTO。
- 主实现靠前，局部 helper 靠后。
- 导出内容保持清晰，不在一个文件里混合多个无关职责。

### 函数和方法
- 先做输入校验和权限/归属校验。
- 中间执行核心业务逻辑。
- 对外返回 DTO 或稳定模型，不直接暴露内部临时结构。
- 错误处理应贴近调用边界；后端统一错误响应放在全局异常处理。

### UI 组织
- 路由级页面放 views/fragments。
- 可复用小块放 components/adapter/views。
- 业务状态放 store/repository/viewmodel 类结构中，不塞进纯展示组件。

## 模块边界

### 跨项目边界
- MusicServer API 是桌面端和 Android 端唯一共享后端边界。
- 客户端新增私有曲库能力时，应先确认或扩展 MusicServer DTO/API，再同步客户端类型。
- MusicPlayer 与 AndroidMusicPlayer 不直接共享源码；共享的是产品规则、API 契约和数据语义。
- spec-workflow 文档用于记录跨项目规格和决策，不能只把约定藏在某一个客户端实现里。

### MusicPlayer 边界
- 主进程负责系统能力，渲染进程负责 UI 和交互，preload 负责安全桥接。
- 渲染进程不要直接使用 Node/Electron 高权限 API。
- MusicServer 状态集中在 `musicServer` store 和相关 API/类型/映射工具中。

### AndroidMusicPlayer 边界
- `musicserver/` 包集中管理后端接入，不要把 Retrofit 调用散落到 Fragment。
- normal/fdroid 差异必须通过 source set 隔离。
- 播放服务和 UI Fragment 之间通过现有播放器远程控制/helper 模式交互。

### MusicServer 边界
- `api` 层不承载复杂业务。
- `security` 和 `auth` 负责认证；领域 service 负责授权和归属校验。
- 文件路径处理必须留在 storage/service 层，Controller 不拼接真实文件路径。

## 代码规模建议
- Controller/Fragment/View 过大时，优先拆 service、store、repository 或可复用组件。
- 后端 service 方法应围绕一个用户动作，不把多个 API 流程塞进一个方法。
- Android Fragment 中避免堆积网络和数据库细节，复杂逻辑下沉到 repository/helper。
- Vue 组件中避免同时承担 API、状态、复杂映射和展示；跨页面逻辑进入 store/service/utils。

## 文档标准
- `.spec-workflow/steering/product.md`、`tech.md`、`structure.md` 是后续规格生成的项目级背景。
- 新增跨项目能力时，优先在 `.spec-workflow/specs/<feature>/` 下写 requirements、design、tasks。
- MusicServer API 行为变化应更新 `MusicServer/README.md` 和相关 spec。
- MusicPlayer 开发约定变化应更新 `MusicPlayer/DEV.md` 或对应指导文件。
- Android 渠道、构建、发布相关变化应更新 Android 项目 README、FAQ 或 fastlane 元数据。

## 生成与运行产物
- `MusicPlayer/out/`、`dist/`、`node_modules/` 属于生成或依赖目录，不作为源代码修改目标。
- `MusicServer/target/`、`.local/`、`.m2/` 是构建/运行数据，不应提交。
- `AndroidMusicPlayer/.gradle/`、`build/`、生成 APK/AAB 不应提交。
- `.DS_Store` 等系统文件不应作为功能改动提交。
