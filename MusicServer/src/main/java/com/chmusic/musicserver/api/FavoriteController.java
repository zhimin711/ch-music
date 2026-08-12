package com.chmusic.musicserver.api;

import com.chmusic.musicserver.api.dto.AddTrackRequest;
import com.chmusic.musicserver.api.dto.FavoriteResponse;
import com.chmusic.musicserver.favorite.FavoriteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteResponse> list(Authentication authentication) {
        return favoriteService.list(CurrentUser.from(authentication));
    }

    @PostMapping("/{musicId}")
    public List<FavoriteResponse> add(Authentication authentication, @PathVariable Long musicId) {
        return favoriteService.add(CurrentUser.from(authentication), musicId);
    }

    @DeleteMapping("/external")
    public List<FavoriteResponse> removeExternal(Authentication authentication, @RequestParam String source,
            @RequestParam String externalId) {
        return favoriteService.removeExternal(CurrentUser.from(authentication), source, externalId);
    }

    @PostMapping
    public List<FavoriteResponse> addExternal(Authentication authentication,
            @Valid @RequestBody AddTrackRequest request) {
        return favoriteService.addExternal(CurrentUser.from(authentication),
                new FavoriteService.FavoriteRequestPayload(request.musicId(), request.source(), request.externalId(),
                        request.title(), request.artist(), request.album(), request.picUrl(), request.duration()));
    }

    @DeleteMapping("/{musicId}")
    public List<FavoriteResponse> remove(Authentication authentication, @PathVariable Long musicId) {
        return favoriteService.remove(CurrentUser.from(authentication), musicId);
    }
}
