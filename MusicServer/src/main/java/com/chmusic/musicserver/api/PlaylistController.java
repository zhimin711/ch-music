package com.chmusic.musicserver.api;

import com.chmusic.musicserver.api.dto.AddTrackRequest;
import com.chmusic.musicserver.api.dto.PlaylistRequest;
import com.chmusic.musicserver.api.dto.PlaylistResponse;
import com.chmusic.musicserver.playlist.PlaylistService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    public List<PlaylistResponse> list(Authentication authentication) {
        return playlistService.list(CurrentUser.from(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaylistResponse create(Authentication authentication, @Valid @RequestBody PlaylistRequest request) {
        return playlistService.create(CurrentUser.from(authentication), request);
    }

    @GetMapping("/{playlistId}")
    public PlaylistResponse details(Authentication authentication, @PathVariable Long playlistId) {
        return playlistService.details(CurrentUser.from(authentication), playlistId);
    }

    @PutMapping("/{playlistId}")
    public PlaylistResponse update(Authentication authentication, @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistRequest request) {
        return playlistService.update(CurrentUser.from(authentication), playlistId, request);
    }

    @DeleteMapping("/{playlistId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long playlistId) {
        playlistService.delete(CurrentUser.from(authentication), playlistId);
    }

    @PostMapping("/{playlistId}/tracks")
    public PlaylistResponse addTrack(Authentication authentication, @PathVariable Long playlistId,
            @Valid @RequestBody AddTrackRequest request) {
        return playlistService.addTrack(CurrentUser.from(authentication), playlistId, request.musicId());
    }

    @DeleteMapping("/{playlistId}/tracks/{musicId}")
    public PlaylistResponse removeTrack(Authentication authentication, @PathVariable Long playlistId,
            @PathVariable Long musicId) {
        return playlistService.removeTrack(CurrentUser.from(authentication), playlistId, musicId);
    }
}
