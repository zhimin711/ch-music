# Requirements Document

## Introduction

本规格定义 CH Music 的完整 Range、转码和离线缓存策略。当前 MusicServer 已能返回原始音频资源，但尚未形成完整的 HTTP Range 响应、按客户端能力选择转码格式、服务端转码产物缓存、桌面端离线缓存和 Android 端离线缓存闭环。

该能力的目标是让用户在桌面端和 Android 端播放私有曲库时获得更可靠的拖动进度、断点续播、弱网播放和离线播放体验，同时保持 MusicServer 自托管、用户数据隔离和可配置的部署特性。

参考方向：

- HTTP Range 请求应遵循 `Range`、`Accept-Ranges`、`Content-Range`、`206 Partial Content`、`416 Range Not Satisfiable` 等语义。
- 服务端应优先复用 Spring MVC 对 `HttpRange`、`Resource`、`ResourceRegion` 或等价能力的支持。
- Android 离线能力应优先贴合 Media3/ExoPlayer 的缓存、下载和数据源模式。
- 转码能力应以 FFmpeg 或等价命令行媒体工具为可选依赖，支持检测、降级和部署配置。

## Alignment with Product Vision

本功能直接支撑 steering 文档中的“个人音乐云”和“流媒体增强”方向：

- 提升 MusicServer 作为私有曲库后端的稳定性，让客户端播放不再依赖一次性完整文件响应。
- 保持桌面端和 Android 端的原有播放体验，新增能力应自然融入现有播放器流程。
- 支持移动端弱网、远程访问和离线场景，为后续跨设备同步、离线缓存和带宽控制打基础。
- 继续坚持用户拥有曲库、默认自托管、API 边界稳定、数据按用户隔离等产品原则。

## Requirements

### Requirement 1: HTTP Range 流媒体播放

**User Story:** 作为私有曲库用户，我希望 MusicServer 支持标准 Range 请求，以便桌面端和 Android 端可以稳定拖动进度、断点续播和跳转播放。

#### Acceptance Criteria

1. WHEN 客户端请求 `GET /api/music/{musicId}/stream` 且不带 `Range` 头 THEN MusicServer SHALL 返回完整音频内容，并包含 `Accept-Ranges: bytes`、正确的 `Content-Length`、`Content-Type` 和内联文件名响应头。
2. WHEN 客户端请求单段合法字节范围 THEN MusicServer SHALL 返回 `206 Partial Content`，并包含正确的 `Content-Range`、`Content-Length`、`Accept-Ranges: bytes` 和音频 `Content-Type`。
3. WHEN 客户端请求后缀范围或开放结束范围 THEN MusicServer SHALL 按 HTTP Range 语义计算实际字节区间，并返回可播放的部分内容。
4. IF 客户端请求越界或非法范围 THEN MusicServer SHALL 返回 `416 Range Not Satisfiable`，并包含当前资源长度信息。
5. WHEN 请求的音乐不属于当前认证用户 THEN MusicServer SHALL 保持现有用户隔离语义，不暴露文件存在性或路径信息。
6. WHEN 资源文件缺失 THEN MusicServer SHALL 返回受控错误响应，不输出本地文件系统路径。

### Requirement 2: 播放格式协商与转码入口

**User Story:** 作为桌面端或 Android 端用户，我希望客户端能根据网络和设备能力选择原始音频或转码音频，以便在不同场景下获得合适的音质和稳定性。

#### Acceptance Criteria

1. WHEN 客户端请求音乐详情或列表 THEN MusicServer SHALL 暴露足够的播放能力元数据，让客户端知道该歌曲是否支持原始播放、Range 播放、转码播放和离线缓存。
2. WHEN 客户端请求流媒体播放并指定目标格式、码率或质量档位 THEN MusicServer SHALL 返回匹配的原始流或转码流。
3. IF 请求的转码格式不受支持 THEN MusicServer SHALL 返回明确的客户端可理解错误，而不是回退到不可预测格式。
4. WHEN 客户端未指定转码偏好 THEN MusicServer SHALL 默认返回原始文件流，保持向后兼容。
5. IF 服务器未安装或未启用转码工具 THEN MusicServer SHALL 在能力元数据中声明转码不可用，并保持原始播放可用。

### Requirement 3: 服务端转码任务策略

**User Story:** 作为自托管用户，我希望 MusicServer 能以可控方式执行音频转码，以便远程播放和移动网络播放不会消耗过多带宽或服务器资源。

#### Acceptance Criteria

