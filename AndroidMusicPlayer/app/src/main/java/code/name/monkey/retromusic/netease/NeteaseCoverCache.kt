package code.name.monkey.retromusic.netease

import java.util.concurrent.ConcurrentHashMap

/**
 * 网易云歌曲封面 URL 内存缓存
 *
 * 由于 [code.name.monkey.retromusic.model.Song] 模型本身不携带封面 URL 字段，
 * 而 MediaStore 的 albumArt content URI 对网易云的负数 albumId 无效
 * （会抛 UnsupportedOperationException），因此需要一个旁路缓存：
 *
 *  - 在 [NeteaseSongMapper.toSong] 转换时写入
 *  - 在 [code.name.monkey.retromusic.glide.RetroGlideExtension.getSongModel] 加载图片时读取
 *
 * 全局单例，进程存活期内有效，量级为歌单条数，无需持久化。
 */
object NeteaseCoverCache {

    private val cache = ConcurrentHashMap<Long, String>()

    /**
     * @param neteaseId 网易云原始歌曲 ID（不是 [code.name.monkey.retromusic.model.Song.id]）
     */
    fun put(neteaseId: Long, coverUrl: String?) {
        if (coverUrl.isNullOrBlank()) return
        cache[neteaseId] = coverUrl
    }

    /**
     * @param neteaseId 网易云原始歌曲 ID
     */
    fun get(neteaseId: Long): String? = cache[neteaseId]

    fun clear() = cache.clear()
}
