package code.name.monkey.retromusic.musicserver

import com.google.gson.annotations.SerializedName

data class MusicServerUser(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?
) {
    val displayLabel: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: username
}

data class MusicServerAuthResponse(
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("user") val user: MusicServerUser
)

data class MusicServerAuthRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("displayName") val displayName: String? = null
)

data class MusicServerProfileUpdateRequest(
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class MusicServerMusic(
    @SerializedName("id") val id: String,
    @SerializedName("musicId") val musicId: Long?,
    @SerializedName("trackId") val trackId: Long?,
    @SerializedName("source") val source: String?,
    @SerializedName("externalId") val externalId: String?,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String?,
    @SerializedName("album") val album: String?,
    @SerializedName("picUrl") val picUrl: String?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("originalFilename") val originalFilename: String?,
    @SerializedName("contentType") val contentType: String?,
    @SerializedName("fileSize") val fileSize: Long,
    @SerializedName("checksum") val checksum: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("streamUrl") val streamUrl: String?,
    @SerializedName("playback") val playback: MusicServerPlaybackCapabilities?
) {
    val stableMusicId: Long?
        get() = musicId ?: id.toLongOrNull()

    val isPrivateMusic: Boolean
        get() = (source ?: "musicServer") == MusicServerDefaults.PRIVATE_SOURCE
}

data class MusicServerPlaybackCapabilities(
    @SerializedName("supportsRange") val supportsRange: Boolean,
    @SerializedName("supportsOriginal") val supportsOriginal: Boolean,
    @SerializedName("supportsTranscoding") val supportsTranscoding: Boolean,
    @SerializedName("supportsOfflineCache") val supportsOfflineCache: Boolean,
    @SerializedName("variants") val variants: List<MusicServerPlaybackVariant> = emptyList()
)

data class MusicServerPlaybackVariant(
    @SerializedName("profileId") val profileId: String,
    @SerializedName("label") val label: String?,
    @SerializedName("contentType") val contentType: String?,
    @SerializedName("bitrateKbps") val bitrateKbps: Int?,
    @SerializedName("streamUrl") val streamUrl: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("fileSize") val fileSize: Long?,
    @SerializedName("checksum") val checksum: String?
)

data class MusicServerFavorite(
    @SerializedName("id") val id: Long,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("music") val music: MusicServerMusic
)

data class MusicServerPlaylist(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("tracks") val tracks: List<MusicServerMusic>
)

data class MusicServerPlaylistRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)

data class MusicServerAddTrackRequest(
    @SerializedName("musicId") val musicId: Long? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("externalId") val externalId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("picUrl") val picUrl: String? = null,
    @SerializedName("duration") val duration: Long? = null
)

data class MusicServerState(
    val user: MusicServerUser? = null,
    val music: List<MusicServerMusic> = emptyList(),
    val favorites: List<MusicServerFavorite> = emptyList(),
    val playlists: List<MusicServerPlaylist> = emptyList(),
    val cacheEntries: Map<String, MusicServerCacheEntry> = emptyMap(),
    val cachePolicy: MusicServerCachePolicy = MusicServerCachePolicy()
) {
    val isLoggedIn: Boolean
        get() = user != null
}

enum class MusicServerCacheState {
    QUEUED,
    DOWNLOADING,
    READY,
    FAILED,
    STALE,
    PAUSED,
    WAITING_FOR_WIFI,
    STORAGE_LOW
}

data class MusicServerCacheEntry(
    val cacheKey: String,
    val serverBaseUrl: String,
    val userId: Long,
    val musicId: Long,
    val profileId: String,
    val title: String,
    val checksum: String?,
    val fileSize: Long,
    val contentType: String?,
    val streamUrl: String,
    val localPath: String,
    val tempPath: String,
    val state: MusicServerCacheState,
    val downloadedBytes: Long = 0L,
    val pinned: Boolean = true,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long? = null
)

data class MusicServerCachePolicy(
    val wifiOnly: Boolean = true,
    val maxSizeBytes: Long = 2L * 1024L * 1024L * 1024L,
    val storageLow: Boolean = false
)

data class MusicServerCacheSyncResult(
    val activeKeys: List<String>,
    val staleKeys: List<String>,
    val removedKeys: List<String>,
    val entries: Map<String, MusicServerCacheEntry>
)
