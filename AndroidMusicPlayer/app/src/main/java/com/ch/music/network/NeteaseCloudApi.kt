package com.ch.music.network

import com.ch.music.network.models.NeteaseArtistAlbumsResponse
import com.ch.music.network.models.NeteaseArtistDetailResponse
import com.ch.music.network.models.NeteaseArtistSongsResponse
import com.ch.music.network.models.NeteaseToplistResponse
import com.ch.music.network.models.NeteaseBannerResponse
import com.ch.music.network.models.NeteasePersonalizedResponse
import com.ch.music.network.models.NeteaseSearchResponse
import com.ch.music.network.models.NeteaseHotSingerResponse
import com.ch.music.network.models.NeteaseNewSongResponse
import com.ch.music.network.models.NeteasePlaylistCategoryResponse
import com.ch.music.network.models.NeteasePlaylistListResponse
import com.ch.music.network.models.NeteasePlaylistDetailResponse
import com.ch.music.network.models.NeteaseNewAlbumResponse
import com.ch.music.network.models.NeteaseSongUrlResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 网易云音乐 API 接口
 * 对标电脑端 src/renderer/api/home.ts 和 list.ts
 */
interface NeteaseCloudApi {


    // ==================== 搜索相关 ====================

    /**
     * 搜索网易云歌曲。
     *
     * 对应 MusicServer 的 `/api/netease/public/search`，服务端会转发到
     * 网易云 `/cloudsearch`。type=1 表示单曲，和桌面端默认搜索类型一致。
     */
    @GET("search")
    suspend fun searchSongs(
        @Query("keywords") keywords: String,
        @Query("type") type: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): NeteaseSearchResponse

    // ==================== 首页相关 ====================

    /**
     * 获取轮播图
     * @param type 0: pc, 1: android, 2: iphone, 3: ipad
     */
    @GET("banner")
    suspend fun getBanners(@Query("type") type: Int = 1): NeteaseBannerResponse

    /**
     * 获取推荐歌单
     * @param limit 返回数量
     */
    @GET("personalized")
    suspend fun getPersonalizedPlaylist(
        @Query("limit") limit: Int = 18
    ): NeteasePersonalizedResponse

    /**
     * 获取热门歌手
     */
    @GET("top/artists")
    suspend fun getHotSinger(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 30
    ): NeteaseHotSingerResponse

    /**
     * 获取新歌推荐
     */
    @GET("personalized/newsong")
    suspend fun getRecommendMusic(
        @Query("limit") limit: Int = 12
    ): NeteaseNewSongResponse

    /**
     * 获取新碟上架
     */
    @GET("album/newest")
    suspend fun getNewAlbum(): NeteaseNewAlbumResponse

    /**
     * 获取新碟上架（带分页参数）
     */
    @GET("top/album")
    suspend fun getTopAlbum(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("area") area: String? = null
    ): NeteaseNewAlbumResponse

    // ==================== 歌单相关 ====================

    /**
     * 获取歌单分类
     */
    @GET("playlist/catlist")
    suspend fun getPlaylistCategory(): NeteasePlaylistCategoryResponse

    /**
     * 根据分类获取歌单列表
     * @param cat 分类，如 "华语"、"流行"，空字符串表示全部
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @GET("top/playlist")
    suspend fun getListByCat(
        @Query("cat") cat: String = "",
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): NeteasePlaylistListResponse

    /**
     * 获取歌单详情
     * @param id 歌单 ID
     */
    @GET("playlist/detail")
    suspend fun getPlaylistDetail(
        @Query("id") id: Long
    ): NeteasePlaylistDetailResponse

    // ==================== 排行榜相关 ====================

    /**
     * 获取排行榜列表
     */
    @GET("toplist")
    suspend fun getToplist(): NeteaseToplistResponse

    // ==================== 歌手相关 ====================

    /**
     * 获取歌手详情
     * 后端：/api/netease/public/artist?id=
     */
    @GET("artist")
    suspend fun getArtistDetail(
        @Query("id") id: Long
    ): NeteaseArtistDetailResponse

    /**
     * 获取歌手热门歌曲 / 全部歌曲
     * 后端：/api/netease/public/artist/songs?id=&order=hot&limit=&offset=
     */
    @GET("artist/songs")
    suspend fun getArtistTopSongs(
        @Query("id") id: Long,
        @Query("order") order: String = "hot",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): NeteaseArtistSongsResponse

    /**
     * 获取歌手专辑
     * 后端：/api/netease/public/artist/album?id=&limit=&offset=
     */
    @GET("artist/album")
    suspend fun getArtistAlbums(
        @Query("id") id: Long,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): NeteaseArtistAlbumsResponse

    // ==================== 歌曲播放相关 ====================

    /**
     * 获取歌曲播放 URL（v1 端点）
     *
     * 端点：`song/url/v1?id=<id>&level=higher&encodeType=flac`
     * - 单个 id：后端上游对多 id 响应体拼接后 Jackson 无法解析（trailing token），
     *   所以调用方只传单个 id
     * - level：音质等级，默认 higher
     * - encodeType：编码格式，默认 flac（服务端会回落到 mp3）
     */
    @GET("song/url/v1")
    suspend fun getSongUrl(
        @Query("id") id: String,
        @Query("level") level: String = "higher",
        @Query("encodeType") encodeType: String = "flac"
    ): NeteaseSongUrlResponse
}
