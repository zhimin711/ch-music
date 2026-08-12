package com.chmusic.musicserver.netease;

import com.chmusic.musicserver.config.MusicServerProperties;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class NeteaseSettings {
    private final MusicServerProperties properties;

    public NeteaseSettings(MusicServerProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.getNetease().isEnabled();
    }

    public String baseUrl() {
        return properties.getNetease().getBaseUrl();
    }

    public Duration connectTimeout() {
        return properties.getNetease().getConnectTimeout();
    }

    public Duration readTimeout() {
        return properties.getNetease().getReadTimeout();
    }

    public int anonymousRequestsPerMinute() {
        return Math.max(1, properties.getNetease().getRateLimit().getAnonymousPerMinute());
    }

    public boolean cacheEnabled() {
        return properties.getNetease().getCache().isEnabled();
    }

    public Duration shortCacheTtl() {
        return properties.getNetease().getCache().getShortTtl();
    }

    public Duration mediumCacheTtl() {
        return properties.getNetease().getCache().getMediumTtl();
    }

    public Duration playbackCacheTtl() {
        return properties.getNetease().getCache().getPlaybackTtl();
    }
}
