package com.chmusic.musicserver.api.dto;

import java.time.Instant;

public record AuthResponse(String tokenType, String accessToken, Instant expiresAt, UserResponse user) {
}
