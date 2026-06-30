package com.chmusic.musicserver.netease;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

@Component
public class NeteasePublicClient {
    private static final Logger log = LoggerFactory.getLogger(NeteasePublicClient.class);

    private final RestClient restClient;
    private final NeteaseSettings settings;

    public NeteasePublicClient(NeteaseSettings settings) {
        this.settings = settings;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(settings.connectTimeout());
        requestFactory.setReadTimeout(settings.readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public JsonNode fetch(NeteasePublicEndpoint endpoint, MultiValueMap<String, String> params, String traceId) {
        URI uri = UriComponentsBuilder.fromPath(endpoint.sidecarPath())
                .queryParams(params)
                .encode()
                .build()
                .toUri();
        try {
            JsonNode body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Netease sidecar returned an empty body");
            }
            return body;
        } catch (ResourceAccessException ex) {
            log.warn("netease sidecar unavailable traceId={} endpoint={}", traceId, endpoint.name());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Netease sidecar is unavailable", ex);
        } catch (RestClientResponseException ex) {
            HttpStatus status = resolveStatus(ex);
            log.warn("netease sidecar error traceId={} endpoint={} status={}", traceId, endpoint.name(),
                    ex.getStatusCode().value());
            throw new ResponseStatusException(toPublicProxyStatus(status), "Netease sidecar request failed", ex);
        }
    }

    public boolean enabled() {
        return settings.enabled();
    }

    private static HttpStatus resolveStatus(RestClientResponseException ex) {
        try {
            return HttpStatus.valueOf(ex.getStatusCode().value());
        } catch (IllegalArgumentException ignored) {
            return HttpStatus.BAD_GATEWAY;
        }
    }

    private static HttpStatus toPublicProxyStatus(HttpStatus status) {
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            return HttpStatus.BAD_GATEWAY;
        }
        return status;
    }
}
