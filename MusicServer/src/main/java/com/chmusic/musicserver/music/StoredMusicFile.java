package com.chmusic.musicserver.music;

public record StoredMusicFile(String originalFilename, String storagePath, String contentType, long fileSize,
        String checksum) {
}
