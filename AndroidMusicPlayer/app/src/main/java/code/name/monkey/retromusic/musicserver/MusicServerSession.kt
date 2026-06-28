package code.name.monkey.retromusic.musicserver

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class MusicServerSession(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "music_server_session",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    var accessToken: String
        get() = preferences.getString(KEY_TOKEN, "").orEmpty()
        private set(value) = preferences.edit { putString(KEY_TOKEN, value) }

    var expiresAt: String
        get() = preferences.getString(KEY_EXPIRES_AT, "").orEmpty()
        private set(value) = preferences.edit { putString(KEY_EXPIRES_AT, value) }

    var user: MusicServerUser?
        get() {
            val raw = preferences.getString(KEY_USER, null) ?: return null
            return try {
                gson.fromJson(raw, MusicServerUser::class.java)
            } catch (_: JsonSyntaxException) {
                clear()
                null
            }
        }
        private set(value) = preferences.edit {
            if (value == null) remove(KEY_USER) else putString(KEY_USER, gson.toJson(value))
        }

    val isLoggedIn: Boolean
        get() = accessToken.isNotBlank() && user != null

    fun save(auth: MusicServerAuthResponse) {
        accessToken = auth.accessToken
        expiresAt = auth.expiresAt
        user = auth.user
    }

    fun saveUser(value: MusicServerUser) {
        user = value
    }

    fun clear() {
        preferences.edit {
            remove(KEY_TOKEN)
            remove(KEY_EXPIRES_AT)
            remove(KEY_USER)
        }
    }

    private companion object {
        const val KEY_TOKEN = "access_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER = "user"
    }
}
