package com.chmusic.musicserver.music;

import com.chmusic.musicserver.config.MusicServerProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TranscodeProfileCatalog {
    private final MusicServerProperties properties;
    private final TranscodeToolProbe toolProbe;

    public TranscodeProfileCatalog(MusicServerProperties properties, TranscodeToolProbe toolProbe) {
        this.properties = properties;
        this.toolProbe = toolProbe;
    }

    public CapabilitySnapshot capabilities() {
        MusicServerProperties.Transcoding transcoding = properties.getTranscoding();
        List<ProfileDescriptor> profiles = profiles();
        if (!transcoding.isEnabled()) {
            return new CapabilitySnapshot(false, false, CatalogStatus.DISABLED, "TRANSCODING_DISABLED", profiles);
        }

        TranscodeToolProbe.ProbeResult probe = toolProbe.probe(transcoding.getFfmpegPath());
        if (!probe.available()) {
            return new CapabilitySnapshot(true, false, CatalogStatus.TOOL_UNAVAILABLE, probe.reason(), profiles);
        }

        return new CapabilitySnapshot(true, true, CatalogStatus.AVAILABLE, "AVAILABLE", profiles);
    }

    public ProfileResolution resolve(String profileId) {
        CapabilitySnapshot snapshot = capabilities();
        if (snapshot.status() == CatalogStatus.DISABLED) {
            return new ProfileResolution(CatalogStatus.DISABLED, snapshot.reason(), Optional.empty());
        }

        Optional<ProfileDescriptor> profile = find(profileId);
        if (profile.isEmpty()) {
            return new ProfileResolution(CatalogStatus.PROFILE_NOT_FOUND, "PROFILE_NOT_FOUND", Optional.empty());
        }

        if (!snapshot.toolAvailable()) {
            return new ProfileResolution(snapshot.status(), snapshot.reason(), Optional.empty());
        }

        return new ProfileResolution(CatalogStatus.AVAILABLE, "AVAILABLE", profile);
    }

    public Optional<ProfileDescriptor> find(String profileId) {
        if (!StringUtils.hasText(profileId)) {
            return Optional.empty();
        }
        String normalizedProfileId = profileId.trim();
        return profiles().stream()
                .filter(profile -> profile.id().equals(normalizedProfileId))
                .findFirst();
    }

    public List<ProfileDescriptor> profiles() {
        return properties.getTranscoding().getProfiles().stream()
                .filter(profile -> StringUtils.hasText(profile.getId()))
                .map(ProfileDescriptor::from)
                .toList();
    }

    public enum CatalogStatus {
        AVAILABLE,
        DISABLED,
        TOOL_UNAVAILABLE,
        PROFILE_NOT_FOUND
    }

    public record CapabilitySnapshot(boolean enabled, boolean toolAvailable, CatalogStatus status, String reason,
            List<ProfileDescriptor> profiles) {
    }

    public record ProfileResolution(CatalogStatus status, String reason, Optional<ProfileDescriptor> profile) {
    }

    public record ProfileDescriptor(String id, String label, String contentType, Integer bitrateKbps, String extension,
            String args, boolean offlineCacheable) {
        private static ProfileDescriptor from(MusicServerProperties.Profile profile) {
            String id = profile.getId().trim();
            String label = StringUtils.hasText(profile.getLabel()) ? profile.getLabel().trim() : id;
            String contentType = StringUtils.hasText(profile.getContentType()) ? profile.getContentType().trim() : null;
            String extension = StringUtils.hasText(profile.getExtension()) ? profile.getExtension().trim() : null;
            String args = StringUtils.hasText(profile.getArgs()) ? profile.getArgs().trim() : null;
            return new ProfileDescriptor(id, label, contentType, profile.getBitrateKbps(), extension, args,
                    profile.isOfflineCacheable());
        }
    }
}
