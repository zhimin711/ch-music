package com.chmusic.musicserver.netease;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class NeteasePublicCache {
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public Optional<JsonNode> get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.body());
    }

    public void put(String key, JsonNode body, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        entries.put(key, new Entry(body, Instant.now().plus(ttl)));
    }

    private record Entry(JsonNode body, Instant expiresAt) {
    }
}
