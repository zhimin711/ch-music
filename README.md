# CH Music

CH Music 是一个跨端个人音乐系统，由桌面端、Android 端和自托管后端共同组成。目标是让用户在不同设备上获得熟悉的音乐播放体验，同时掌控自己的私有曲库、歌单、收藏和账号数据。

## 项目组成

| 项目 | 目录 | 说明 |
| --- | --- | --- |
| 桌面端 | `MusicPlayer/` | Electron + Vue 的桌面音乐播放器，提供在线发现、私有音乐播放、歌词、下载、快捷键、远程控制等能力。 |
| Android 端 | `AndroidMusicPlayer/` | 原生 Android 音乐播放器，提供本地曲库、歌词、播放队列、小组件、Android Auto，并逐步接入 MusicServer。 |
| 私有后端 | `MusicServer/` | Spring Boot 后端，提供账号、头像、上传音乐、鉴权流媒体、歌单和收藏 API。 |

三端通过 MusicServer 的 REST API 共享私有曲库能力。桌面端和 Android 端仍保留各自原有的本地播放、在线音乐和系统集成能力。

## 核心能力

- 桌面端音乐播放、搜索、推荐、歌单、MV、歌词、桌面歌词、EQ、下载、快捷键和多语言界面。
- Android 本地音乐播放、Material You 风格、歌词、队列、小组件、锁屏控制、Android Auto 和渠道化构建。
- 私有音乐后端注册、登录、登出、用户资料、头像上传、音乐上传、在线播放、歌单和收藏。
- 桌面端与 Android 端通过 `/api/auth`、`/api/music`、`/api/playlists`、`/api/favorites` 接入同一个 MusicServer。
- MusicServer 私有音乐流支持 HTTP Range，桌面端和 Android 端可 seek 播放并复用同一鉴权接口。
- MusicServer 可选启用 FFmpeg 转码 profile；未启用或未安装 FFmpeg 时，客户端仍播放原始音频。
- 桌面端和 Android 端支持私有音乐离线缓存，缓存按 `serverBaseUrl + userId + musicId + profileId` 隔离，并用 checksum 标记过期。
- 本地开发默认可运行，不强制依赖外部数据库。

## 快速开始

### 1. 启动 MusicServer

