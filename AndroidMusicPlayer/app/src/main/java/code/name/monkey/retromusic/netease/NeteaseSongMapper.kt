package code.name.monkey.retromusic.netease

import code.name.monkey.retromusic.model.Song
import kotlin.math.abs

/**
 * 网易云歌曲映射工具
 *
 * 类似 MusicServerSongMapper：将网易云歌曲转为统一 [Song] 对象，
 * 保留 ID 反向映射机制以区分本地 / 远程歌曲。
 */
object NeteaseSongMapper {

    /** 用于区分网易云歌曲 ID 与本地歌曲 ID 的偏移量 */
    private const val NETEASE_ID_OFFSET = -8_000_000_000L

    private const val UNKNOWN_ARTIST = "未知歌手"
    private const val UNKNOWN_ALBUM = "未知专辑"
    private const val UNKNOWN_TITLE = "未知歌曲"

    /**
     * 是否网易云在线歌曲（URL 为 HTTP 流且 ID 在网易云 ID 区间）
     */
    fun isNeteaseSong(song: Song): Boolean {
        return song.id < NETEASE_ID_OFFSET && (song.data.startsWith("http://") || song.data.startsWith("https://"))
    }

    /**
     * 根据 Song 反推出网易云原始歌曲 ID
     * 不是网易云歌曲返回 null
     */
    fun neteaseIdFromSong(song: Song): Long? {
        if (song.id >= NETEASE_ID_OFFSET) return null
        return NETEASE_ID_OFFSET - song.id
    }

    /**
     * 将网易云原始歌曲 ID 转换为本地 Song.id
     */
    fun toLocalId(neteaseId: Long): Long = NETEASE_ID_OFFSET - neteaseId

    /**
     * 将网易云 NeteaseSong 转换为本地 [Song]，URL 由调用方传入或后续补齐
     */
    fun toSong(
        neteaseSong: code.name.monkey.retromusic.network.models.NeteaseSong,
        playUrl: String? = null,
        trackIndex: Int = 0,
        coverUrl: String? = null
    ): Song {
        val artistName = neteaseSong.ar?.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST
        val albumName = neteaseSong.al?.name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM
        val albumId = neteaseSong.al?.id ?: abs(albumName.hashCode().toLong())
        val artistId = neteaseSong.ar?.firstOrNull()?.id ?: abs(artistName.hashCode().toLong())

        // 优先使用调用方传入的 coverUrl（如 NewSong.picUrl），否则回落到网易云 album.picUrl
        val effectiveCoverUrl = coverUrl?.takeIf { it.isNotBlank() }
            ?: neteaseSong.al?.picUrl?.takeIf { it.isNotBlank() }
        NeteaseCoverCache.put(neteaseSong.id, effectiveCoverUrl)

        return Song(
            id = toLocalId(neteaseSong.id),
            title = neteaseSong.name?.takeIf { it.isNotBlank() } ?: UNKNOWN_TITLE,
            trackNumber = trackIndex + 1,
            year = 0,
            duration = neteaseSong.dt ?: 0L,
            // 关键：data 字段填入 HTTP URL，Song.uri 扩展会自动识别为远程流
            data = playUrl ?: "",
            dateModified = System.currentTimeMillis(),
            albumId = NETEASE_ID_OFFSET - albumId,
            albumName = albumName,
            artistId = NETEASE_ID_OFFSET - artistId,
            artistName = artistName,
            composer = null,
            albumArtist = artistName
        )
    }

    /**
     * 用新的播放 URL 复制一个 Song 对象
     */
    fun withPlayUrl(song: Song, url: String): Song {
        return Song(
            id = song.id,
            title = song.title,
            trackNumber = song.trackNumber,
            year = song.year,
            duration = song.duration,
            data = url,
            dateModified = song.dateModified,
            albumId = song.albumId,
            albumName = song.albumName,
            artistId = song.artistId,
            artistName = song.artistName,
            composer = song.composer,
            albumArtist = song.albumArtist
        )
    }
}
