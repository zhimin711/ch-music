package code.name.monkey.retromusic.netease

import androidx.collection.LruCache
import java.util.concurrent.TimeUnit

/**
 * 网易云歌曲 URL 缓存
 *
 * 网易云返回的播放 URL 通常有 6-24 小时有效期，
 * 此缓存避免短时间内重复请求 API。
 */
object NeteaseUrlCache {

    private const val CACHE_SIZE = 500
    private val EXPIRY_MS = TimeUnit.HOURS.toMillis(6)

    private data class Entry(val url: String, val timestamp: Long)

    private val cache = LruCache<Long, Entry>(CACHE_SIZE)

    /**
     * 获取缓存的 URL，过期返回 null
     */
    @Synchronized
    fun get(songId: Long): String? {
        val entry = cache.get(songId) ?: return null
        if (System.currentTimeMillis() - entry.timestamp > EXPIRY_MS) {
            cache.remove(songId)
            return null
        }
        return entry.url
    }

    /**
     * 缓存歌曲 URL
     */
    @Synchronized
    fun put(songId: Long, url: String) {
        cache.put(songId, Entry(url, System.currentTimeMillis()))
    }

    /**
     * 批量缓存
     */
    @Synchronized
    fun putAll(urls: Map<Long, String>) {
        val now = System.currentTimeMillis()
        urls.forEach { (id, url) -> cache.put(id, Entry(url, now)) }
    }

    /**
     * 清空缓存
     */
    @Synchronized
    fun clear() {
        cache.evictAll()
    }
}