1. WHEN 用户请求某首音乐的转码版本且缓存不存在 THEN MusicServer SHALL 创建或复用转码任务，并限制同一歌曲、同一 profile 的重复任务。
2. WHEN 转码任务正在执行 THEN MusicServer SHALL 向客户端提供明确状态，避免客户端无限重试。
3. WHEN 转码任务成功 THEN MusicServer SHALL 保存转码产物及其 profile、源文件 checksum、大小、时长和创建时间等元数据。
4. IF 源文件 checksum 或存储路径发生变化 THEN MusicServer SHALL 使旧转码产物失效。
5. IF 转码失败 THEN MusicServer SHALL 记录失败原因、返回可理解错误，并允许后续重新尝试。
6. WHEN 服务器达到并发转码上限 THEN MusicServer SHALL 队列化或拒绝新任务，并返回明确状态。
7. IF 音频格式已经满足目标 profile THEN MusicServer SHALL 可直接复用原始文件，不强制转码。

### Requirement 4: 服务端转码产物缓存

**User Story:** 作为自托管用户，我希望转码后的音频可以在服务端缓存，以便重复播放同一歌曲时减少 CPU 消耗和等待时间。

#### Acceptance Criteria

1. WHEN 转码产物已存在且仍与源文件 checksum 匹配 THEN MusicServer SHALL 直接返回缓存产物。
2. WHEN 服务端缓存达到容量限制 THEN MusicServer SHALL 按可配置策略清理旧转码产物，且不得删除原始上传文件。
3. WHEN 用户删除音乐文件 THEN MusicServer SHALL 删除或标记清理该音乐关联的所有转码产物。
4. WHEN 用户登出或 token 失效 THEN MusicServer SHALL 不影响服务端转码产物本身，但后续访问仍必须重新鉴权。
5. IF 缓存目录不可写或磁盘空间不足 THEN MusicServer SHALL 禁用或暂停新增转码缓存，并向客户端返回可理解错误。
6. WHEN MusicServer 启动 THEN 系统 SHALL 能识别已有转码缓存并校验其归属和源文件一致性。

### Requirement 5: 桌面端离线缓存策略

**User Story:** 作为桌面端用户，我希望可以将 MusicServer 中的私有音乐缓存到本机，以便无网络时仍能播放已缓存歌曲。

#### Acceptance Criteria

1. WHEN 用户选择缓存单曲、歌单或收藏 THEN MusicPlayer SHALL 将对应私有音乐加入离线缓存队列。
2. WHEN 下载离线缓存 THEN MusicPlayer SHALL 使用受鉴权保护的 MusicServer 流媒体地址，并支持断点续传或重新校验。
3. WHEN 某首歌曲已完整缓存且 checksum 匹配 THEN MusicPlayer SHALL 优先播放本地缓存文件。
4. IF 本地缓存不完整、checksum 不匹配或源文件已变化 THEN MusicPlayer SHALL 重新下载或回退到在线播放。
5. WHEN 用户退出登录或切换 MusicServer 账号 THEN MusicPlayer SHALL 防止继续访问上一账号的私有缓存，至少隔离缓存索引和播放入口。
6. WHEN 用户删除缓存 THEN MusicPlayer SHALL 删除对应缓存文件和索引，但不得删除 MusicServer 上的原始音乐。
7. WHEN 缓存空间达到用户设置上限 THEN MusicPlayer SHALL 按可预测策略清理可清理缓存，并保留用户显式固定的离线内容。

### Requirement 6: Android 端离线缓存策略

**User Story:** 作为 Android 用户，我希望可以在手机上缓存私有音乐，以便通勤、弱网或无网络时播放 MusicServer 曲库。

#### Acceptance Criteria

1. WHEN 用户选择缓存单曲、歌单或收藏 THEN AndroidMusicPlayer SHALL 将对应私有音乐加入离线下载队列。
2. WHEN Android 端缓存私有音乐 THEN 系统 SHALL 使用后台任务或播放器生态兼容的下载机制，避免阻塞 UI。
3. WHEN 歌曲已完整缓存且与 MusicServer checksum 匹配 THEN AndroidMusicPlayer SHALL 优先使用本地缓存播放。
4. IF 设备处于移动网络且用户设置为仅 Wi-Fi 缓存 THEN AndroidMusicPlayer SHALL 暂停新的离线下载。
5. IF token 过期或登录状态失效 THEN AndroidMusicPlayer SHALL 暂停需要鉴权的下载并提示用户重新登录。
6. WHEN 用户退出登录或切换账号 THEN AndroidMusicPlayer SHALL 隔离或清理上一账号的私有缓存索引，避免跨账号展示。
7. WHEN 系统存储不足 THEN AndroidMusicPlayer SHALL 暂停下载、保留已完成缓存，并向用户展示可恢复状态。

### Requirement 7: 缓存一致性与失效

