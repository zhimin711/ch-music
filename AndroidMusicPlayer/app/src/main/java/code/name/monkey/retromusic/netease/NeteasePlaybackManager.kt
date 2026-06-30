package code.name.monkey.retromusic.netease

import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.repository.NeteaseRepository

/**
 * 网易云播放管理器
 *
 * 负责：
 * 1. 单首 / 批量预取播放 URL
 * 2. URL 缓存检查，避免重复请求
 * 3. 将 NetEase 歌曲填充播放 URL 转化为可直接播放的 [Song]
 */
class NeteasePlaybackManager(
    private val repository: NeteaseRepository
) {

    /**
     * 给一个 Song 补齐播放 URL：若 data 已是 HTTP URL 则直接返回，
     * 否则查缓存 / 远程 API。
     *
     * 返回：成功时为带有 URL 的 Song，失败时为 null
     */
    suspend fun resolveSong(song: Song): Song? {
        if (song.data.startsWith("http://") || song.data.startsWith("https://")) {
            return song
        }
        val neteaseId = NeteaseSongMapper.neteaseIdFromSong(song) ?: return null

        // 1. 优先缓存
        NeteaseUrlCache.get(neteaseId)?.let { cachedUrl ->
            return NeteaseSongMapper.withPlayUrl(song, cachedUrl)
        }

        // 2. 远程请求
        return when (val result = repository.getSongUrl(neteaseId)) {
            is Result.Success -> {
                val url = result.data ?: return null
                NeteaseUrlCache.put(neteaseId, url)
                NeteaseSongMapper.withPlayUrl(song, url)
            }
            is Result.Error, is Result.Loading -> null
        }
    }

    /**
     * 批量预取一批歌曲的 URL，已有 URL 或非网易云歌曲直接返回原对象。
     * 缓存命中的歌曲不会触发远程请求。
     *
     * 返回：与输入对应的歌曲列表（失败的歌曲保留原状）
     */
    suspend fun resolveSongs(songs: List<Song>): List<Song> {
        if (songs.isEmpty()) return songs

        // 收集需要远程请求的歌曲 ID（排除已有 URL 与缓存命中）
        val pendingIds = mutableListOf<Long>()
        val cachedUrls = mutableMapOf<Long, String>()

        for (song in songs) {
            if (song.data.startsWith("http://") || song.data.startsWith("https://")) continue
            val neteaseId = NeteaseSongMapper.neteaseIdFromSong(song) ?: continue
            val cached = NeteaseUrlCache.get(neteaseId)
            if (cached != null) {
                cachedUrls[neteaseId] = cached
            } else {
                pendingIds += neteaseId
            }
        }

        // 远程批量获取
        val fetchedUrls: Map<Long, String> = if (pendingIds.isEmpty()) {
            emptyMap()
        } else {
            when (val result = repository.getSongsUrl(pendingIds)) {
                is Result.Success -> {
                    NeteaseUrlCache.putAll(result.data)
                    result.data
                }
                is Result.Error, is Result.Loading -> emptyMap()
            }
        }

        // 组装最终列表
        return songs.map { song ->
            if (song.data.startsWith("http://") || song.data.startsWith("https://")) return@map song
            val neteaseId = NeteaseSongMapper.neteaseIdFromSong(song) ?: return@map song
            val url = cachedUrls[neteaseId] ?: fetchedUrls[neteaseId]
            if (url != null) {
                NeteaseSongMapper.withPlayUrl(song, url)
            } else {
                song
            }
        }
    }

    /**
     * 仅过滤出可播放的歌曲（有 URL 的）
     */
    fun playableOnly(songs: List<Song>): List<Song> {
        return songs.filter {
            it.data.startsWith("http://") || it.data.startsWith("https://")
        }
    }
}
