package com.chmusic.musicserver.music;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "music_files")
public class MusicFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, unique = true)
    private String storagePath;

    @Column(nullable = false)
    private String title;

    private String artist;

    private String album;

    @Column(length = 2000)
    private String coverPath;

    @Column(length = 120)
    private String coverContentType;

    private Long duration;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected MusicFile() {
    }

    public MusicFile(AppUser owner, String originalFilename, String storagePath, String title, String artist,
            String album, String contentType, long fileSize, String checksum) {
        this(owner, originalFilename, storagePath, title, artist, album, null, null, null, contentType, fileSize,
                checksum);
    }

    public MusicFile(AppUser owner, String originalFilename, String storagePath, String title, String artist,
            String album, String coverPath, String coverContentType, Long duration, String contentType, long fileSize,
            String checksum) {
        this.owner = owner;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.coverPath = coverPath;
        this.coverContentType = coverContentType;
        this.duration = duration;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.checksum = checksum;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoragePath() {
        return storagePath;
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

    public String getCoverPath() {
        return coverPath;
    }

    public String getCoverContentType() {
        return coverContentType;
    }

    public Long getDuration() {
        return duration;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
