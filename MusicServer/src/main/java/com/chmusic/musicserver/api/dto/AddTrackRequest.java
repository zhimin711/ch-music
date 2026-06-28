package com.chmusic.musicserver.api.dto;

import jakarta.validation.constraints.Size;

public record AddTrackRequest(
        Long musicId,
        @Size(max = 40) String source,
        @Size(max = 120) String externalId,
        @Size(max = 500) String title,
        @Size(max = 300) String artist,
        @Size(max = 300) String album,
        @Size(max = 1000) String picUrl,
        Long duration) {
}
