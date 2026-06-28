package com.chmusic.musicserver.playlist;

import com.chmusic.musicserver.music.MusicFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "playlist_tracks")
public class PlaylistTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id")
    private MusicFile music;

    @Column(length = 40)
    private String externalSource;

    @Column(length = 120)
    private String externalId;

    @Column(length = 500)
    private String title;

    @Column(length = 300)
    private String artist;

    @Column(length = 300)
    private String album;

    @Column(length = 1000)
    private String picUrl;

    private Long duration;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private Instant createdAt;

    protected PlaylistTrack() {
    }

    public PlaylistTrack(Playlist playlist, MusicFile music, int sortOrder) {
        this.playlist = playlist;
        this.music = music;
        this.sortOrder = sortOrder;
    }

    public PlaylistTrack(Playlist playlist, String externalSource, String externalId, String title, String artist,
            String album, String picUrl, Long duration, int sortOrder) {
        this.playlist = playlist;
        this.externalSource = externalSource;
        this.externalId = externalId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.picUrl = picUrl;
        this.duration = duration;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public MusicFile getMusic() {
        return music;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public Long getDuration() {
        return duration;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
