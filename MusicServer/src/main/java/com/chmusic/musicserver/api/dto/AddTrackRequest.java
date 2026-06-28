package com.chmusic.musicserver.api.dto;

import jakarta.validation.constraints.NotNull;

public record AddTrackRequest(@NotNull Long musicId) {
}
