package com.chmusic.musicserver.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MusicServerApiIntegrationTests {
    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();
    private static final byte[] AUDIO_BYTES = "test-audio-bytes".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginLogoutAndMeFollowTokenContract() throws Exception {
        String username = uniqueUsername("auth");
        String password = "password123";

        AuthResult registered = register(username, password, "Auth User");
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(registered.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)))
                .andExpect(jsonPath("$.displayName", is("Auth User")));

        AuthResult loggedIn = login(username, password);
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(loggedIn.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "wrong-password"
                                }
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(loggedIn.token())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(loggedIn.token())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadListDetailAndOwnerIsolationWorkTogether() throws Exception {
        AuthResult owner = register("music-owner");
        AuthResult other = register("music-other");

        long musicId = uploadMusic(owner.token(), "Cloud Song").musicId();

        mockMvc.perform(get("/api/music").header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].musicId", is((int) musicId)))
                .andExpect(jsonPath("$[0].source", is("musicServer")))
                .andExpect(jsonPath("$[0].streamUrl", is("/api/music/" + musicId + "/stream")));

        mockMvc.perform(get("/api/music/{musicId}", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Cloud Song")));

        mockMvc.perform(get("/api/music/{musicId}", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/music/{musicId}", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void playlistAddRemoveAndOwnerIsolationWorkTogether() throws Exception {
        AuthResult owner = register("playlist-owner");
        AuthResult other = register("playlist-other");
        long musicId = uploadMusic(owner.token(), "Playlist Song").musicId();

        long playlistId = createPlaylist(owner.token(), "My Playlist");

        MvcResult added = mockMvc.perform(post("/api/playlists/{playlistId}/tracks", playlistId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "musicId": %d
                                }
                                """.formatted(musicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks", hasSize(1)))
                .andExpect(jsonPath("$.tracks[0].musicId", is((int) musicId)))
                .andReturn();
        long trackId = objectMapper.readTree(added.getResponse().getContentAsString())
                .get("tracks")
                .get(0)
                .get("trackId")
                .asLong();

        mockMvc.perform(post("/api/playlists/{playlistId}/tracks", playlistId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "musicId": %d
                                }
                                """.formatted(musicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks", hasSize(1)));

        mockMvc.perform(get("/api/playlists/{playlistId}", playlistId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/playlists/{playlistId}/tracks", createPlaylist(other.token(), "Other Playlist"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "musicId": %d
                                }
                                """.formatted(musicId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/playlists/{playlistId}/tracks/{trackId}", playlistId, trackId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks", hasSize(0)));
    }

    @Test
    void favoritesAddRemoveAndOwnerIsolationWorkTogether() throws Exception {
        AuthResult owner = register("favorite-owner");
        AuthResult other = register("favorite-other");
        long musicId = uploadMusic(owner.token(), "Favorite Song").musicId();

        mockMvc.perform(post("/api/favorites/{musicId}", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].music.musicId", is((int) musicId)))
                .andExpect(jsonPath("$[0].music.title", is("Favorite Song")));

        mockMvc.perform(post("/api/favorites/{musicId}", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/favorites/{musicId}", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/favorites").header(HttpHeaders.AUTHORIZATION, bearer(other.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(delete("/api/favorites/{musicId}", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private AuthResult register(String usernamePrefix) throws Exception {
        return register(uniqueUsername(usernamePrefix), "password123", "Test User");
    }

    private AuthResult register(String username, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(username, password, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthResult(body.get("accessToken").asText(), body.get("user").get("id").asLong(), username);
    }

    private AuthResult login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthResult(body.get("accessToken").asText(), body.get("user").get("id").asLong(), username);
    }

    private UploadedMusic uploadMusic(String token, String title) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "song.mp3", "audio/mpeg", AUDIO_BYTES);
        MvcResult upload = mockMvc.perform(multipart("/api/music")
                        .file(file)
                        .param("title", title)
                        .param("artist", "Test Artist")
                        .param("album", "Test Album")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(upload.getResponse().getContentAsString());
        return new UploadedMusic(body.get("musicId").asLong());
    }

    private long createPlaylist(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/playlists")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Test playlist"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private static String uniqueUsername(String prefix) {
        return prefix + "-" + USER_SEQUENCE.incrementAndGet();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record AuthResult(String token, long userId, String username) {
    }

    private record UploadedMusic(long musicId) {
    }
}
