package com.chmusic.musicserver.api;

import com.chmusic.musicserver.netease.NeteasePublicEndpoint;
import com.chmusic.musicserver.netease.NeteasePublicService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/netease/public")
public class NeteasePublicController {
    private final NeteasePublicService neteasePublicService;

    public NeteasePublicController(NeteasePublicService neteasePublicService) {
        this.neteasePublicService = neteasePublicService;
    }

    @GetMapping("/search")
    public JsonNode search(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.SEARCH, params, request, authentication);
    }

    @GetMapping("/search/suggest")
    public JsonNode searchSuggest(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.SEARCH_SUGGEST, params, request, authentication);
    }

    @GetMapping("/search/default")
    public JsonNode searchDefault(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.SEARCH_DEFAULT, params, request, authentication);
    }

    @GetMapping("/search/hot/detail")
    public JsonNode searchHotDetail(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.SEARCH_HOT_DETAIL, params, request, authentication);
    }

    @GetMapping("/song/detail")
    public JsonNode songDetail(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.SONG_DETAIL, params, request, authentication);
    }

    @GetMapping({"/song/url", "/song/url/v1"})
    public JsonNode songUrl(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.SONG_URL, params, request, authentication);
    }

    @GetMapping("/lyric")
    public JsonNode lyric(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.LYRIC, params, request, authentication);
    }

    @GetMapping("/playlist/detail")
    public JsonNode playlistDetail(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.PLAYLIST_DETAIL, params, request, authentication);
    }

    @GetMapping("/playlist/catlist")
    public JsonNode playlistCatlist(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.PLAYLIST_CATLIST, params, request, authentication);
    }

    @GetMapping("/top/playlist")
    public JsonNode topPlaylist(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.TOP_PLAYLIST, params, request, authentication);
    }

    @GetMapping("/top/playlist/highquality")
    public JsonNode topPlaylistHighquality(@RequestParam MultiValueMap<String, String> params,
            HttpServletRequest request, Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.TOP_PLAYLIST_HIGHQUALITY, params, request,
                authentication);
    }

    @GetMapping("/personalized")
    public JsonNode personalized(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.PERSONALIZED, params, request, authentication);
    }

    @GetMapping("/personalized/newsong")
    public JsonNode personalizedNewsong(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.PERSONALIZED_NEWSONG, params, request, authentication);
    }

    @GetMapping("/personalized/privatecontent")
    public JsonNode personalizedPrivatecontent(@RequestParam MultiValueMap<String, String> params,
            HttpServletRequest request, Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.PERSONALIZED_PRIVATECONTENT, params, request,
                authentication);
    }

    @GetMapping("/personalized/mv")
    public JsonNode personalizedMv(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.PERSONALIZED_MV, params, request, authentication);
    }

    @GetMapping("/personalized/djprogram")
    public JsonNode personalizedDjprogram(@RequestParam MultiValueMap<String, String> params,
            HttpServletRequest request, Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.PERSONALIZED_DJPROGRAM, params, request,
                authentication);
    }

    @GetMapping("/banner")
    public JsonNode banner(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.BANNER, params, request, authentication);
    }

    @GetMapping("/album")
    public JsonNode album(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.ALBUM, params, request, authentication);
    }

    @GetMapping("/album/new")
    public JsonNode albumNew(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.ALBUM_NEW, params, request, authentication);
    }

    @GetMapping("/album/newest")
    public JsonNode albumNewest(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.ALBUM_NEWEST, params, request, authentication);
    }

    @GetMapping("/top/album")
    public JsonNode topAlbum(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.TOP_ALBUM, params, request, authentication);
    }

    @GetMapping("/artist")
    public JsonNode artist(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.ARTIST, params, request, authentication);
    }

    @GetMapping("/artist/songs")
    public JsonNode artistSongs(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.ARTIST_SONGS, params, request, authentication);
    }

    @GetMapping("/artist/album")
    public JsonNode artistAlbum(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.ARTIST_ALBUM, params, request, authentication);
    }

    @GetMapping("/artist/new/song")
    public JsonNode artistNewSong(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.ARTIST_NEW_SONG, params, request, authentication);
    }

    @GetMapping("/top/artists")
    public JsonNode topArtists(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.TOP_ARTISTS, params, request, authentication);
    }

    @GetMapping("/toplist")
    public JsonNode toplist(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.TOPLIST, params, request, authentication);
    }

    @GetMapping("/toplist/detail")
    public JsonNode toplistDetail(@RequestParam MultiValueMap<String, String> params, HttpServletRequest request,
            Authentication authentication) {
        return neteasePublicService.fetch(NeteasePublicEndpoint.TOPLIST_DETAIL, params, request, authentication);
    }
}
