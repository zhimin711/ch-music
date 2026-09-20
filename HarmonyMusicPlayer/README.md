# CH Music HarmonyOS NEXT

CH Music 的 HarmonyOS NEXT 原生客户端，使用 ArkTS + ArkUI，对接仓库现有的 MusicServer REST API。

## 已实现

- MusicServer 登录与注册，服务地址可配置。
- access token 与用户信息使用 Preferences 持久化。
- 私有曲库、收藏、歌单读取。
- 收藏/取消收藏、新建歌单的基础操作。
- 基于 Media Kit `AVPlayer` 的原生音频播放。
- 通过 `media.createMediaSourceWithUrl(url, headers)` 注入 Bearer Token，直接播放 MusicServer 的鉴权 HTTP Range 流。
- 播放队列、上一首/下一首、seek、播放完成自动下一首。
- AVSession 接入系统播控中心，支持系统播放/暂停/上一首/下一首/seek，并同步标题、歌手、专辑、时长和播放位置。
- 手机、平板、2in1 Stage 模型工程骨架。

## 工程要求

- DevEco Studio，HarmonyOS SDK。
- 最低兼容 API 12。
- target 配置为 HarmonyOS 6.0.0(20)。如本机 SDK 不包含该版本，可按 DevEco Studio 配套 SDK 调整 `build-profile.json5`。
- MusicServer 需可被真机访问。真机不能使用电脑的 `127.0.0.1`，请在登录页填写局域网或公网 HTTPS 地址。

## 运行

1. 启动仓库根目录的 `MusicServer`。
2. 用 DevEco Studio 打开 `HarmonyMusicPlayer/`。
3. 配置自动签名。
4. 运行 `entry` 到 HarmonyOS NEXT 设备。
5. 首次启动输入 MusicServer 地址、用户名和密码。

## 与 MusicServer 的契约

客户端当前直接使用：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`
- `GET /api/music`
- `GET /api/music/{id}/stream`
- `GET /api/favorites`
- `POST /api/favorites/{musicId}`
- `DELETE /api/favorites/{musicId}`
- `GET /api/playlists`
- `POST /api/playlists`

播放时不会把 token 放进 URL。Bearer Token 通过 MediaSource 的 HTTP headers 传递，因此服务端现有鉴权和 Range 语义无需修改。

## 下一阶段

当前提交优先落地“可登录、可浏览、可原生播放、可系统播控”的第一阶段闭环。离线缓存、上传音乐、歌单详情编辑、转码 profile 选择、封面鉴权加载、歌词与跨设备能力可在此架构上继续补齐。
