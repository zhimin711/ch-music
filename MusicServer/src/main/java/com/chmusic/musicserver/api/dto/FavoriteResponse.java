package com.chmusic.musicserver.api.dto;

import com.chmusic.musicserver.favorite.FavoriteTrack;
import java.time.Instant;

public record FavoriteResponse(Long id, Instant createdAt, MusicResponse music) {
    public static FavoriteResponse from(FavoriteTrack favorite) {
        return new FavoriteResponse(favorite.getId(), favorite.getCreatedAt(), MusicResponse.from(favorite));
    }
}
