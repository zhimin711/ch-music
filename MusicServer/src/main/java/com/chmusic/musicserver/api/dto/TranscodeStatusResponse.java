package com.chmusic.musicserver.api.dto;

import com.chmusic.musicserver.music.TranscodeProfileCatalog;
import com.chmusic.musicserver.music.TranscodeService;
import java.util.List;

public record TranscodeStatusResponse(Long musicId, String profileId, String state, String reason, String streamUrl,
        Integer retryAfterSeconds, Long fileSize, String checksum) {
    public static TranscodeStatusResponse from(TranscodeService.TranscodeStatus status) {
        return new TranscodeStatusResponse(status.musicId(), status.profileId(), status.state().name(),
                status.reason(), status.streamUrl(), status.retryAfterSeconds(), status.fileSize(), status.checksum());
    }

    public record CapabilitiesResponse(boolean enabled, boolean toolAvailable, String status, String reason,
            List<ProfileResponse> profiles) {
        public static CapabilitiesResponse from(TranscodeProfileCatalog.CapabilitySnapshot snapshot) {
            return new CapabilitiesResponse(snapshot.enabled(), snapshot.toolAvailable(), snapshot.status().name(),
                    snapshot.reason(), snapshot.profiles().stream().map(ProfileResponse::from).toList());
        }
    }

    public record ProfileResponse(String profileId, String label, String contentType, Integer bitrateKbps,
            String extension, boolean offlineCacheable) {
        private static ProfileResponse from(TranscodeProfileCatalog.ProfileDescriptor profile) {
            return new ProfileResponse(profile.id(), profile.label(), profile.contentType(), profile.bitrateKbps(),
                    profile.extension(), profile.offlineCacheable());
        }
    }
}
