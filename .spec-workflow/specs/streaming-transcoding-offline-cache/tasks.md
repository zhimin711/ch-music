# Tasks Document

- [x] 1. 扩展 MusicServer 流媒体配置和播放能力模型
  - Files: `MusicServer/src/main/java/com/chmusic/musicserver/config/MusicServerProperties.java`, `MusicServer/src/main/java/com/chmusic/musicserver/api/dto/MusicResponse.java`
  - 增加 streaming/transcoding/cache 配置结构。
  - 在 `MusicResponse` 中加入 `updatedAt`、`PlaybackCapabilities`、`PlaybackVariant` 等响应字段，并保持现有字段向后兼容。
  - _Leverage: `MusicServerProperties`, `MusicResponse`, `MusicFile`_
  - _Requirements: 2.1, 2.5, 7.1, 8.4_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java/Spring Boot backend developer | Task: Extend MusicServer configuration and response DTOs for streaming, transcoding, cache capabilities, keeping existing API fields compatible | Restrictions: Do not remove existing MusicResponse fields; do not make FFmpeg mandatory; keep DTOs separate from JPA entities | _Leverage: MusicServerProperties, MusicResponse, MusicFile | _Requirements: 2.1, 2.5, 7.1, 8.4 | Success: application starts with default config, existing list/detail responses still serialize old fields plus new capability fields, and config properties can be bound by Spring. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 2. 实现 MusicServer Range 响应核心服务
  - Files: `MusicServer/src/main/java/com/chmusic/musicserver/music/StreamingService.java`, `MusicServer/src/main/java/com/chmusic/musicserver/api/MusicController.java`
  - 新增 `StreamingService`，支持无 Range 的 `200 OK`、合法单段 Range 的 `206 Partial Content`、非法范围的 `416 Range Not Satisfiable`。
  - 修改 `/api/music/{musicId}/stream` 由 `StreamingService` 统一返回。
  - _Leverage: `MusicService.requireOwnedMusic`, `MusicStorageService.pathOf`, Spring `HttpRange`/`Resource`_
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring MVC streaming specialist | Task: Implement standard HTTP Range streaming for MusicServer original files and wire MusicController to use it | Restrictions: Do not read whole audio files into memory; preserve owner checks; do not leak local paths; support only single-range initially and document/test behavior | _Leverage: MusicService.requireOwnedMusic, MusicStorageService.pathOf, MusicController | _Requirements: 1.1-1.6 | Success: stream endpoint returns correct 200/206/416 headers and bodies, supports seek-friendly playback, and keeps auth/ownership behavior intact. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 3. 添加 Range 流媒体后端测试
  - Files: `MusicServer/src/test/java/com/chmusic/musicserver/api/MusicControllerTests.java`
  - 覆盖无 Range、普通 Range、开放结束 Range、后缀 Range、越界 Range、跨用户访问和文件缺失场景。
  - _Leverage: Spring Boot WebMVC test starters, existing `MusicServerApplicationTests`_
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Backend QA engineer | Task: Add WebMVC/integration tests for MusicServer Range streaming behavior | Restrictions: Tests must use temporary files/data and not depend on real local .local data; do not test Spring internals; assert important response headers | _Leverage: Spring Boot test stack, MusicController, MusicService | _Requirements: 1.1-1.6 | Success: tests verify 200/206/416 semantics, ownership protection, and missing-file error behavior. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 4. 实现转码 profile 目录和工具检测
  - Files: `MusicServer/src/main/java/com/chmusic/musicserver/music/TranscodeProfileCatalog.java`, `MusicServer/src/main/java/com/chmusic/musicserver/music/TranscodeToolProbe.java`
  - 从配置读取可用 profile，检测 FFmpeg 或等价工具是否可执行。
  - 提供禁用、未安装、profile 不存在等状态。
  - _Leverage: `MusicServerProperties`_
  - _Requirements: 2.2, 2.3, 2.5, 8.4_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java backend configuration developer | Task: Implement configurable transcode profile catalog and optional FFmpeg tool probing | Restrictions: FFmpeg must remain optional; probing must be safe and bounded; unsupported profile should produce clear status, not exceptions leaking implementation detail | _Leverage: MusicServerProperties | _Requirements: 2.2, 2.3, 2.5, 8.4 | Success: capabilities can report enabled/toolAvailable/profile list accurately for default disabled and configured states. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 5. 创建转码产物实体和仓储
  - Files: `MusicServer/src/main/java/com/chmusic/musicserver/music/TranscodeVariant.java`, `MusicServer/src/main/java/com/chmusic/musicserver/music/TranscodeVariantRepository.java`
  - 增加转码产物索引，保存 owner、music、profile、状态、路径、大小、checksum、错误和访问时间。
  - 确保按 owner/music/profile 查询唯一产物。
  - _Leverage: existing JPA entity/repository patterns in `music`, `playlist`, `favorite` packages_
  - _Requirements: 3.3, 3.4, 4.1, 4.3, 7.1_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Data JPA developer | Task: Add TranscodeVariant entity and repository for server-side transcode cache indexing | Restrictions: Do not expose entity directly in API; preserve owner scoping; avoid breaking current JPA schema startup | _Leverage: MusicFile, AppUser, existing repository patterns | _Requirements: 3.3, 3.4, 4.1, 4.3, 7.1 | Success: entity persists and queries variants by owner/music/profile, supports stale/failed/ready states, and schema starts locally. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 6. 实现服务端转码缓存服务
  - Files: `MusicServer/src/main/java/com/chmusic/musicserver/music/TranscodeCacheService.java`, `MusicServer/src/main/java/com/chmusic/musicserver/music/MusicService.java`
  - 管理转码产物路径、checksum 校验、容量清理、音乐删除时清理关联产物。
  - _Leverage: `MusicStorageService`, `TranscodeVariantRepository`, `MusicService.delete`_
  - _Requirements: 4.1, 4.2, 4.3, 4.5, 4.6, 7.2, 7.3_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Backend storage engineer | Task: Implement server-side transcode cache pathing, validation, pruning, and delete hooks | Restrictions: Never delete original uploaded audio during cache pruning; normalize and constrain paths; make disk errors controlled and user-safe | _Leverage: MusicStorageService, TranscodeVariantRepository, MusicService.delete | _Requirements: 4.1-4.6, 7.2, 7.3 | Success: cache service validates variants by checksum, removes variants when music is deleted, and prunes only cache files according to config. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 7. 实现转码执行器和任务状态服务
  - Files: `MusicServer/src/main/java/com/chmusic/musicserver/music/TranscodeExecutor.java`, `MusicServer/src/main/java/com/chmusic/musicserver/music/TranscodeService.java`
  - 封装 FFmpeg 命令执行、并发限制、临时文件、READY/FAILED/QUEUED/PROCESSING 状态。
  - 同一 music/profile 复用任务，失败可重试。
  - _Leverage: `TranscodeProfileCatalog`, `TranscodeCacheService`, `TranscodeVariantRepository`_
  - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.6, 3.7, 4.1_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Backend media processing developer | Task: Implement bounded transcode execution and status management using optional FFmpeg | Restrictions: No unbounded process spawning; sanitize command args; write temp output atomically before marking READY; do not block request threads for long transcodes where avoidable | _Leverage: TranscodeProfileCatalog, TranscodeCacheService, TranscodeVariantRepository | _Requirements: 3.1-3.7, 4.1 | Success: prepare/status workflow creates/reuses jobs, records failures safely, marks successful variants ready, and respects concurrency config. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 8. 暴露转码能力、准备和状态 API
  - Files: `MusicServer/src/main/java/com/chmusic/musicserver/api/MusicController.java`, `MusicServer/src/main/java/com/chmusic/musicserver/api/dto/TranscodeStatusResponse.java`
  - 增加 `GET /api/music/transcode-capabilities`、`POST /api/music/{musicId}/transcodes/{profileId}`、`GET /api/music/{musicId}/transcodes/{profileId}`。
  - 扩展 stream endpoint 支持 `profile` 查询参数。
  - _Leverage: `TranscodeService`, `StreamingService`, `MusicResponse`_
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.2, 8.2_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: REST API developer | Task: Expose MusicServer transcode capability, prepare/status APIs, and profile-aware stream endpoint | Restrictions: Keep original stream behavior default; all endpoints must enforce owner auth; return structured client-understandable errors for unsupported/not-ready profiles | _Leverage: MusicController, TranscodeService, StreamingService, MusicResponse | _Requirements: 2.1-2.4, 3.2, 8.2 | Success: clients can discover capabilities, prepare a profile, poll status, and stream ready variants while original stream remains backward compatible. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 9. 添加转码和缓存后端测试
  - Files: `MusicServer/src/test/java/com/chmusic/musicserver/music/TranscodeServiceTests.java`, `MusicServer/src/test/java/com/chmusic/musicserver/api/TranscodeApiTests.java`
  - 使用 fake executor 覆盖 profile 不可用、工具不可用、任务复用、成功、失败、checksum 失效和删除清理。
  - _Leverage: Spring Boot test stack, temporary directories_
  - _Requirements: 2.1-2.5, 3.1-3.7, 4.1-4.6, 7.1-7.3, 8.5_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Backend test engineer | Task: Add unit/integration tests for transcode capability, prepare/status, cache validity, and cleanup | Restrictions: Do not require real FFmpeg in tests; use fake executor and temp directories; assert sanitized error behavior | _Leverage: TranscodeService, TranscodeCacheService, MusicController | _Requirements: 2.1-2.5, 3.1-3.7, 4.1-4.6, 7.1-7.3, 8.5 | Success: tests cover happy path and failure states without external media tooling dependency. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 10. 扩展 MusicPlayer 类型和 API 客户端
  - Files: `MusicPlayer/src/renderer/types/musicServer.ts`, `MusicPlayer/src/renderer/api/musicServer.ts`
  - 增加 playback capabilities、variants、transcode status、offline cache request/response 类型。
  - 增加 capabilities、prepare/status transcode API 方法。
  - _Leverage: existing MusicServer TypeScript types and axios instance_
  - _Requirements: 2.1, 2.2, 2.3, 7.1, 8.3_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: TypeScript API client developer | Task: Extend MusicPlayer MusicServer types and API wrapper for playback capabilities and transcode endpoints | Restrictions: Preserve existing exported functions and interfaces; avoid breaking current store code; keep request auth interceptor unchanged | _Leverage: musicServer.ts API wrapper, musicServer types | _Requirements: 2.1-2.3, 7.1, 8.3 | Success: TypeScript compiles, existing calls still typecheck, and new endpoints/types are available for stores. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 11. 实现 MusicPlayer 主进程私有音乐离线缓存管理器
  - Files: `MusicPlayer/src/main/modules/musicServerOfflineCache.ts`, `MusicPlayer/src/preload/index.ts`
  - 新增缓存索引、账号命名空间、断点下载、checksum 校验、IPC 通道。
  - 优先复用现有磁盘缓存配置和下载管理思路。
  - _Leverage: `MusicPlayer/src/main/modules/cache.ts`, `downloadManager.ts`, `config.ts`, `preload/index.ts`_
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 7.2, 7.6_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Electron main-process developer | Task: Implement MusicServer offline cache manager with namespaced index, resumable downloads, checksum validation, IPC, and cleanup | Restrictions: Do not merge private offline cache blindly with generic online cache; do not expose raw local paths to untrusted renderer operations except playback URL resolution; do not commit cached files | _Leverage: cache.ts, downloadManager.ts, config.ts, preload/index.ts | _Requirements: 5.1-5.7, 7.2, 7.6 | Success: renderer can enqueue/remove/query/resolve cached MusicServer files, cache is namespaced by server/user, and checksum mismatch marks stale. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 12. 集成 MusicPlayer 渲染进程缓存状态和播放 URL 选择
  - Files: `MusicPlayer/src/renderer/store/modules/musicServer.ts`, `MusicPlayer/src/renderer/utils/musicServerUtils.ts`
  - 在 store 中管理缓存状态和缓存动作。
  - `toMusicServerSongResult` 播放 URL 优先使用已验证本地缓存，否则使用 stream URL。
  - _Leverage: existing `musicServer` store, `toMusicServerSongResult`, Electron preload API_
  - _Requirements: 5.1, 5.3, 5.4, 7.2, 7.5, 8.1, 8.3_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue/Pinia integration developer | Task: Integrate MusicServer offline cache state and cached playback URL resolution into MusicPlayer renderer store and song mapping | Restrictions: Do not block existing online playback if cache state fails; keep UI state serializable; avoid large UI redesign in this task | _Leverage: musicServer store, musicServerUtils, preload API | _Requirements: 5.1, 5.3, 5.4, 7.2, 7.5, 8.1, 8.3 | Success: store exposes cache actions/state and MusicServer songs can resolve cached URLs when valid with online fallback. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 13. 添加 MusicPlayer 离线缓存基础 UI 控制
  - Files: `MusicPlayer/src/renderer/views/user/UserPage.vue` or relevant MusicServer user/library view, `MusicPlayer/src/i18n/lang/zh-CN/user.ts`
  - 为私有音乐单曲提供缓存、移除缓存、重试、状态展示入口。
  - 中文文案先落地，并确保后续可补齐多语言。
  - _Leverage: existing MusicServer user view and store actions_
  - _Requirements: 5.1, 5.6, 5.7, 8.1, 8.3, 8.6_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue frontend developer | Task: Add minimal MusicPlayer UI controls for MusicServer offline cache status/actions in the existing private library surface | Restrictions: Do not redesign the whole page; do not add visible explanatory tutorial text; follow existing UI conventions and i18n pattern | _Leverage: existing user/MusicServer view, musicServer store, i18n user file | _Requirements: 5.1, 5.6, 5.7, 8.1, 8.3, 8.6 | Success: users can see cache state and trigger cache/remove/retry for private songs without breaking existing playback. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 14. 实现 Android MusicServer 缓存模型和管理器
  - Files: `AndroidMusicPlayer/app/src/main/java/code/name/monkey/retromusic/musicserver/MusicServerCacheManager.kt`, `AndroidMusicPlayer/app/src/main/java/code/name/monkey/retromusic/musicserver/MusicServerModels.kt`
  - 增加缓存 entry/state、账号命名空间、checksum 校验、Wi-Fi only 和存储不足状态。
  - _Leverage: `MusicServerRepository`, `MusicServerSession`, existing model patterns_
  - _Requirements: 6.1, 6.3, 6.4, 6.5, 6.6, 6.7, 7.2, 7.6_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Android Kotlin repository developer | Task: Implement Android MusicServer cache state models and manager for offline entries, namespacing, validation, and download policy state | Restrictions: Do not scatter cache logic into fragments; do not require Google-only APIs so fdroid remains viable; keep UI thread non-blocking | _Leverage: MusicServerRepository, MusicServerSession, MusicServerModels | _Requirements: 6.1, 6.3-6.7, 7.2, 7.6 | Success: repository layer exposes cache states and can enqueue/remove/sync private music cache entries with account isolation. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 15. 接入 Android ExoPlayer 缓存数据源
  - Files: `AndroidMusicPlayer/app/src/main/java/code/name/monkey/retromusic/musicserver/MusicServerDataSourceFactory.kt`, `AndroidMusicPlayer/app/src/main/java/code/name/monkey/retromusic/service/RetroExoPlayer.kt`
  - 为 MusicServer HTTP 流使用带 token 的 DataSource 和本地缓存数据源。
  - 保持本地文件和非 MusicServer URL 播放路径兼容。
  - _Leverage: Media3 ExoPlayer, current `RetroExoPlayer`, `MusicServerSongMapper`_
  - _Requirements: 1.1, 1.2, 6.2, 6.3, 7.5_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Android media playback developer | Task: Integrate Media3 cache-aware data source for MusicServer streams into RetroExoPlayer while preserving existing playback | Restrictions: Do not break local file playback; avoid token logging; keep fdroid compatibility; avoid blocking player prepare on heavy cache work | _Leverage: RetroExoPlayer, MusicServerSongMapper, Media3 dependencies | _Requirements: 1.1, 1.2, 6.2, 6.3, 7.5 | Success: MusicServer streams play through cache-aware data source, cached files are preferred when valid, and seeking still works via Range-capable server. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [x] 16. 添加 Android 私有曲库缓存 UI 状态
  - Files: `AndroidMusicPlayer/app/src/main/java/code/name/monkey/retromusic/fragments/other/UserInfoFragment.kt`, relevant Android string resources
  - 在现有 MusicServer 用户/曲库界面显示缓存、移除缓存、重试和状态。
  - _Leverage: existing `UserInfoFragment`, `MusicServerRepository`_
  - _Requirements: 6.1, 6.5, 6.7, 8.1, 8.3, 8.6_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Android UI developer | Task: Add minimal offline cache controls/status to the existing MusicServer user/library UI | Restrictions: Do not redesign full Retro Music UI; keep strings resource-based; do not block UI thread; hide unavailable actions when cache feature is disabled | _Leverage: UserInfoFragment, MusicServerRepository, Android string resources | _Requirements: 6.1, 6.5, 6.7, 8.1, 8.3, 8.6 | Success: users can see Android cache state and trigger cache/remove/retry from the existing private music surface. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [-] 17. 实现跨端缓存失效和同步规则
  - Files: `MusicPlayer/src/renderer/store/modules/musicServer.ts`, `AndroidMusicPlayer/app/src/main/java/code/name/monkey/retromusic/musicserver/MusicServerRepository.kt`
  - 客户端刷新私有曲库时同步服务端元数据，标记 stale、missing、ready。
  - 处理退出登录、切换账号、切换服务器地址时的缓存隔离。
  - _Leverage: existing restoreSession/loadAll flows in both clients_
  - _Requirements: 7.2, 7.3, 7.4, 7.5, 7.6_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Cross-client integration developer | Task: Implement cache synchronization and invalidation rules in desktop and Android MusicServer session flows | Restrictions: Do not delete user files unexpectedly on logout; never show cache entries from another account/server; offline valid caches may remain playable when explicitly allowed | _Leverage: MusicPlayer musicServer store, Android MusicServerRepository restore/load flows | _Requirements: 7.2-7.6 | Success: both clients mark stale/missing cache entries correctly on refresh and isolate caches by server/user identity. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [ ] 18. 补充端到端文档和运行说明
  - Files: `MusicServer/README.md`, `README.md`, `.spec-workflow/specs/streaming-transcoding-offline-cache/design.md`
  - 更新 Range、转码配置、缓存目录、客户端离线缓存行为说明。
  - _Leverage: existing README files and approved spec documents_
  - _Requirements: 8.4, 8.5_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Technical writer with backend/client context | Task: Update user-facing and developer docs for Range, transcode configuration, cache directories, and offline cache behavior | Restrictions: Keep docs concise and accurate; do not claim unsupported defaults; mention FFmpeg is optional/configured | _Leverage: MusicServer/README.md, root README.md, spec design | _Requirements: 8.4, 8.5 | Success: maintainers can configure and troubleshoot streaming/transcoding/cache features from docs. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._

- [ ] 19. 执行三端验证和回归测试
  - Files: no production files; test commands and any necessary test fixtures only
  - 运行 MusicServer 测试、MusicPlayer typecheck/lint、Android lint/build 的相关最小集合。
  - 验证关键用户路径：上传、Range 播放、转码准备/播放、桌面缓存播放、Android 缓存播放、删除失效、账号切换隔离。
  - _Leverage: `MusicServer/mvnw`, `MusicPlayer/package.json`, `AndroidMusicPlayer/gradlew`_
  - _Requirements: All_
  - _Prompt: Implement the task for spec streaming-transcoding-offline-cache, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Release validation engineer | Task: Run and document validation for backend, desktop, and Android streaming/transcoding/offline-cache flows | Restrictions: Do not skip failed checks silently; do not require external FFmpeg for default test path; record any environment limitations clearly | _Leverage: Maven tests, npm typecheck/lint, Gradle lint/build, spec requirements | _Requirements: All | Success: relevant automated checks pass or documented blockers exist, and manual smoke scenarios are verified enough for implementation completion. Before implementation mark this task [-] in tasks.md, after completion call log-implementation with artifacts and then mark [x]._
