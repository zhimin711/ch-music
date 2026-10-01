# Android 打包与发布

工作流：仓库根目录 `.github/workflows/android-release.yml`，名称 **Android Package and Release**。

## 签名配置

在 GitHub 仓库 Settings → Secrets and variables → Actions 中添加：

| 类型 | 名称 | 内容 |
| --- | --- | --- |
| Secret | SIGNING_KEYSTORE_GITHUB | 正式签名 keystore 文件的 Base64 编码 |
| Secret | SIGNING_PROPERTIES_GITHUB | 以下 properties 文件的 Base64 编码 |
| Variable（可选） | MUSIC_SERVER_BASE_URL | 编译进 APK 的后端地址，例如 https://music.example.com |

properties 内容：

```properties
storeFile=../retro.keystore
storePassword=你的证书密码
keyAlias=你的密钥别名
keyPassword=你的密钥密码
```

`storeFile` 相对于 `AndroidMusicPlayer/app`；工作流会将 keystore 写入 `AndroidMusicPlayer/retro.keystore`。密码中的反斜杠等特殊字符需遵守 Java properties 转义规则。

Linux 编码示例（macOS 使用 `base64 < 文件 | tr -d '\\n'`）：

```bash
base64 -w 0 release.keystore
base64 -w 0 signing.properties
```

将编码结果粘贴到对应 Secret。不要提交证书、properties 或编码内容。后续升级必须使用同一签名证书。
缺少任何签名 Secret 时，工作流直接失败，不使用项目默认的 debug 签名回退。

未配置 MUSIC_SERVER_BASE_URL 时沿用项目 release 默认地址 `https://music-server.example.invalid`；部署自己的后端时应设置此 Variable。

## 手动打包

工作流文件需先合并到仓库默认分支，GitHub Actions 页面才会提供 workflow_dispatch 的 Run workflow 入口。合并后：

1. Actions → Android Package and Release → Run workflow。
2. 选择需要构建的分支，例如 `20260813-optmize-ui`。
3. `publish=false`：只打包，APK 和 SHA256SUMS.txt 保存在运行的 Artifacts，保留 14 天。
4. `publish=true`：构建成功后创建 GitHub Release 和标签；`prerelease=true` 标记为预发布。

版本自动读取 `AndroidMusicPlayer/app/build.gradle.kts` 的 versionName。手动发布遇到已存在的标签或 Release 会失败，不覆盖已有版本。

## 标签发布

不等待默认分支合并也可以从已包含工作流的提交推送标签触发发布：

```bash
git fetch origin
git switch 20260813-optmize-ui
git pull --ff-only origin 20260813-optmize-ui
# 先确认 versionName=6.6.0；后续版本同时递增 versionCode。
git tag android-v6.6.0
git push origin android-v6.6.0
```

只匹配 `android-v*` 标签，避免与桌面端 `v*` 标签混淆。标签必须匹配 versionName；带连字符的版本自动标记为预发布。所有标签发布都使用标签指向的源码。

每次发布前修改 versionName 并递增 versionCode，且提交版本修改后再打标签。工作流不会自动改版本。

## 产物与验证

- `ch-music-android-normal-版本.apk`：normal 渠道。
- `ch-music-android-fdroid-版本.apk`：fdroid 渠道。
- `SHA256SUMS.txt`：APK 的 SHA-256 校验值。

构建环境：Ubuntu 24.04、Java 21、仓库 Gradle Wrapper、Android API 35。
执行 `:app:assembleNormalRelease :app:assembleFdroidRelease`，包含项目 release 的 R8/资源压缩，并使用 apksigner 验证两个 APK 的签名。
两个渠道使用相同 applicationId，不能作为两个独立应用同时安装。

无需额外 GitHub Token；发布 job 使用 GITHUB_TOKEN 的 contents:write 权限。
签名文件在构建结束后清理，上传范围仅包含两个 APK 和校验文件。

原 `AndroidMusicPlayer/.github/workflows/` 是子项目遗留文件，GitHub 不会将其作为当前仓库工作流执行。
