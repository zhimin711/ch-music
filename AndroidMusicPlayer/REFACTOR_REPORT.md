# 朝华音乐 Android 端重构实施报告

## 项目概述
基于电脑端网易云音乐架构的 Android 端重构，实现了首页推荐、歌单分类、排行榜等功能。

## 已完成功能

### 1. 架构层
- ✅ 网易云 API 接口层 (`NeteaseCloudApi.kt`)
- ✅ 数据模型层 (`NeteaseModels.kt`, `NeteaseSongUrlModels.kt`)
- ✅ 数据仓库层 (`NeteaseRepository.kt`)
- ✅ ViewModel 层 (`HomeViewModel.kt`)
- ✅ Koin 依赖注入配置

### 2. 首页框架
- ✅ `HomeFragment.kt` - ViewPager2 + TabLayout 三栏布局
- ✅ 推荐 Tab: `HomeRecommendFragment.kt`
- ✅ 歌单 Tab: `SonglistFragment.kt`
- ✅ 排行榜 Tab: `ToplistFragment.kt`

### 3. 推荐页组件
- ✅ Hero 每日推荐卡片区域
- ✅ 快捷入口（漫游/雷达/定制）
- ✅ 推荐歌单 3 列网格
- ✅ 热门歌手横向滚动列表
- ✅ 新歌推荐列表

### 4. 歌单详情页
- ✅ 折叠式 Toolbar + 封面图
- ✅ 歌单基本信息（名称、歌曲数、播放次数）
- ✅ 歌曲列表展示
- ✅ 播放/随机播放按钮
- ✅ 错误状态展示

### 5. 适配器
- ✅ `HomePlaylistCardAdapter.kt` - 歌单卡片
- ✅ `HomeArtistAdapter.kt` - 歌手头像
- ✅ `ToplistAdapter.kt` - 排行榜卡片

### 6. 品牌重命名
- ✅ Glide 模块重命名 (ZhaohuaGlideModule)
- ✅ 备份目录重命名 (ZhaohuaMusic/Backups)
- ✅ WakeLock 标签重命名
- ✅ GitHub 项目链接更新

已完成的构建错误修复：

1. ✅ **重复字符串资源** - `toplist_desc`, `recommended_playlists`, `hot_artists`, `new_songs` 重复定义
2. ✅ **XML 命名空间缺失** - `item_home_artist.xml` 缺少 `xmlns:app` 命名空间
3. ✅ **布局属性不匹配** - `item_home_playlist_card.xml` 和 `item_toplist_card.xml` 的 LinearLayout 根布局使用 ConstraintLayout 的 `layout_constraintDimensionRatio` 属性，已改为 ConstraintLayout 根布局
4. ✅ **缺少字符串资源** - 添加 `toplist` 字符串
5. ✅ **缺少 Drawable 资源** - 创建 `ic_error_outline.xml` 错误图标
6. ✅ **自定义视图属性缺失** - 移除不存在的 `RetroShapeableImageView` 和 `retroCircle`/`retroCornerSize` 属性，改用标准 `ImageView` + Glide 转换（`circleCrop()` 和 `RoundedCorners`）
7. ✅ **Glide 引用错误** - 修复 `GlideApp` 未解析引用错误，改用标准 `Glide.with()` API
8. ✅ **when 表达式非 exhaustive** - 为所有 `Result` 的 `when` 表达式添加 `Loading` 分支
9. ✅ **Fragment 基类继承错误** - `SonglistDetailFragment` 从 `AbsMainActivityFragment` 改为 `Fragment`，移除不存在的 `mainActivity` 引用
10. ✅ **SongAdapter 构造函数参数不匹配** - 修复 `songs` -> `dataSet`, `layoutRes` -> `itemLayoutRes`，移除 lambda 参数
11. ✅ **Result.Error 属性名错误** - `exception` -> `error`
12. ✅ **ProgressIndicator 类不存在** - 改为标准 `ProgressBar`
13. ✅ **GsonConverterFactory 包名错误** - 使用 `retrofit2.converter.gson.GsonConverterFactory` 而非 `com.google.gson.GsonConverterFactory`

## 核心数据流

```
用户点击歌单卡片
    ↓
Fragment 接收点击事件
    ↓
创建 SonglistDetailFragment 传递参数
    ↓
HomeViewModel.loadPlaylistDetail(playlistId)
    ↓
NeteaseRepository.getPlaylistDetail(id)
    ↓
NeteaseCloudApi.getPlaylistDetail(id)
    ↓
API 返回数据 → 展示歌单详情和歌曲列表
    ↓
用户点击歌曲 → 跳转到播放器
```

## 后续需要完善的功能

### Phase 4-1: 在线播放集成 (高优先级)

**问题**: 当前 `Song` 模型的 `data` 字段是本地文件路径，网易云歌曲需要通过 API 动态获取播放 URL。

**解决方案**:
1. **创建扩展数据模型**
   ```kotlin
   data class NetEaseSong(
       val song: Song,           // 基础 Song 对象
       val playUrl: String?      // 播放 URL，懒加载
   )
   ```

