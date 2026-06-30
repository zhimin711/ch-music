package code.name.monkey.retromusic.network.models

/**
 * 网易云歌曲播放 URL 响应
 */
data class NeteaseSongUrlResponse(
    val code: Int,
    val data: List<SongUrl>?
)

data class SongUrl(
    val id: Long,
    val url: String?,
    val br: Int,          // 比特率
    val size: Long,       // 文件大小
    val md5: String?,
    val type: String?     // 音频类型，如 mp3
)
