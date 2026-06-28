package com.chmusic.musicserver.api.dto;

import com.chmusic.musicserver.playlist.Playlist;
import java.time.Instant;
import java.util.List;

public record PlaylistResponse(Long id, String name, String description, Instant createdAt, List<MusicResponse> tracks) {
    public static PlaylistResponse summary(Playlist playlist) {
        return new PlaylistResponse(playlist.getId(), playlist.getName(), playlist.getDescription(),
                playlist.getCreatedAt(), List.of());
    }

    public static PlaylistResponse details(Playlist playlist) {
        List<MusicResponse> tracks = playlist.getTracks().stream()
                .map(MusicResponse::from)
                .toList();
        return new PlaylistResponse(playlist.getId(), playlist.getName(), playlist.getDescription(),
                playlist.getCreatedAt(), tracks);
    }
}
