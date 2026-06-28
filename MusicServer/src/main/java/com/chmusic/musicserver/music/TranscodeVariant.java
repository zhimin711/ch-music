package com.chmusic.musicserver.music;

import com.chmusic.musicserver.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "transcode_variants",
        uniqueConstraints = @UniqueConstraint(name = "uk_transcode_variant_owner_music_profile",
                columnNames = { "owner_id", "music_id", "profile_id" }),
        indexes = {
                @Index(name = "idx_transcode_variant_owner_status", columnList = "owner_id,status"),
                @Index(name = "idx_transcode_variant_status_access", columnList = "status,last_accessed_at")
        })
public class TranscodeVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "music_id", nullable = false)
    private MusicFile music;

    @Column(name = "profile_id", nullable = false, length = 120)
    private String profileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Status status = Status.QUEUED;

    @Column(length = 2000)
    private String storagePath;

    private Long fileSize;

    @Column(length = 64)
    private String checksum;

    @Column(length = 1000)
    private String errorMessage;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected TranscodeVariant() {
    }

    public TranscodeVariant(AppUser owner, MusicFile music, String profileId) {
        this.owner = owner;
        this.music = music;
        this.profileId = profileId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        lastAccessedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markQueued() {
        status = Status.QUEUED;
        errorMessage = null;
    }

    public void markProcessing() {
        status = Status.PROCESSING;
        errorMessage = null;
    }

    public void markReady(String storagePath, long fileSize, String checksum) {
        this.status = Status.READY;
        this.storagePath = storagePath;
        this.fileSize = fileSize;
        this.checksum = checksum;
        this.errorMessage = null;
        touchAccessedAt();
    }

    public void markFailed(String errorMessage) {
        status = Status.FAILED;
        this.errorMessage = trimError(errorMessage);
    }

    public void markStale(String errorMessage) {
        status = Status.STALE;
        this.errorMessage = trimError(errorMessage);
    }

    public void touchAccessedAt() {
        lastAccessedAt = Instant.now();
    }

    private static String trimError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        return errorMessage.length() <= 1000 ? errorMessage : errorMessage.substring(0, 1000);
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public MusicFile getMusic() {
        return music;
    }

    public String getProfileId() {
        return profileId;
    }

    public Status getStatus() {
        return status;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum Status {
        QUEUED,
        PROCESSING,
        READY,
        FAILED,
        STALE
    }
}
