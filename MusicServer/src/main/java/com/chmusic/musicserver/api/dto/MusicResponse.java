package com.chmusic.musicserver.api.dto;

import com.chmusic.musicserver.music.MusicFile;
import java.time.Instant;

public record MusicResponse(Long id, String title, String artist, String album, String originalFilename,
        String contentType, long fileSize, String checksum, Instant createdAt, String streamUrl) {
    public static MusicResponse from(MusicFile music) {
        return new MusicResponse(
                music.getId(),
                music.getTitle(),
                music.getArtist(),
                music.getAlbum(),
                music.getOriginalFilename(),
                music.getContentType(),
                music.getFileSize(),
                music.getChecksum(),
                music.getCreatedAt(),
                "/api/music/" + music.getId() + "/stream");
    }
}