```bash
cd MusicServer
MAVEN_USER_HOME=.m2 ./mvnw -s .mvn/local-settings.xml spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

默认本地数据：

- H2 数据库：`MusicServer/.local/musicserver-db`
- 上传文件：`MusicServer/.local/music-storage`
- 转码临时目录：`MusicServer/.local/transcode-temp`
- 转码缓存目录：`MusicServer/.local/transcode-cache`

Range 默认启用；转码默认关闭且 FFmpeg 不是启动必需项。需要转码时可在 Spring 配置中设置：

```properties
music.streaming.range.enabled=true
music.transcoding.enabled=true
music.transcoding.ffmpeg-path=ffmpeg
music.transcoding.max-concurrency=1
music.transcoding.temp-root=./.local/transcode-temp
music.transcoding.cache-root=./.local/transcode-cache
music.transcoding.cache-max-size=20GB
music.transcoding.profiles[0].id=aac-128
music.transcoding.profiles[0].label=AAC 128k
music.transcoding.profiles[0].content-type=audio/aac
music.transcoding.profiles[0].bitrate-kbps=128
music.transcoding.profiles[0].extension=.aac
music.transcoding.profiles[0].args=-vn -c:a aac -b:a 128k
music.transcoding.profiles[0].offline-cacheable=true
```

### 2. 启动桌面端 MusicPlayer

```bash
cd MusicPlayer
npm install
npm run dev
```

桌面端默认连接本地 MusicServer：

```text
http://localhost:8080
```

如需覆盖后端地址，可通过 `VITE_MUSIC_SERVER_BASE_URL` 配置。

桌面端私有音乐离线缓存由 Electron 主进程管理，索引保存在应用数据目录的 `music-server-offline-cache` store，文件位于应用数据目录下的 `music-server-private/`。渲染进程只拿到安全的缓存状态和播放 URL，不直接枚举本地路径。

### 3. 启动 AndroidMusicPlayer

```bash
cd AndroidMusicPlayer
./gradlew assembleDebug
```

Android 模拟器 debug 构建默认访问：

```text
http://10.0.2.2:8080
```

如需指定 MusicServer 地址：

```bash
./gradlew assembleDebug -PMUSIC_SERVER_BASE_URL=http://你的服务地址:8080
```

Android 私有音乐离线缓存位于应用私有文件目录 `music-server-cache/`。默认策略为 Wi-Fi only，存储空间不足会将条目标记为 `STORAGE_LOW`；播放时 ExoPlayer 优先使用 READY 缓存文件，否则回退到带鉴权请求头的 MusicServer Range 流。

## 常用命令

### MusicPlayer

```bash
cd MusicPlayer
npm run dev              # 桌面端开发
npm run dev:web          # Web 调试
npm run typecheck        # TypeScript 类型检查
npm run lint             # ESLint 和 i18n 检查
npm run build            # 构建
npm run build:win        # Windows 打包
npm run build:mac        # macOS 打包
npm run build:linux      # Linux 打包
```

### MusicServer

```bash
cd MusicServer
MAVEN_USER_HOME=.m2 ./mvnw -s .mvn/local-settings.xml spring-boot:run
MAVEN_USER_HOME=.m2 ./mvnw -s .mvn/local-settings.xml test
```

### AndroidMusicPlayer

```bash
cd AndroidMusicPlayer
./gradlew assembleDebug
./gradlew assembleNormalDebug
./gradlew assembleFdroidDebug
./gradlew lint
```

## MusicServer API 概览

受保护接口使用：

```http
Authorization: Bearer <accessToken>
```

主要接口：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `PUT /api/auth/me`
- `POST /api/auth/me/avatar`
- `GET /api/music`
- `POST /api/music`
- `GET /api/music/{musicId}`
- `GET /api/music/{musicId}/stream`
- `GET /api/music/{musicId}/stream?profile={profileId}`
- `GET /api/music/transcode-capabilities`
- `POST /api/music/{musicId}/transcodes/{profileId}`
- `GET /api/music/{musicId}/transcodes/{profileId}`
- `DELETE /api/music/{musicId}`
- `GET /api/playlists`
- `POST /api/playlists`
- `GET /api/playlists/{playlistId}`
- `PUT /api/playlists/{playlistId}`
- `DELETE /api/playlists/{playlistId}`
- `POST /api/playlists/{playlistId}/tracks`
- `DELETE /api/playlists/{playlistId}/tracks/{trackId}`
- `GET /api/favorites`
- `POST /api/favorites/{musicId}`
- `DELETE /api/favorites/{musicId}`

## 目录结构

```text
ch-music/
├── .spec-workflow/          # spec-workflow 指导文件、规格和审批数据
├── MusicPlayer/             # Electron/Vue 桌面端
├── AndroidMusicPlayer/      # Android 客户端
├── MusicServer/             # Spring Boot 私有后端
└── README.md                # 仓库入口说明
```

更详细的项目级指导文件：

- `.spec-workflow/steering/product.md`
- `.spec-workflow/steering/tech.md`
- `.spec-workflow/steering/structure.md`

## 开发约定

- 跨端私有曲库能力以 MusicServer REST API 为边界。
- MusicPlayer 和 AndroidMusicPlayer 不直接共享源码，只共享 API 契约和数据语义。
- 后端 API 变更需要同步更新客户端类型、DTO 和相关规格文档。
- MusicServer 中所有用户资源必须按当前认证用户隔离。
- 客户端离线缓存必须按服务器地址、用户、音乐和 profile 隔离；刷新曲库时同步 checksum，源文件删除或 checksum 改变后缓存应隐藏、删除或标记为过期。
- Android 的 `normal` 和 `fdroid` 渠道差异必须放在对应 source set 中。
- MusicPlayer 新增用户可见文案时，需要保持多语言 key 一致。

## 相关文档

- `MusicPlayer/README.md`：桌面端项目说明。
- `MusicPlayer/DEV.md`：桌面端开发文档。
- `MusicServer/README.md`：后端运行和 API 说明。
- `AndroidMusicPlayer/README.md`：Android 项目说明。
- `.spec-workflow/specs/`：功能规格文档。

## 当前重点方向

- 统一桌面端与 Android 端的 MusicServer 登录、私有曲库、收藏和歌单体验。
- 补齐 MusicServer 的流媒体、元数据、迁移和生产部署能力。
- 在不破坏现有播放器体验的前提下，逐步完成 CH Music 品牌和私有音乐云能力整合。

## 声明

本项目用于学习、研究和个人使用场景。涉及第三方音乐服务或开源项目的部分，请遵守对应服务条款、版权要求和开源许可证。
