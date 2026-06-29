package com.chmusic.musicserver.api.dto;

import com.chmusic.musicserver.favorite.FavoriteTrack;
import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.playlist.PlaylistTrack;
import java.time.Instant;
import java.util.List;

public record MusicResponse(String id, Long musicId, Long trackId, String source, String externalId, String title,
        String artist, String album, String picUrl, Long duration, String originalFilename, String contentType,
        long fileSize, String checksum, Instant createdAt, Instant updatedAt, String streamUrl,
        PlaybackCapabilities playback) {
    private static final String ORIGINAL_PROFILE_ID = "original";
    private static final String READY_STATE = "READY";

    public static MusicResponse from(MusicFile music) {
        String streamUrl = "/api/music/" + music.getId() + "/stream";
        return new MusicResponse(
                String.valueOf(music.getId()),
                music.getId(),
                null,
                "musicServer",
                null,
                music.getTitle(),
                music.getArtist(),
                music.getAlbum(),
                null,
                null,
                music.getOriginalFilename(),
                music.getContentType(),
                music.getFileSize(),
                music.getChecksum(),
                music.getCreatedAt(),
                music.getUpdatedAt(),
                streamUrl,
                PlaybackCapabilities.forOriginal(music, streamUrl));
    }

    public static MusicResponse from(PlaylistTrack track) {
        MusicFile music = track.getMusic();
        if (music != null) {
            MusicResponse response = from(music);
            return new MusicResponse(response.id(), response.musicId(), track.getId(), response.source(),
                    response.externalId(), response.title(), response.artist(), response.album(), response.picUrl(),
                    response.duration(), response.originalFilename(), response.contentType(), response.fileSize(),
                    response.checksum(), response.createdAt(), response.updatedAt(), response.streamUrl(),
                    response.playback());
        }

        return new MusicResponse(
                track.getExternalId(),
                null,
                track.getId(),
                track.getExternalSource(),
                track.getExternalId(),
                track.getTitle(),
                track.getArtist(),
                track.getAlbum(),
                track.getPicUrl(),
                track.getDuration(),
                track.getTitle(),
                "external/" + track.getExternalSource(),
                0,
                "",
                track.getCreatedAt(),
                track.getCreatedAt(),
                null,
                PlaybackCapabilities.unavailable());
    }

    public static MusicResponse from(FavoriteTrack favorite) {
        MusicFile music = favorite.getMusic();
        if (music != null) {
            return from(music);
        }

        return new MusicResponse(
                favorite.getExternalId(),
                null,
                favorite.getId(),
                favorite.getExternalSource(),
                favorite.getExternalId(),
                favorite.getTitle(),
                favorite.getArtist(),
                favorite.getAlbum(),
                favorite.getPicUrl(),
                favorite.getDuration(),
                favorite.getTitle(),
                "external/" + favorite.getExternalSource(),
                0,
                "",
                favorite.getCreatedAt(),
                favorite.getCreatedAt(),
                null,
                PlaybackCapabilities.unavailable());
    }

    public record PlaybackCapabilities(boolean supportsRange, boolean supportsOriginal, boolean supportsTranscoding,
            boolean supportsOfflineCache, List<PlaybackVariant> variants) {
        public static PlaybackCapabilities forOriginal(MusicFile music, String streamUrl) {
            PlaybackVariant original = new PlaybackVariant(
                    ORIGINAL_PROFILE_ID,
                    "Original",
                    music.getContentType(),
                    null,
                    streamUrl,
                    READY_STATE,
                    music.getFileSize(),
                    music.getChecksum());
            return new PlaybackCapabilities(true, true, false, true, List.of(original));
        }

        public static PlaybackCapabilities unavailable() {
            return new PlaybackCapabilities(false, false, false, false, List.of());
        }
    }

    public record PlaybackVariant(String profileId, String label, String contentType, Integer bitrateKbps,
            String streamUrl, String state, Long fileSize, String checksum) {
    }
}