**User Story:** 作为用户，我希望离线缓存始终对应我当前账号和最新曲库，以便不会播放到已删除、被替换或无权限的内容。

#### Acceptance Criteria

1. WHEN MusicServer 返回音乐列表或详情 THEN 响应 SHALL 包含可用于缓存校验的稳定字段，如 `musicId`、`checksum`、`fileSize`、`contentType` 和更新时间或版本信息。
2. WHEN 客户端刷新私有曲库 THEN 客户端 SHALL 对比本地缓存索引与服务端元数据，标记过期、缺失或仍有效的缓存。
3. IF 用户在服务端删除音乐 THEN 客户端下一次同步 SHALL 将对应离线缓存标记为不可播放或待清理。
4. IF 用户没有某首音乐权限 THEN 客户端 SHALL 不通过离线索引继续展示或播放该私有内容。
5. WHEN 客户端处于离线状态 THEN 已验证且未过期的缓存 SHALL 可继续播放，并在恢复网络后重新校验。
6. WHEN token、服务器地址或账号发生变化 THEN 客户端 SHALL 使用独立缓存命名空间，避免混用不同服务或不同用户的数据。

### Requirement 8: 用户控制与可见性

**User Story:** 作为用户或自托管维护者，我希望能看到播放、转码和缓存状态，以便理解系统当前行为并处理异常。

#### Acceptance Criteria

1. WHEN 客户端正在下载离线缓存 THEN 客户端 SHALL 展示进度、状态、失败原因和重试入口。
2. WHEN 服务端正在准备转码版本 THEN 客户端 SHALL 展示“准备中”或等价状态，而不是表现为播放失败。
3. WHEN 用户查看 MusicServer 私有曲库 THEN 客户端 SHALL 能区分在线播放、已缓存、缓存过期、转码不可用等状态。
4. WHEN 管理者配置 MusicServer THEN 系统 SHALL 支持配置转码开关、转码工具路径、缓存目录、缓存容量和并发限制。
5. WHEN 发生 Range、转码或缓存相关错误 THEN 系统 SHALL 记录足够诊断信息，同时避免记录明文 token 和本地敏感路径。
6. IF 某能力在当前平台不可用 THEN 客户端 SHALL 清晰降级，不展示不可执行的操作入口。

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: Range 响应、转码任务、转码缓存、桌面离线缓存、Android 离线缓存应分别由清晰模块承担，避免堆叠在现有 Controller 或 UI 文件中。
- **Modular Design**: MusicServer 应把流媒体响应、转码 profile、转码任务队列、缓存索引和文件清理分离；客户端应把缓存索引、下载队列、播放 URL 选择和 UI 状态分离。
- **Dependency Management**: FFmpeg 或等价工具必须作为可选外部依赖处理；Android 应优先复用 Media3/ExoPlayer 生态能力；桌面端应优先复用现有下载、文件和播放器服务。
- **Clear Interfaces**: MusicServer API 必须为客户端提供稳定的播放能力、缓存校验和转码状态字段，避免客户端解析响应头之外的隐式行为。

### Performance
- MusicServer SHALL NOT 将大音频文件整体读入内存来响应 Range 或转码缓存请求。
- Range 响应 SHALL 以流式方式读取文件，并支持大文件播放。
- 转码任务 SHALL 有并发上限，默认配置应适合个人服务器或 NAS。
- 客户端缓存下载 SHALL 不阻塞当前播放、主窗口或 Android UI 线程。

### Security
- 所有原始流、转码流和离线缓存下载入口 SHALL 保持用户鉴权和归属校验。
- 通过 URL 参数携带 token 的兼容方案 SHALL 仅用于播放器无法设置请求头的场景，并尽量减少日志暴露。
- 离线缓存索引 SHALL 按服务器地址和用户隔离。
- 服务端错误日志 SHALL 避免输出明文 token、用户密码、完整本地敏感路径。

### Reliability
- 客户端 SHALL 能从下载中断、网络切换、应用重启中恢复离线缓存任务。
- MusicServer SHALL 能从转码任务失败、缓存目录缺失、转码工具不可用中降级恢复。
- 客户端 SHALL 在服务端不可用时继续播放已验证离线缓存。
- 删除音乐、退出登录、切换账号和源文件变化 SHALL 有明确缓存失效策略。

### Usability
- 用户 SHALL 能清楚知道歌曲当前是在线播放、转码播放、正在缓存、已离线或缓存失效。
- 用户 SHALL 能手动缓存和移除缓存单曲、歌单或收藏。
- 用户 SHALL 能设置离线缓存空间上限和移动网络下载偏好。
- 自托管维护者 SHALL 能通过配置文件理解并调整转码和缓存行为。
