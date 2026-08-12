package com.chmusic.musicserver.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(length = 1000)
    private String avatarUrl;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(columnDefinition = "bytea")
    private byte[] avatarData;

    @Column(length = 120)
    private String avatarContentType;

    @Column(length = 255)
    private String avatarFilename;

    private Long avatarSize;

    private Instant avatarUpdatedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
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

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public byte[] getAvatarData() {
        return avatarData;
    }

    public String getAvatarContentType() {
        return avatarContentType;
    }

    public String getAvatarFilename() {
        return avatarFilename;
    }

    public Long getAvatarSize() {
        return avatarSize;
    }

    public Instant getAvatarUpdatedAt() {
        return avatarUpdatedAt;
    }

    public void updateProfile(String displayName, String avatarUrl) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }

    public void updateAvatar(String avatarUrl, StoredAvatar avatar) {
        this.avatarUrl = avatarUrl;
        this.avatarData = avatar.bytes();
        this.avatarContentType = avatar.contentType();
        this.avatarFilename = avatar.filename();
        this.avatarSize = avatar.fileSize();
        this.avatarUpdatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
