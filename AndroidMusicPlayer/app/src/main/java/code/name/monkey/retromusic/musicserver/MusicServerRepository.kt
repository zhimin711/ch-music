package code.name.monkey.retromusic.musicserver

import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class MusicServerRepository(
    private val api: MusicServerApi,
    private val session: MusicServerSession
) {
    private val _state = MutableStateFlow(MusicServerState(user = session.user))
    val state: StateFlow<MusicServerState> = _state

    val currentUser: MusicServerUser?
        get() = _state.value.user

    suspend fun login(username: String, password: String) {
        val auth = api.login(MusicServerAuthRequest(username, password))
        session.save(auth)
        refreshAll(auth.user)
    }

    suspend fun register(username: String, password: String, displayName: String?) {
        val auth = api.register(MusicServerAuthRequest(username, password, displayName))
        session.save(auth)
        refreshAll(auth.user)
    }

    suspend fun logout() {
        runCatching { api.logout() }
        clearSession()
    }

    suspend fun restoreSession() {
        if (session.accessToken.isBlank()) {
            clearSession()
            return
        }
        try {
            val user = api.me()
            session.saveUser(user)
            refreshAll(user)
        } catch (error: Throwable) {
            if (error.isUnauthorized()) clearSession() else throw error
        }
    }

    suspend fun updateProfile(displayName: String) {
        val user = api.updateMe(MusicServerProfileUpdateRequest(displayName = displayName))
        session.saveUser(user)
        _state.value = _state.value.copy(user = user)
    }

    suspend fun uploadAvatar(file: File, contentType: String?) {
        val user = api.uploadAvatar(file.toMultipart("file", contentType))
        session.saveUser(user)
        _state.value = _state.value.copy(user = user)
    }

    suspend fun refreshAll() = refreshAll(_state.value.user ?: session.user)

    suspend fun uploadMusic(file: File, contentType: String?, title: String?, artist: String?, album: String?) {
        api.uploadMusic(
            file = file.toMultipart("file", contentType),
            title = title.asPlainRequestBody(),
            artist = artist.asPlainRequestBody(),
            album = album.asPlainRequestBody()
        )
        refreshAll()
    }

    suspend fun deleteMusic(musicId: Long) {
        api.deleteMusic(musicId)
        refreshAll()
    }

    suspend fun toggleFavorite(music: MusicServerMusic) {
        val musicId = music.stableMusicId ?: return
        val isFavorite = _state.value.favorites.any { it.music.stableMusicId == musicId }
        val favorites = if (isFavorite) api.removeFavorite(musicId) else api.addFavorite(musicId)
        _state.value = _state.value.copy(favorites = favorites)
    }

    suspend fun createPlaylist(name: String, description: String?) {
        api.createPlaylist(MusicServerPlaylistRequest(name, description))
        refreshPlaylists()
    }

    suspend fun updatePlaylist(playlistId: Long, name: String, description: String?) {
        api.updatePlaylist(playlistId, MusicServerPlaylistRequest(name, description))
        refreshPlaylists()
    }

    suspend fun deletePlaylist(playlistId: Long) {
        api.deletePlaylist(playlistId)
        refreshPlaylists()
    }

    suspend fun addPrivateTrackToPlaylist(playlistId: Long, music: MusicServerMusic) {
        val musicId = music.stableMusicId ?: return
        api.addPlaylistTrack(playlistId, MusicServerAddTrackRequest(musicId = musicId))
        refreshPlaylists()
    }

    suspend fun addLocalTrackToPlaylist(playlistId: Long, song: Song) {
        api.addPlaylistTrack(
            playlistId,
            MusicServerAddTrackRequest(
                source = MusicServerDefaults.EXTERNAL_LOCAL_SOURCE,
                externalId = song.id.toString(),
                title = song.title,
                artist = song.artistName,
                album = song.albumName,
                duration = song.duration
            )
        )
        refreshPlaylists()
    }

    suspend fun removePlaylistTrack(playlistId: Long, trackId: Long) {
        api.removePlaylistTrack(playlistId, trackId)
        refreshPlaylists()
    }

    fun toSong(music: MusicServerMusic): Song = MusicServerSongMapper.toSong(music, session)

    private suspend fun refreshAll(user: MusicServerUser?) {
        if (user == null) {
            clearSession()
            return
        }
        val music = api.music()
        val favorites = api.favorites()
        val playlists = api.playlists()
        _state.value = MusicServerState(user, music, favorites, playlists)
    }

    private suspend fun refreshPlaylists() {
        _state.value = _state.value.copy(playlists = api.playlists())
    }

    private fun clearSession() {
        session.clear()
        _state.value = MusicServerState()
    }

    private fun File.toMultipart(partName: String, contentType: String?): MultipartBody.Part {
        val body = asRequestBody(contentType?.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, name, body)
    }

    private fun String?.asPlainRequestBody(): RequestBody? {
        val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return value.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}
