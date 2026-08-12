package com.ch.music.repository

import com.ch.music.network.NeteaseCloudApi
import com.ch.music.network.Result
import com.ch.music.util.logE
import com.ch.music.netease.NeteaseQualityCache
import com.ch.music.network.models.ArtistDetail
import com.ch.music.network.models.BannerItem
import com.ch.music.network.models.HotArtist
import com.ch.music.network.models.NeteaseAlbum
import com.ch.music.network.models.NeteasePlaylistCategoryResponse
import com.ch.music.network.models.NeteasePlaylistListResponse
import com.ch.music.network.models.NeteaseSong
import com.ch.music.network.models.NewSong
import com.ch.music.network.models.PersonalizedPlaylist
import com.ch.music.network.models.PlaylistDetail
import com.ch.music.network.models.ToplistItem

/**
 * 网易云音乐数据仓库
 * 负责处理网易云 API 的数据获取和简单缓存逻辑
 */
class NeteaseRepository(
    private val neteaseApi: NeteaseCloudApi
) {


    // ==================== 搜索 ====================

    suspend fun searchSongs(
        keywords: String,
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<NeteaseSong>> {
        return try {
            val response = neteaseApi.searchSongs(keywords, limit = limit, offset = offset)
            if (response.code == 200 && response.result != null) {
                Result.Success(response.result.songs.orEmpty())
            } else {
                Result.Error(IllegalStateException("Failed to search songs: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    // ==================== 首页数据 ====================

    suspend fun getBanners(type: Int = 1): Result<List<BannerItem>> {
        return try {
            val response = neteaseApi.getBanners(type)
            if (response.code == 200 && response.banners != null) {
                Result.Success(response.banners)
            } else {
                Result.Error(IllegalStateException("Failed to fetch banners: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    suspend fun getPersonalizedPlaylist(limit: Int = 18): Result<List<PersonalizedPlaylist>> {
        return try {
            val response = neteaseApi.getPersonalizedPlaylist(limit)
            if (response.code == 200 && response.result != null) {
                Result.Success(response.result)
            } else {
                Result.Error(IllegalStateException("Failed to fetch personalized playlists: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    suspend fun getHotSinger(offset: Int = 0, limit: Int = 30): Result<List<HotArtist>> {
        return try {
            val response = neteaseApi.getHotSinger(offset, limit)
            if (response.code == 200 && response.artists != null) {
                Result.Success(response.artists)
            } else {
                Result.Error(IllegalStateException("Failed to fetch hot singers: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    suspend fun getRecommendMusic(limit: Int = 12): Result<List<NewSong>> {
        return try {
            val response = neteaseApi.getRecommendMusic(limit)
            if (response.code == 200 && response.result != null) {
                Result.Success(response.result)
            } else {
                Result.Error(IllegalStateException("Failed to fetch recommend music: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    suspend fun getNewAlbum(): Result<List<NeteaseAlbum>> {
        return try {
            val response = neteaseApi.getNewAlbum()
            if (response.code == 200 && response.albums != null) {
                Result.Success(response.albums)
            } else {
                Result.Error(IllegalStateException("Failed to fetch new albums: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    // ==================== 歌单相关 ====================

    suspend fun getPlaylistCategory(): Result<NeteasePlaylistCategoryResponse> {
        return try {
            val response = neteaseApi.getPlaylistCategory()
            if (response.code == 200) {
                Result.Success(response)
            } else {
                Result.Error(IllegalStateException("Failed to fetch playlist categories: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    suspend fun getListByCat(
        cat: String = "",
        limit: Int = 30,
        offset: Int = 0
    ): Result<NeteasePlaylistListResponse> {
        return try {
            val response = neteaseApi.getListByCat(cat, limit, offset)
            if (response.code == 200) {
                Result.Success(response)
            } else {
                Result.Error(IllegalStateException("Failed to fetch playlist by category: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    suspend fun getPlaylistDetail(id: Long): Result<PlaylistDetail> {
        return try {
            val response = neteaseApi.getPlaylistDetail(id)
            if (response.code == 200 && response.playlist != null) {
                Result.Success(response.playlist)
            } else {
                Result.Error(IllegalStateException("Failed to fetch playlist detail: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    // ==================== 排行榜 ====================

    suspend fun getToplist(): Result<List<ToplistItem>> {
        return try {
            val response = neteaseApi.getToplist()
            if (response.code == 200 && response.list != null) {
                Result.Success(response.list)
            } else {
                Result.Error(IllegalStateException("Failed to fetch toplist: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    // ==================== 歌手 ====================

    suspend fun getArtistDetail(id: Long): Result<ArtistDetail> {
        return try {
            val response = neteaseApi.getArtistDetail(id)
            val artist = response.artist ?: response.data?.artist
            if (response.code == 200 && artist != null) {
                Result.Success(artist)
            } else {
                Result.Error(IllegalStateException("Failed to fetch artist detail: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    suspend fun getArtistTopSongs(
        id: Long,
        order: String = "hot",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<NeteaseSong>> {
        return try {
            val response = neteaseApi.getArtistTopSongs(id, order, limit, offset)
            if (response.code == 200 && response.songs != null) {
                Result.Success(response.songs)
            } else {
                Result.Error(IllegalStateException("Failed to fetch artist songs: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    suspend fun getArtistAlbums(
        id: Long,
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<NeteaseAlbum>> {
        return try {
            val response = neteaseApi.getArtistAlbums(id, limit, offset)
            if (response.code == 200 && response.hotAlbums != null) {
                Result.Success(response.hotAlbums)
            } else {
                Result.Error(IllegalStateException("Failed to fetch artist albums: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    // ==================== 歌曲播放 ====================

    /**
     * 获取单首歌曲的播放 URL
     *
     * 从 [NeteaseQualityCache] 取该歌的 level/encodeType，
     * 若无缓存则用默认值。
     */
    suspend fun getSongUrl(songId: Long): Result<String?> {
        return try {
            val opt = NeteaseQualityCache.get(songId)
            val response = neteaseApi.getSongUrl(
                id = songId.toString(),
                level = opt.level,
                encodeType = opt.encodeType
            )
            if (response.code == 200 && response.data != null) {
                val url = response.data.firstOrNull()?.url
                Result.Success(url)
            } else {
                Result.Error(IllegalStateException("Failed to fetch song url: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }

    /**
     * 批量获取多首歌曲的播放 URL
     *
     * 注意：后端 song/url 端点多 id 拼接返回体会让 Jackson trailing-token 报错，
     * 且不同歌需要不同 level/encodeType，因此这个方法**不再被生产代码调用**，
     * 走 [NeteasePlaybackManager] 内的逐首解析。保留以防外部依赖。
     */
    suspend fun getSongsUrl(songIds: List<Long>): Result<Map<Long, String>> {
        return try {
            val response = neteaseApi.getSongUrl(songIds.joinToString(","))
            if (response.code == 200 && response.data != null) {
                val urlMap = response.data
                    .filter { !it.url.isNullOrEmpty() }
                    .associate { it.id to it.url!! }
                Result.Success(urlMap)
            } else {
                Result.Error(IllegalStateException("Failed to fetch songs url: code ${response.code}"))
            }
        } catch (e: Exception) {
            logE(e)
            Result.Error(e)
        }
    }
}
