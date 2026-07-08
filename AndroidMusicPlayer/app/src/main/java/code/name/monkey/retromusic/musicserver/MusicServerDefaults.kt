package code.name.monkey.retromusic.musicserver

import code.name.monkey.retromusic.BuildConfig

object MusicServerDefaults {
    const val PRIVATE_SOURCE = "musicServer"
    const val EXTERNAL_LOCAL_SOURCE = "androidLocal"
    const val NETEASE_SOURCE = "netease"

    val baseUrl: String
        get() = BuildConfig.MUSIC_SERVER_BASE_URL.trim().trimEnd('/')
}
