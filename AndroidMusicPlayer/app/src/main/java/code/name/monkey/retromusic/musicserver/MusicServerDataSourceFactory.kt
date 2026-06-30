package code.name.monkey.retromusic.musicserver

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import code.name.monkey.retromusic.model.Song

@OptIn(UnstableApi::class)
class MusicServerDataSourceFactory(
    context: Context,
    private val session: MusicServerSession,
    private val cacheManager: MusicServerCacheManager
) {
    private val appContext = context.applicationContext

    fun createMediaSource(song: Song): MediaSource? {
        val musicId = MusicServerSongMapper.musicIdFromSong(song) ?: return null
        val cachedFile = cacheManager.resolveReadyCacheFile(session.user, musicId)
        val mediaItem = MediaItem.fromUri(cachedFile?.toUri() ?: MusicServerSongMapper.playbackUri(song))
        val dataSourceFactory = if (cachedFile != null) {
            DefaultDataSource.Factory(appContext)
        } else {
            authenticatedDataSourceFactory()
        }
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    private fun authenticatedDataSourceFactory(): DataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        if (session.accessToken.isNotBlank()) {
            httpFactory.setDefaultRequestProperties(
                mapOf("Authorization" to "Bearer ${session.accessToken}")
            )
        }
        return DefaultDataSource.Factory(appContext, httpFactory)
    }
}
