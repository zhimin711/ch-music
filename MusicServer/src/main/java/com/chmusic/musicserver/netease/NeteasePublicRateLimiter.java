package com.chmusic.musicserver.netease;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NeteasePublicRateLimiter {
    private static final long WINDOW_MILLIS = 60_000;

    private final NeteaseSettings settings;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    @Autowired
    public NeteasePublicRateLimiter(NeteaseSettings settings) {
        this(settings, Clock.systemUTC());
    }

    NeteasePublicRateLimiter(NeteaseSettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    public boolean allow(String key) {
        long now = clock.millis();
        Deque<Long> bucket = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && now - bucket.peekFirst() >= WINDOW_MILLIS) {
                bucket.removeFirst();
            }
            if (bucket.size() >= settings.anonymousRequestsPerMinute()) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
    }
}
