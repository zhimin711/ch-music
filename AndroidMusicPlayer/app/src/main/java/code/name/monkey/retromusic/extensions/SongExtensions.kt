package code.name.monkey.retromusic.extensions

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.net.Uri
import code.name.monkey.retromusic.musicserver.MusicServerSongMapper
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil

val Song.uri: Uri
    get() {
        // 优先按 data 判断：任何 http/https 数据源（MusicServer 或网易云在线歌曲）
        // 都直接走网络 URI，避免落到 MediaStore（负 ID 时 ExoPlayer 会抛 UnsupportedOperationException）。
        val src = data
        return if (src.startsWith("http://") || src.startsWith("https://")) {
            Uri.parse(src)
        } else if (MusicServerSongMapper.isRemoteSong(this)) {
            Uri.parse(src)
        } else {
            MusicUtil.getSongFileUri(songId = id)
        }
    }

val Song.albumArtUri get() = MusicUtil.getMediaStoreAlbumCoverUri(albumId)

fun ArrayList<Song>.toMediaSessionQueue(): List<QueueItem> {
    return map { song ->
        val mediaDescription = MediaDescriptionCompat.Builder()
            .setMediaId(song.id.toString())
            .setTitle(song.title)
            .setSubtitle(song.artistName)
            .setIconUri(song.albumArtUri)
            .build()
        QueueItem(mediaDescription, song.hashCode().toLong())
    }
}
