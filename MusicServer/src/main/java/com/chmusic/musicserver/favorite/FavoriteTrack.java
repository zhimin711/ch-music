package com.chmusic.musicserver.favorite;

import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.user.AppUser;
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
@Table(name = "favorite_tracks")
public class FavoriteTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "music_id", nullable = false)
    private MusicFile music;

    @Column(nullable = false)
    private Instant createdAt;

    protected FavoriteTrack() {
    }

    public FavoriteTrack(AppUser owner, MusicFile music) {
        this.owner = owner;
        this.music = music;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
