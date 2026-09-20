package com.chmusic.musicserver.netease;

import com.chmusic.musicserver.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class NeteasePublicService {
    private static final Logger log = LoggerFactory.getLogger(NeteasePublicService.class);

    private final NeteasePublicClient client;
    private final NeteasePublicCache cache;
    private final NeteasePublicRateLimiter rateLimiter;
    private final NeteaseSettings settings;

    public NeteasePublicService(NeteasePublicClient client, NeteasePublicCache cache,
            NeteasePublicRateLimiter rateLimiter, NeteaseSettings settings) {
        this.client = client;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        this.settings = settings;
    }

    public JsonNode fetch(NeteasePublicEndpoint endpoint, MultiValueMap<String, String> params,
            HttpServletRequest request, Authentication authentication) {
        if (!client.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Netease public API is disabled");
        }

        String actorKey = actorKey(request, authentication);
        String rateKey = actorKey + ":" + endpoint.name();
        if (!rateLimiter.allow(rateKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many Netease public API requests");
        }

        MultiValueMap<String, String> filteredParams = filterParams(endpoint, params);
        String cacheKey = cacheKey(endpoint, filteredParams);
        if (settings.cacheEnabled()) {
            JsonNode cached = cache.get(cacheKey).orElse(null);
            if (cached != null) {
                return cached;
            }
        }

        String traceId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        try {
            JsonNode body = client.fetch(endpoint, filteredParams, traceId);
            if (settings.cacheEnabled()) {
                Duration ttl = endpoint.ttl(settings);
                cache.put(cacheKey, body, ttl);
            }
            log.info("netease public request traceId={} actor={} endpoint={} durationMs={} status=ok",
                    traceId, actorKey, endpoint.name(), Duration.between(startedAt, Instant.now()).toMillis());
            return body;
        } catch (RuntimeException ex) {
            log.warn("netease public request traceId={} actor={} endpoint={} durationMs={} status=failed",
                    traceId, actorKey, endpoint.name(), Duration.between(startedAt, Instant.now()).toMillis());
            throw ex;
        }
    }

    private static MultiValueMap<String, String> filterParams(NeteasePublicEndpoint endpoint,
            MultiValueMap<String, String> params) {
        LinkedMultiValueMap<String, String> filtered = new LinkedMultiValueMap<>();
        for (String name : params.keySet()) {
            if (!endpoint.allowedParams().contains(name)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported parameter for Netease public endpoint: " + name);
            }
            filtered.put(name, params.get(name));
        }
        return filtered;
    }

    private static String cacheKey(NeteasePublicEndpoint endpoint, MultiValueMap<String, String> params) {
        StringBuilder key = new StringBuilder(endpoint.name());
        params.keySet().stream().sorted().forEach(name -> {
            key.append('|').append(name).append('=');
            params.get(name).stream().sorted(Comparator.naturalOrder()).forEach(value -> key.append(value).append(','));
        });
        return key.toString();
    }

    private static String actorKey(HttpServletRequest request, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AppUser user) {
            return "user:" + user.getId();
        }
        return "ip:" + clientIp(request);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
