package com.chmusic.musicserver.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
class NeteasePublicControllerTests {
    private static final ConcurrentHashMap<String, AtomicInteger> REQUEST_COUNTS = new ConcurrentHashMap<>();
    private static final HttpServer SIDECAR = startSidecar();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void neteaseProperties(DynamicPropertyRegistry registry) {
        registry.add("music.netease.base-url",
                () -> "http://127.0.0.1:" + SIDECAR.getAddress().getPort());
        registry.add("music.netease.rate-limit.anonymous-per-minute", () -> "100");
    }

    @AfterAll
    static void stopSidecar() {
        SIDECAR.stop(0);
    }

    @Test
    void publicSearchAllowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/netease/public/search").param("keywords", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.path").value("/cloudsearch"));
    }

    @Test
    void publicSearchIgnoresInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/api/netease/public/search")
                        .param("keywords", "test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-public-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.path").value("/cloudsearch"));
    }

    @Test
    void publicEndpointRejectsUnsupportedParameters() throws Exception {
        mockMvc.perform(get("/api/netease/public/search").param("keywords", "test").param("cookie", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicEndpointCachesReadonlyResponses() throws Exception {
        REQUEST_COUNTS.remove("/lyric");

        mockMvc.perform(get("/api/netease/public/lyric").param("id", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/netease/public/lyric").param("id", "1"))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(REQUEST_COUNTS.get("/lyric").get()).isEqualTo(1);
    }

    @Test
    void privateMusicStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/music"))
                .andExpect(status().isUnauthorized());
    }

    private static HttpServer startSidecar() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                REQUEST_COUNTS.computeIfAbsent(path, ignored -> new AtomicInteger()).incrementAndGet();
                byte[] body = ("""
                        {"ok":true,"path":"%s"}
                        """.formatted(path)).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start mock Netease sidecar", ex);
        }
    }
}
