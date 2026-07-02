package code.name.monkey.retromusic.network.models

import com.google.gson.annotations.SerializedName

// ==================== 基础响应包装 ====================

/**
 * 网易云 API 标准响应格式
 */
data class NeteaseResponse<T>(
    val code: Int,
    val data: T? = null
)

// ==================== Banner / 轮播图 ====================

data class NeteaseBannerResponse(
    val code: Int,
    val banners: List<BannerItem>?
)

data class BannerItem(
    val imageUrl: String,
    val targetId: Long,
    val targetType: Int,
    val typeTitle: String?,
    val url: String?
)

// ==================== 推荐歌单 ====================

data class NeteasePersonalizedResponse(
    val code: Int,
    val result: List<PersonalizedPlaylist>?
)

data class PersonalizedPlaylist(
    val id: Long,
    val name: String,
    val picUrl: String,
    val playCount: Long,
    val trackCount: Int,
    val type: Int,
    val copywriter: String?
)

// ==================== 热门歌手 ====================

data class NeteaseHotSingerResponse(
    val code: Int,
    val artists: List<HotArtist>?
)

data class HotArtist(
    val id: Long,
    val name: String,
    val picUrl: String?,
    val img1v1Url: String?,
    val alias: List<String>?,
    val albumSize: Int,
    val followed: Boolean?
)

// ==================== 新歌推荐 ====================

data class NeteaseNewSongResponse(
    val code: Int,
    val result: List<NewSong>?
)

data class NewSong(
    val id: Long,
    val name: String,
    val picUrl: String,
    val song: NeteaseSong?,
    val artists: List<NeteaseArtist>?
)

// ==================== 新碟上架 ====================

data class NeteaseNewAlbumResponse(
    val code: Int,
    val albums: List<NeteaseAlbum>?,
    val total: Int?
)

data class NeteaseAlbum(
    val id: Long,
    val name: String,
    val picUrl: String,
    val artist: NeteaseArtist?,
    val artists: List<NeteaseArtist>?,
    val publishTime: Long,
    val size: Int
)

// ==================== 歌单分类 ====================

data class NeteasePlaylistCategoryResponse(
    val code: Int,
    val sub: List<PlaylistCategory>?,
    val categories: Map<String, String>?
)

data class PlaylistCategory(
    val name: String,
    val category: Int,
    val hot: Boolean?
)

// ==================== 歌单列表 ====================

data class NeteasePlaylistListResponse(
    val code: Int,
    val playlists: List<PlaylistItem>?,
    val total: Int,
    val more: Boolean,
    val cat: String?
)

data class PlaylistItem(
    val id: Long,
    val name: String,
    val coverImgUrl: String,
    val playCount: Long,
    val trackCount: Int,
    val creator: PlaylistCreator?,
    val description: String?
)

data class PlaylistCreator(
    val nickname: String,
    val avatarUrl: String,
    val userId: Long
)

// ==================== 歌单详情 ====================

data class NeteasePlaylistDetailResponse(
    val code: Int,
    val playlist: PlaylistDetail?
)

data class PlaylistDetail(
    val id: Long,
    val name: String,
    val coverImgUrl: String,
    val description: String?,
    val playCount: Long,
    val trackCount: Int,
    val createTime: Long,
    val updateTime: Long,
    val tracks: List<NeteaseSong>?,
    val trackIds: List<TrackId>?,
    val creator: PlaylistCreator?
)

data class TrackId(
    val id: Long
)

// ==================== 排行榜 ====================

data class NeteaseToplistResponse(
    val code: Int,
    val list: List<ToplistItem>?
)

data class ToplistItem(
    val id: Long,
    val name: String,
    val coverImgUrl: String,
    val updateFrequency: String?,    // 更新频率，如 "每天更新"
    val playCount: Long,
    val trackCount: Int,
    val description: String?
)

// ==================== 通用实体 ====================

data class NeteaseArtist(
    val id: Long,
    val name: String,
    val picUrl: String?
)

data class NeteaseSong(
    val id: Long,
    val name: String,
    val ar: List<NeteaseArtist>?,    // artists
    val al: NeteaseAlbum?,            // album
    val dt: Long?,                    // duration in ms
    // ---- 音质相关（可选，用于推导 song/url/v1 的 level & encodeType）----
    val privilege: NeteasePrivilege? = null,
    val hrMusic: NeteaseMusicQuality? = null,   // hires
    val sqMusic: NeteaseMusicQuality? = null,   // lossless
    val hMusic: NeteaseMusicQuality? = null,    // exhigh
    val mMusic: NeteaseMusicQuality? = null,    // higher
    val lMusic: NeteaseMusicQuality? = null     // standard
)

/**
 * 网易云单首歌的权限/可用音质信息（song/detail 或歌单 tracks 里返回）。
 * 我们只关心播放侧字段。
 */
data class NeteasePrivilege(
    val plLevel: String?,         // 当前账号该歌实际可播等级 (standard/higher/exhigh/lossless/hires/sky/...)
    val playMaxBrLevel: String?,  // 理论最高等级
    val playMaxbr: Long?,         // 最大比特率 (bps)
    val maxbr: Long?              // 兜底：最大比特率
)

/**
 * 一档音质的资源信息（hMusic/mMusic/sqMusic/hrMusic 等）。
 * 主要用 extension 判定 encodeType (flac/mp3)。
 */
data class NeteaseMusicQuality(
    val bitrate: Long?,
    val extension: String?,       // "flac" / "mp3" 等
    val size: Long?
)
