package code.name.monkey.retromusic.network

import code.name.monkey.retromusic.network.models.NeteaseResponse
import code.name.monkey.retromusic.network.models.NeteaseToplistResponse
import code.name.monkey.retromusic.network.models.NeteaseBannerResponse
import code.name.monkey.retromusic.network.models.NeteasePersonalizedResponse
import code.name.monkey.retromusic.network.models.NeteaseHotSingerResponse
import code.name.monkey.retromusic.network.models.NeteaseNewSongResponse
import code.name.monkey.retromusic.network.models.NeteasePlaylistCategoryResponse
import code.name.monkey.retromusic.network.models.NeteasePlaylistListResponse
import code.name.monkey.retromusic.network.models.NeteasePlaylistDetailResponse
import code.name.monkey.retromusic.network.models.NeteaseNewAlbumResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 网易云音乐 API 接口
 * 对标电脑端 src/renderer/api/home.ts 和 list.ts
 */
interface NeteaseCloudApi {

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
    ): code.name.monkey.retromusic.network.models.NeteaseSongUrlResponse
}
