package com.chmusic.musicserver.music;

public record MusicMetadata(String title, String artist, String album, Long duration, EmbeddedCover cover) {
    public static MusicMetadata empty() {
        return new MusicMetadata(null, null, null, null, null);
    }
}
