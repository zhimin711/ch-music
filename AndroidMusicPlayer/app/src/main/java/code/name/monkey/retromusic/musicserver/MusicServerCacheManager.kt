package code.name.monkey.retromusic.musicserver

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

class MusicServerCacheManager(
    context: Context,
    private val client: OkHttpClient
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cacheRoot = File(appContext.filesDir, CACHE_ROOT_DIR)
    private val _entries = MutableStateFlow(readEntries())
    private val _policy = MutableStateFlow(readPolicy())

    val entries: StateFlow<Map<String, MusicServerCacheEntry>> = _entries
    val policy: StateFlow<MusicServerCachePolicy> = _policy

    init {
        cacheRoot.mkdirs()
        restoreInterruptedDownloads()
    }

    suspend fun enqueue(
        user: MusicServerUser,
        music: MusicServerMusic,
        profileId: String = ORIGINAL_PROFILE_ID,
        pinned: Boolean = true
    ): MusicServerCacheEntry? = withContext(Dispatchers.IO) {
        val musicId = music.stableMusicId ?: return@withContext null
        val variant = music.playback?.variants?.firstOrNull { it.profileId == profileId }
        val streamUrl = variant?.streamUrl?.takeIf { it.isNotBlank() }
            ?: music.streamUrl?.takeIf { it.isNotBlank() }
            ?: "/api/music/$musicId/stream"
        val checksum = variant?.checksum ?: music.checksum
        val fileSize = variant?.fileSize ?: music.fileSize
        val contentType = variant?.contentType ?: music.contentType
        val cacheKey = cacheKey(MusicServerDefaults.baseUrl, user.id, musicId, profileId)
        val existing = _entries.value[cacheKey]
        val now = System.currentTimeMillis()
        val next = existing?.copy(
            title = music.title,
            checksum = checksum,
            fileSize = fileSize,
            contentType = contentType,
            streamUrl = absoluteStreamUrl(streamUrl),
            pinned = pinned,
            state = if (existing.state == MusicServerCacheState.READY) {
                existing.state
            } else {
                MusicServerCacheState.QUEUED
            },
            error = null,
            updatedAt = now
        ) ?: MusicServerCacheEntry(
            cacheKey = cacheKey,
            serverBaseUrl = MusicServerDefaults.baseUrl,
            userId = user.id,
            musicId = musicId,
            profileId = profileId,
            title = music.title,
            checksum = checksum,
            fileSize = fileSize,
            contentType = contentType,
            streamUrl = absoluteStreamUrl(streamUrl),
            localPath = localFile(cacheKey, profileId, contentType).absolutePath,
            tempPath = File(localFile(cacheKey, profileId, contentType).absolutePath + TEMP_SUFFIX).absolutePath,
            state = MusicServerCacheState.QUEUED,
            pinned = pinned,
            createdAt = now,
            updatedAt = now
        )
        val validated = if (next.state == MusicServerCacheState.READY) validate(next) else next
        saveEntry(validated)
        validated
    }

    suspend fun download(cacheKey: String): MusicServerCacheEntry? = withContext(Dispatchers.IO) {
        val entry = _entries.value[cacheKey] ?: return@withContext null
        if (entry.state == MusicServerCacheState.READY && validate(entry).state == MusicServerCacheState.READY) {
            return@withContext _entries.value[cacheKey]
        }
        if (policy.value.wifiOnly && !isOnWifi()) {
            val waitingEntry = entry.copy(
                state = MusicServerCacheState.WAITING_FOR_WIFI,
                error = "WAITING_FOR_WIFI",
                updatedAt = System.currentTimeMillis()
            )
            saveEntry(waitingEntry)
            return@withContext waitingEntry
        }
        if (policy.value.storageLow || isStorageLow()) {
            val storageEntry = entry.copy(
                state = MusicServerCacheState.STORAGE_LOW,
                error = "STORAGE_LOW",
                updatedAt = System.currentTimeMillis()
            )
            saveEntry(storageEntry)
            updatePolicy(policy.value.copy(storageLow = true))
            return@withContext storageEntry
        }

        val tempFile = File(entry.tempPath)
        tempFile.parentFile?.mkdirs()
        val existingBytes = tempFile.takeIf { it.exists() }?.length() ?: 0L
        val requestBuilder = Request.Builder().url(entry.streamUrl)
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val downloadingEntry = entry.copy(
            state = MusicServerCacheState.DOWNLOADING,
            downloadedBytes = existingBytes,
            error = null,
            updatedAt = System.currentTimeMillis()
        )
        saveEntry(downloadingEntry)

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP_${response.code}")
                }
                val append = existingBytes > 0 && response.code == 206
                if (!append && tempFile.exists()) tempFile.delete()
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(tempFile, append).buffered().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("EMPTY_BODY")
            }
            finalizeDownload(downloadingEntry)
        } catch (error: Throwable) {
            val failed = downloadingEntry.copy(
                state = MusicServerCacheState.FAILED,
                error = safeError(error),
                downloadedBytes = tempFile.takeIf { it.exists() }?.length() ?: existingBytes,
                updatedAt = System.currentTimeMillis()
            )
            saveEntry(failed)
            failed
        }
    }

    suspend fun pause(cacheKey: String): MusicServerCacheEntry? = withContext(Dispatchers.IO) {
        val entry = _entries.value[cacheKey] ?: return@withContext null
        val paused = entry.copy(
            state = MusicServerCacheState.PAUSED,
            downloadedBytes = File(entry.tempPath).takeIf { it.exists() }?.length() ?: entry.downloadedBytes,
            updatedAt = System.currentTimeMillis()
        )
        saveEntry(paused)
        paused
    }

    suspend fun retry(cacheKey: String): MusicServerCacheEntry? = withContext(Dispatchers.IO) {
        val entry = _entries.value[cacheKey] ?: return@withContext null
        val queued = entry.copy(
            state = MusicServerCacheState.QUEUED,
            error = null,
            updatedAt = System.currentTimeMillis()
        )
        saveEntry(queued)
        queued
    }

    suspend fun remove(cacheKey: String): Boolean = withContext(Dispatchers.IO) {
        val entry = _entries.value[cacheKey] ?: return@withContext false
        deleteIfInsideRoot(File(entry.localPath))
        deleteIfInsideRoot(File(entry.tempPath))
        val next = _entries.value.toMutableMap()
        next.remove(cacheKey)
        saveEntries(next)
        true
    }

    suspend fun validate(cacheKey: String): MusicServerCacheEntry? = withContext(Dispatchers.IO) {
        val entry = _entries.value[cacheKey] ?: return@withContext null
        validate(entry)
    }

    suspend fun syncIndex(
        user: MusicServerUser?,
        musicList: List<MusicServerMusic>
    ): MusicServerCacheSyncResult = withContext(Dispatchers.IO) {
        if (user == null) {
            return@withContext MusicServerCacheSyncResult(
                activeKeys = emptyList(),
                staleKeys = emptyList(),
                removedKeys = emptyList(),
                entries = emptyMap()
            )
        }

        val activeMusic = musicList.mapNotNull { music ->
            val musicId = music.stableMusicId ?: return@mapNotNull null
            musicId to music.checksum
        }.toMap()

        val activeKeys = mutableListOf<String>()
        val staleKeys = mutableListOf<String>()
        val removedKeys = mutableListOf<String>()
        val next = _entries.value.toMutableMap()

        _entries.value.values
            .filter { it.serverBaseUrl == MusicServerDefaults.baseUrl && it.userId == user.id }
            .forEach { entry ->
                val currentChecksum = activeMusic[entry.musicId]
                if (!activeMusic.containsKey(entry.musicId)) {
                    deleteIfInsideRoot(File(entry.localPath))
                    deleteIfInsideRoot(File(entry.tempPath))
                    next.remove(entry.cacheKey)
                    removedKeys += entry.cacheKey
                } else if (!currentChecksum.isNullOrBlank() && currentChecksum != entry.checksum) {
                    val stale = entry.copy(
                        checksum = currentChecksum,
                        state = MusicServerCacheState.STALE,
                        error = "CHECKSUM_CHANGED",
                        updatedAt = System.currentTimeMillis()
                    )
                    next[entry.cacheKey] = stale
                    staleKeys += entry.cacheKey
                } else {
                    activeKeys += entry.cacheKey
                }
            }

        saveEntries(next)
        MusicServerCacheSyncResult(
            activeKeys = activeKeys,
            staleKeys = staleKeys,
            removedKeys = removedKeys,
            entries = next.filterValues {
                it.serverBaseUrl == MusicServerDefaults.baseUrl && it.userId == user.id
            }
        )
    }

    fun entriesFor(user: MusicServerUser?): Map<String, MusicServerCacheEntry> {
        if (user == null) return emptyMap()
        return _entries.value.filterValues {
            it.serverBaseUrl == MusicServerDefaults.baseUrl && it.userId == user.id
        }
    }

    fun resolveReadyCacheFile(
        user: MusicServerUser?,
        musicId: Long,
        profileId: String = ORIGINAL_PROFILE_ID
    ): File? {
        val entry = entriesFor(user).values.firstOrNull {
            it.musicId == musicId &&
                it.profileId == profileId &&
                it.state == MusicServerCacheState.READY
        } ?: return null
        val file = File(entry.localPath)
        if (!file.exists()) return null
        if (entry.fileSize > 0 && file.length() != entry.fileSize) return null
        return file
    }

    fun updatePolicy(nextPolicy: MusicServerCachePolicy) {
        _policy.value = nextPolicy
        preferences.edit {
            putBoolean(KEY_WIFI_ONLY, nextPolicy.wifiOnly)
            putLong(KEY_MAX_SIZE_BYTES, nextPolicy.maxSizeBytes)
            putBoolean(KEY_STORAGE_LOW, nextPolicy.storageLow)
        }
    }

    private fun finalizeDownload(entry: MusicServerCacheEntry): MusicServerCacheEntry {
        val tempFile = File(entry.tempPath)
        val localFile = File(entry.localPath)
        if (!tempFile.exists()) {
            return markStale(entry, "CACHE_FILE_MISSING")
        }
        if (entry.fileSize > 0 && tempFile.length() != entry.fileSize) {
            return markStale(entry, "CACHE_SIZE_MISMATCH")
        }
        if (!entry.checksum.isNullOrBlank() && sha256(tempFile) != entry.checksum) {
            return markStale(entry, "CACHE_CHECKSUM_MISMATCH")
        }
        localFile.parentFile?.mkdirs()
        if (localFile.exists()) localFile.delete()
        tempFile.renameTo(localFile)
        val ready = entry.copy(
            state = MusicServerCacheState.READY,
            downloadedBytes = localFile.length(),
            error = null,
            lastVerifiedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveEntry(ready)
        pruneToPolicy()
        return ready
    }

    private fun validate(entry: MusicServerCacheEntry): MusicServerCacheEntry {
        if (entry.state != MusicServerCacheState.READY) return entry
        val file = File(entry.localPath)
        if (!file.exists()) return markStale(entry, "CACHE_FILE_MISSING")
        if (entry.fileSize > 0 && file.length() != entry.fileSize) {
            return markStale(entry, "CACHE_SIZE_MISMATCH")
        }
        if (!entry.checksum.isNullOrBlank() && sha256(file) != entry.checksum) {
            return markStale(entry, "CACHE_CHECKSUM_MISMATCH")
        }
        val ready = entry.copy(
            lastVerifiedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveEntry(ready)
        return ready
    }

    private fun markStale(entry: MusicServerCacheEntry, error: String): MusicServerCacheEntry {
        val stale = entry.copy(
            state = MusicServerCacheState.STALE,
            error = error,
            updatedAt = System.currentTimeMillis()
        )
        saveEntry(stale)
        return stale
    }

    private fun restoreInterruptedDownloads() {
        val next = _entries.value.mapValues { (_, entry) ->
            if (entry.state == MusicServerCacheState.DOWNLOADING) {
                entry.copy(state = MusicServerCacheState.PAUSED, updatedAt = System.currentTimeMillis())
            } else {
                entry
            }
        }
        saveEntries(next)
    }

    private fun saveEntry(entry: MusicServerCacheEntry) {
        val next = _entries.value.toMutableMap()
        next[entry.cacheKey] = entry
        saveEntries(next)
    }

    private fun saveEntries(entries: Map<String, MusicServerCacheEntry>) {
        _entries.value = entries
        preferences.edit { putString(KEY_ENTRIES, gson.toJson(entries)) }
    }

    private fun readEntries(): Map<String, MusicServerCacheEntry> {
        val raw = preferences.getString(KEY_ENTRIES, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, MusicServerCacheEntry>>() {}.type
            gson.fromJson<Map<String, MusicServerCacheEntry>>(raw, type).orEmpty()
        } catch (_: JsonSyntaxException) {
            preferences.edit { remove(KEY_ENTRIES) }
            emptyMap()
        }
    }

    private fun readPolicy(): MusicServerCachePolicy {
        return MusicServerCachePolicy(
            wifiOnly = preferences.getBoolean(KEY_WIFI_ONLY, true),
            maxSizeBytes = preferences.getLong(KEY_MAX_SIZE_BYTES, DEFAULT_MAX_SIZE_BYTES),
            storageLow = preferences.getBoolean(KEY_STORAGE_LOW, false)
        )
    }

    private fun localFile(cacheKey: String, profileId: String, contentType: String?): File {
        val extension = extensionFor(contentType)
        return File(cacheRoot, "${cacheKey}/${sanitize(profileId)}$extension")
    }

    private fun cacheKey(serverBaseUrl: String, userId: Long, musicId: Long, profileId: String): String {
        return sha256("$serverBaseUrl|$userId|$musicId|$profileId").take(32)
    }

    private fun absoluteStreamUrl(streamUrl: String): String {
        return if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
            streamUrl
        } else {
            "${MusicServerDefaults.baseUrl}${streamUrl}"
        }
    }

    private fun pruneToPolicy() {
        val readyEntries = _entries.value.values
            .filter { it.state == MusicServerCacheState.READY && !it.pinned }
            .sortedBy { it.updatedAt }
        var totalSize = _entries.value.values
            .filter { it.state == MusicServerCacheState.READY }
            .sumOf { File(it.localPath).takeIf { file -> file.exists() }?.length() ?: 0L }
        val next = _entries.value.toMutableMap()
        for (entry in readyEntries) {
            if (totalSize <= policy.value.maxSizeBytes) break
            val file = File(entry.localPath)
            val size = file.takeIf { it.exists() }?.length() ?: 0L
            deleteIfInsideRoot(file)
            next[entry.cacheKey] = entry.copy(
                state = MusicServerCacheState.STALE,
                error = "CACHE_PRUNED",
                updatedAt = System.currentTimeMillis()
            )
            totalSize -= size
        }
        if (next != _entries.value) saveEntries(next)
    }

    private fun isStorageLow(): Boolean {
        cacheRoot.mkdirs()
        return cacheRoot.usableSpace < MIN_FREE_BYTES
    }

    private fun isOnWifi(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun deleteIfInsideRoot(file: File) {
        val root = cacheRoot.canonicalFile
        val target = file.canonicalFile
        if (target.toPath().startsWith(root.toPath())) {
            target.delete()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun extensionFor(contentType: String?): String {
        return when (contentType?.lowercase()) {
            "audio/mpeg", "audio/mp3" -> ".mp3"
            "audio/mp4", "audio/x-m4a" -> ".m4a"
            "audio/aac" -> ".aac"
            "audio/flac", "audio/x-flac" -> ".flac"
            "audio/wav", "audio/x-wav" -> ".wav"
            "audio/ogg" -> ".ogg"
            "audio/webm" -> ".webm"
            else -> ".audio"
        }
    }

    private fun sanitize(value: String): String {
        return value.replace(Regex("""[<>:"/\\|?*\s]+"""), "_").take(80).ifBlank { ORIGINAL_PROFILE_ID }
    }

    private fun safeError(error: Throwable): String {
        return error.message?.take(120) ?: error::class.java.simpleName
    }

    private companion object {
        const val PREFERENCES_NAME = "music_server_cache"
        const val KEY_ENTRIES = "entries"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val KEY_MAX_SIZE_BYTES = "max_size_bytes"
        const val KEY_STORAGE_LOW = "storage_low"
        const val CACHE_ROOT_DIR = "music-server-cache"
        const val ORIGINAL_PROFILE_ID = "original"
        const val TEMP_SUFFIX = ".part"
        const val DEFAULT_MAX_SIZE_BYTES = 2L * 1024L * 1024L * 1024L
        const val MIN_FREE_BYTES = 128L * 1024L * 1024L
    }
}
