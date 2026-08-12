package com.chmusic.musicserver.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class TranscodeApiTests {
    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();
    private static final byte[] AUDIO_BYTES = "0123456789abcdef".getBytes();
    private static final Path TEST_ROOT = createTestRoot();
    private static final Path FAKE_FFMPEG = createFakeFfmpeg();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void transcodeProperties(DynamicPropertyRegistry registry) {
        registry.add("music.storage.root", () -> TEST_ROOT.resolve("storage").toString());
        registry.add("music.transcoding.enabled", () -> "true");
        registry.add("music.transcoding.ffmpeg-path", FAKE_FFMPEG::toString);
        registry.add("music.transcoding.temp-root", () -> TEST_ROOT.resolve("temp").toString());
        registry.add("music.transcoding.cache-root", () -> TEST_ROOT.resolve("cache").toString());
        registry.add("music.transcoding.cache-max-size", () -> "1GB");
        registry.add("music.transcoding.profiles[0].id", () -> "aac-128");
        registry.add("music.transcoding.profiles[0].label", () -> "AAC 128");
        registry.add("music.transcoding.profiles[0].content-type", () -> "audio/aac");
        registry.add("music.transcoding.profiles[0].bitrate-kbps", () -> "128");
        registry.add("music.transcoding.profiles[0].extension", () -> "aac");
        registry.add("music.transcoding.profiles[0].args", () -> "-vn -c:a copy");
    }

    @Test
    void transcodeCapabilitiesExposeConfiguredProfiles() throws Exception {
        String token = register("capabilities");

        mockMvc.perform(get("/api/music/transcode-capabilities")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.toolAvailable", is(true)))
                .andExpect(jsonPath("$.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.profiles[0].profileId", is("aac-128")))
                .andExpect(jsonPath("$.profiles[0].contentType", is("audio/aac")));
    }

    @Test
    void prepareStatusAndStreamTranscodedProfile() throws Exception {
        UploadedMusic upload = uploadMusic("flow");

        mockMvc.perform(post("/api/music/{musicId}/transcodes/{profileId}", upload.musicId(), "aac-128")
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.profileId", is("aac-128")))
                .andExpect(jsonPath("$.streamUrl", is("/api/music/" + upload.musicId() + "/stream?profile=aac-128")));

        awaitReady(upload.token(), upload.musicId(), "aac-128");

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .param("profile", "aac-128")
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token()))
                        .header(HttpHeaders.RANGE, "bytes=0-4"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "audio/aac"))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-4/18"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 5));
    }

    @Test
    void prepareUnknownProfileReturnsStructuredFailedState() throws Exception {
        UploadedMusic upload = uploadMusic("unknown");

        mockMvc.perform(post("/api/music/{musicId}/transcodes/{profileId}", upload.musicId(), "missing-profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state", is("FAILED")))
                .andExpect(jsonPath("$.reason", is("PROFILE_NOT_FOUND")));
    }

    private void awaitReady(String token, long musicId, String profileId) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        String lastState;
        do {
            MvcResult result = mockMvc.perform(get("/api/music/{musicId}/transcodes/{profileId}", musicId, profileId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            lastState = body.get("state").asText();
            if ("READY".equals(lastState)) {
                return;
            }
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Transcode did not become READY, last state was " + lastState);
    }

    private UploadedMusic uploadMusic(String usernamePrefix) throws Exception {
        String token = register(usernamePrefix);
        MockMultipartFile file = new MockMultipartFile("file", "song.mp3", "audio/mpeg", AUDIO_BYTES);
        MvcResult upload = mockMvc.perform(multipart("/api/music")
                        .file(file)
                        .param("title", "Song")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        long musicId = objectMapper.readTree(upload.getResponse().getContentAsString()).get("musicId").asLong();
        return new UploadedMusic(token, musicId);
    }

    private String register(String usernamePrefix) throws Exception {
        int id = USER_SEQUENCE.incrementAndGet();
        String payload = """
                {
                  "username": "%s-%d",
                  "password": "password123",
                  "displayName": "Test User"
                }
                """.formatted(usernamePrefix, id);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static Path createTestRoot() {
        try {
            return Files.createTempDirectory("musicserver-transcode-api-test-");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static Path createFakeFfmpeg() {
        try {
            Path script = TEST_ROOT.resolve("fake-ffmpeg.sh");
            Files.writeString(script, """
                    #!/bin/sh
                    if [ "$1" = "-version" ]; then
                      exit 0
                    fi
                    out=""
                    for arg in "$@"; do
                      out="$arg"
                    done
                    printf 'transcoded-by-test' > "$out"
                    exit 0
                    """);
            script.toFile().setExecutable(true);
            return script;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private record UploadedMusic(String token, long musicId) {
    }
}
