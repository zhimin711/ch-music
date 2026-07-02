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
     * 实现：**逐首**调用 [resolveSong]。后端 song/url 端点在多 id 情况下响应体
     * 会出现 Jackson 无法解析的 trailing token（`}{` 等），所以宁可多 N 次请求，
     * 也不做多 id 拼接。
     *
     * 返回：与输入对应的歌曲列表（失败的歌曲保留原状）
     */
    suspend fun resolveSongs(songs: List<Song>): List<Song> {
        if (songs.isEmpty()) return songs
        return songs.map { song -> resolveSong(song) ?: song }
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
