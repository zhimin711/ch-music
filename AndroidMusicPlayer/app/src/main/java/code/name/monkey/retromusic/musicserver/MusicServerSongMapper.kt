package code.name.monkey.retromusic.musicserver

import android.net.Uri
import code.name.monkey.retromusic.model.Song
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.abs

object MusicServerSongMapper {
    private const val REMOTE_ID_OFFSET = -9_000_000_000L
    private const val UNKNOWN_ARTIST = "Unknown artist"
    private const val UNKNOWN_ALBUM = "MusicServer"

    fun toSong(music: MusicServerMusic, session: MusicServerSession): Song {
        val musicId = music.stableMusicId ?: stableHash(music.id)
        return Song(
            id = REMOTE_ID_OFFSET - musicId,
            title = music.title,
            trackNumber = 0,
            year = 0,
            duration = music.duration ?: -1L,
            data = buildStreamUrl(music, session),
            dateModified = 0,
            albumId = REMOTE_ID_OFFSET - abs((music.album ?: UNKNOWN_ALBUM).hashCode()).toLong(),
            albumName = music.album?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
            artistId = REMOTE_ID_OFFSET - abs((music.artist ?: UNKNOWN_ARTIST).hashCode()).toLong(),
            artistName = music.artist?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
            composer = null,
            albumArtist = music.artist
        )
    }

    fun isRemoteSong(song: Song): Boolean {
        return song.data.startsWith("http://") || song.data.startsWith("https://")
    }

    fun musicIdFromSong(song: Song): Long? {
        if (!isRemoteSong(song)) return null
        return REMOTE_ID_OFFSET - song.id
    }

    private fun buildStreamUrl(music: MusicServerMusic, session: MusicServerSession): String {
        val streamPath = music.streamUrl?.takeIf { it.isNotBlank() }
            ?: "/api/music/${music.stableMusicId}/stream"
        val base = if (streamPath.startsWith("http://") || streamPath.startsWith("https://")) {
            streamPath
        } else {
            "${MusicServerDefaults.baseUrl}${streamPath}"
        }
        if (session.accessToken.isBlank()) return base
        val separator = if (base.contains("?")) "&" else "?"
        val token = URLEncoder.encode(session.accessToken, StandardCharsets.UTF_8.name())
        return "$base${separator}access_token=$token"
    }

    private fun stableHash(value: String): Long {
        return abs(value.hashCode()).toLong().coerceAtLeast(1L)
    }
}
