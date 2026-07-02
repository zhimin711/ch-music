package com.chmusic.musicserver.netease;

import java.time.Duration;
import java.util.Set;

public enum NeteasePublicEndpoint {
    SEARCH("/cloudsearch", CacheProfile.SHORT, Set.of("keywords", "type", "limit", "offset")),
    SEARCH_SUGGEST("/search/suggest", CacheProfile.SHORT, Set.of("keywords", "type")),
    SEARCH_DEFAULT("/search/default", CacheProfile.SHORT, Set.of()),
    SEARCH_HOT_DETAIL("/search/hot/detail", CacheProfile.SHORT, Set.of()),
    SONG_DETAIL("/song/detail", CacheProfile.MEDIUM, Set.of("ids")),
    SONG_URL("/song/url/v1", CacheProfile.PLAYBACK, Set.of("id", "level", "encodeType")),
    LYRIC("/lyric/new", CacheProfile.MEDIUM, Set.of("id")),
    PLAYLIST_DETAIL("/playlist/detail", CacheProfile.MEDIUM, Set.of("id", "s")),
    PLAYLIST_CATLIST("/playlist/catlist", CacheProfile.MEDIUM, Set.of()),
    TOP_PLAYLIST("/top/playlist", CacheProfile.MEDIUM, Set.of("cat", "offset", "limit", "order")),
    TOP_PLAYLIST_HIGHQUALITY("/top/playlist/highquality", CacheProfile.MEDIUM, Set.of("cat", "tag", "before", "limit")),
    PERSONALIZED("/personalized", CacheProfile.SHORT, Set.of("limit")),
    PERSONALIZED_NEWSONG("/personalized/newsong", CacheProfile.SHORT, Set.of("limit")),
    PERSONALIZED_PRIVATECONTENT("/personalized/privatecontent", CacheProfile.SHORT, Set.of("limit")),
    PERSONALIZED_MV("/personalized/mv", CacheProfile.SHORT, Set.of()),
    PERSONALIZED_DJPROGRAM("/personalized/djprogram", CacheProfile.SHORT, Set.of()),
    BANNER("/banner", CacheProfile.SHORT, Set.of("type")),
    ALBUM("/album", CacheProfile.MEDIUM, Set.of("id")),
    ALBUM_NEW("/album/new", CacheProfile.MEDIUM, Set.of("limit", "offset", "area")),
    ALBUM_NEWEST("/album/newest", CacheProfile.MEDIUM, Set.of()),
    TOP_ALBUM("/top/album", CacheProfile.MEDIUM, Set.of("limit", "offset", "area", "type", "year", "month")),
    ARTIST("/artist/detail", CacheProfile.MEDIUM, Set.of("id")),
    ARTIST_SONGS("/artist/songs", CacheProfile.MEDIUM, Set.of("id", "offset", "limit", "order")),
    ARTIST_ALBUM("/artist/album", CacheProfile.MEDIUM, Set.of("id", "limit", "offset")),
    TOP_ARTISTS("/top/artists", CacheProfile.MEDIUM, Set.of("offset", "limit")),
    ARTIST_NEW_SONG("/artist/new/song", CacheProfile.MEDIUM, Set.of("limit")),
    TOPLIST("/toplist", CacheProfile.MEDIUM, Set.of()),
    TOPLIST_DETAIL("/toplist/detail", CacheProfile.MEDIUM, Set.of());

    private final String sidecarPath;
    private final CacheProfile cacheProfile;
    private final Set<String> allowedParams;

    NeteasePublicEndpoint(String sidecarPath, CacheProfile cacheProfile, Set<String> allowedParams) {
        this.sidecarPath = sidecarPath;
        this.cacheProfile = cacheProfile;
        this.allowedParams = allowedParams;
    }

    public String sidecarPath() {
        return sidecarPath;
    }

    public Set<String> allowedParams() {
        return allowedParams;
    }

    public Duration ttl(NeteaseSettings settings) {
        return switch (cacheProfile) {
            case SHORT -> settings.shortCacheTtl();
            case MEDIUM -> settings.mediumCacheTtl();
            case PLAYBACK -> settings.playbackCacheTtl();
        };
    }

    private enum CacheProfile {
        SHORT,
        MEDIUM,
        PLAYBACK
    }
}
