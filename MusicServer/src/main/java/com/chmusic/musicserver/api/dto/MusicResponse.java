package com.chmusic.musicserver.api.dto;

import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.playlist.PlaylistTrack;
import java.time.Instant;

public record MusicResponse(String id, Long musicId, Long trackId, String source, String externalId, String title,
        String artist, String album, String picUrl, Long duration, String originalFilename, String contentType,
        long fileSize, String checksum, Instant createdAt, String streamUrl) {
    public static MusicResponse from(MusicFile music) {
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
                "/api/music/" + music.getId() + "/stream");
    }

    public static MusicResponse from(PlaylistTrack track) {
        MusicFile music = track.getMusic();
        if (music != null) {
            MusicResponse response = from(music);
            return new MusicResponse(response.id(), response.musicId(), track.getId(), response.source(),
                    response.externalId(), response.title(), response.artist(), response.album(), response.picUrl(),
                    response.duration(), response.originalFilename(), response.contentType(), response.fileSize(),
                    response.checksum(), response.createdAt(), response.streamUrl());
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
                null);
    }
}
