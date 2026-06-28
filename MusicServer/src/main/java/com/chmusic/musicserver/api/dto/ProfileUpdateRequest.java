package com.chmusic.musicserver.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 1000) String avatarUrl) {
}
