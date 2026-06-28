package code.name.monkey.retromusic.extensions

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.net.Uri
import code.name.monkey.retromusic.musicserver.MusicServerSongMapper
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil

val Song.uri: Uri
    get() = if (MusicServerSongMapper.isRemoteSong(this)) {
        Uri.parse(data)
    } else {
        MusicUtil.getSongFileUri(songId = id)
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
