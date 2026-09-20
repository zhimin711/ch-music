package com.ch.music.musicserver

import com.ch.music.BuildConfig

object MusicServerDefaults {
    const val PRIVATE_SOURCE = "musicServer"
    const val EXTERNAL_LOCAL_SOURCE = "androidLocal"
    const val NETEASE_SOURCE = "netease"

    val baseUrl: String
        get() = BuildConfig.MUSIC_SERVER_BASE_URL.trim().trimEnd('/')
}
