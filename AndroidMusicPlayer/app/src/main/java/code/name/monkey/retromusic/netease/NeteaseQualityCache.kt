package code.name.monkey.retromusic.netease

import code.name.monkey.retromusic.network.models.NeteaseSong
import java.util.concurrent.ConcurrentHashMap

/**
 * 网易云单首歌播放音质选项缓存
 *
 * 由 [NeteaseSongMapper.toSong] 从 privilege / hMusic / sqMusic / hrMusic 等字段
 * 推导出对应的 `song/url/v1` 参数 (`level` + `encodeType`)，供 [NeteasePlaybackManager]
 * 请求播放 URL 时读取。
 *
 * 缓存粒度：单曲。因为 level 依赖 privilege，是"当前账号在这首歌上"的能力，
 * 不是全局偏好。
 */
object NeteaseQualityCache {

    data class Option(val level: String, val encodeType: String)

    private val DEFAULT = Option(level = "higher", encodeType = "mp3")

    private val cache = ConcurrentHashMap<Long, Option>()

    fun put(neteaseId: Long, option: Option) {
        cache[neteaseId] = option
    }

    fun get(neteaseId: Long): Option = cache[neteaseId] ?: DEFAULT

    fun clear() = cache.clear()

    /**
     * 从 NeteaseSong 推导播放选项：
     * - level 优先取 privilege.plLevel（当前账号在该歌可播的实际等级）
     * - encodeType 由所选 level 对应的资源块的 extension 决定
     */
    fun deriveOption(song: NeteaseSong): Option {
        val level = song.privilege?.plLevel?.takeIf { it.isNotBlank() }
            ?: fallbackLevel(song)
            ?: DEFAULT.level

        val encodeType = when (level) {
            "hires", "sky", "jymaster" -> song.hrMusic?.extension ?: "flac"
            "lossless"                 -> song.sqMusic?.extension ?: "flac"
            "exhigh"                   -> song.hMusic?.extension ?: "mp3"
            "higher"                   -> song.mMusic?.extension ?: "mp3"
            "standard"                 -> song.lMusic?.extension ?: "mp3"
            "jyeffect"                 -> song.sqMusic?.extension ?: "flac"
            else                       -> song.hMusic?.extension ?: "mp3"
        }
        return Option(level = level, encodeType = encodeType)
    }

    /** privilege 缺失时，按可用资源块从高到低猜一个 level */
    private fun fallbackLevel(song: NeteaseSong): String? {
        return when {
            song.hrMusic != null -> "hires"
            song.sqMusic != null -> "lossless"
            song.hMusic != null  -> "exhigh"
            song.mMusic != null  -> "higher"
            song.lMusic != null  -> "standard"
            else                 -> null
        }
    }
}