2. **修改播放服务** (`MusicService.kt`)
   - 添加对 HTTP URL 的支持
   - 实现播放前预取 URL 的机制
   - 处理播放失败重试逻辑

3. **添加播放 URL 缓存**
   - Room 缓存歌曲播放 URL（24 小时过期）
   - 播放前优先使用缓存，过期重新获取

4. **在 SonglistDetailFragment 中实现**
   ```kotlin
   // 点击播放按钮时预取所有歌曲 URL
   private suspend fun prefetchSongUrls(songs: List<Song>) {
       val songIds = songs.map { it.id }
       val urlMap = neteaseRepository.getSongsUrl(songIds)
       // 保存到缓存或更新歌曲
   }
   ```

### Phase 4-2: 用户登录和个性化推荐 (中优先级)

1. **登录页面**
   - 手机号+密码登录
   - Cookie 登录
   - 二维码登录

2. **Cookie 管理**
   - 使用 SharedPreferences 存储 Cookie
   - 在 OkHttp 拦截器中自动添加 Cookie

3. **个性化功能**
   - 每日推荐（根据用户口味）
   - 私人 FM
   - 喜欢的音乐同步

### Phase 4-3: 错误状态和骨架屏 (Phase 3-3, 中优先级)

1. **骨架屏加载状态**
   - 推荐歌单网格骨架
   - 歌手列表骨架
   - 歌单详情页骨架

2. **空状态页面**
   - 网络错误提示 + 重试按钮
   - 数据为空提示
   - 未登录提示

3. **下拉刷新**
   - 使用 SwipeRefreshLayout
   - 各 Tab 独立刷新

### Phase 4-4: 图片加载优化 (低优先级)

1. **颜色提取**
   - 使用 Palette API 从专辑封面提取颜色
   - 实现沉浸式渐变效果

2. **图片缓存策略**
   - Glide 缓存配置优化
   - 缩略图预加载

### Phase 4-5: 更多在线功能 (低优先级)

1. **歌手详情页**
   - 歌手热门歌曲
   - 歌手专辑列表
   - 歌手简介

2. **专辑详情页**
   - 专辑歌曲列表
   - 专辑介绍

3. **搜索功能**
   - 热门搜索
   - 搜索历史
   - 搜索建议

4. **评论功能**
   - 歌曲评论
   - 歌单评论

## 技术债务和注意事项

### 1. API 代理问题
- 网易云官方 API 可能存在跨域或访问限制
- 建议自建 API 代理服务（参考电脑端的 NeteaseCloudMusicApi）
- 在 Retrofit 中配置 baseURL 支持动态切换

### 2. 版权问题
- 网易云歌曲播放需要登录且有版权限制
- 部分灰色歌曲可能无法播放
- 建议添加播放失败时的友好提示

### 3. 本地与在线切换
- 当前实现了在线音乐功能
- 需要考虑与本地音乐库的整合
- 建议添加设置项让用户选择优先播放源

### 4. 性能优化
- 歌单图片大量加载时的内存管理
- 列表滚动时的图片加载暂停
- RecyclerView 缓存池配置

## 文件清单

### 新增文件 (17 个)
```
app/src/main/java/code/name/monkey/retromusic/
├── network/
│   ├── NeteaseCloudApi.kt
│   └── models/
│       ├── NeteaseModels.kt
│       └── NeteaseSongUrlModels.kt
├── repository/
│   └── NeteaseRepository.kt
├── viewmodel/
│   └── HomeViewModel.kt
├── adapter/home/
│   ├── HomePlaylistCardAdapter.kt
│   ├── HomeArtistAdapter.kt
│   └── ToplistAdapter.kt
└── fragments/
    ├── home/
    │   └── HomeRecommendFragment.kt
    ├── songlist/
    │   ├── SonglistFragment.kt
    │   └── SonglistDetailFragment.kt
    └── toplist/
        └── ToplistFragment.kt

app/src/main/res/
├── layout/
│   ├── fragment_home_recommend.xml
│   ├── fragment_songlist.xml
│   ├── fragment_toplist.xml
│   ├── fragment_songlist_detail.xml
│   ├── item_home_playlist_card.xml
│   ├── item_home_artist.xml
│   └── item_toplist_card.xml
```

### 修改文件 (8 个)
- `MainModule.kt` - Koin 注入配置
- `HomeFragment.kt` - ViewPager 适配器更新
- `RetrofitClient.kt` - 网易云 OkHttp 配置
- `BackupHelper.kt` - 目录重命名
- `LyricUtil.kt` - 目录重命名
- `MediaButtonIntentReceiver.kt` - Wakelock 标签
- `Constants.kt` - 项目链接更新
- `BugReportActivity.kt` - Issue 链接更新

## 总结

当前已完成网易云音乐在线功能的基础架构搭建，包括：
- API 层完整封装
- 首页三栏布局实现
- 歌单详情页基础框架
- 品牌标识统一

核心缺失功能是**在线歌曲播放**，需要对现有播放服务进行扩展以支持 HTTP 流播放。建议优先完成播放功能集成，然后逐步完善错误处理、骨架屏等用户体验优化。
