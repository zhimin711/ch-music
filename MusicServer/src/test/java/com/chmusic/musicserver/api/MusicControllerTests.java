package com.chmusic.musicserver.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.music.MusicFileRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MusicControllerTests {
    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();
    private static final byte[] AUDIO_BYTES = "0123456789abcdef".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MusicFileRepository musicRepository;

    @Test
    void streamWithoutRangeReturnsFullContent() throws Exception {
        UploadedMusic upload = uploadMusic("full");

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, AUDIO_BYTES.length))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "audio/mpeg"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("song.mp3")));
    }

    @Test
    void streamWithClosedRangeReturnsPartialContent() throws Exception {
        UploadedMusic upload = uploadMusic("closed");

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token()))
                        .header(HttpHeaders.RANGE, "bytes=2-5"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 2-5/" + AUDIO_BYTES.length))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 4));
    }

    @Test
    void streamWithOpenEndedRangeReturnsPartialContent() throws Exception {
        UploadedMusic upload = uploadMusic("open");

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token()))
                        .header(HttpHeaders.RANGE, "bytes=10-"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 10-15/" + AUDIO_BYTES.length))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 6));
    }

    @Test
    void streamWithSuffixRangeReturnsPartialContent() throws Exception {
        UploadedMusic upload = uploadMusic("suffix");

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token()))
                        .header(HttpHeaders.RANGE, "bytes=-4"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 12-15/" + AUDIO_BYTES.length))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 4));
    }

    @Test
    void streamWithInvalidRangeReturnsRangeNotSatisfiable() throws Exception {
        UploadedMusic upload = uploadMusic("invalid");

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token()))
                        .header(HttpHeaders.RANGE, "bytes=999-1000"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */" + AUDIO_BYTES.length));
    }

    @Test
    void streamWithMultipleRangesReturnsRangeNotSatisfiable() throws Exception {
        UploadedMusic upload = uploadMusic("multi");

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token()))
                        .header(HttpHeaders.RANGE, "bytes=0-1,2-3"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */" + AUDIO_BYTES.length));
    }

    @Test
    void streamOwnedByAnotherUserReturnsNotFound() throws Exception {
        UploadedMusic upload = uploadMusic("owner");
        String otherToken = register("other");

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void streamMissingStoredFileReturnsNotFound() throws Exception {
        UploadedMusic upload = uploadMusic("missing");
        MusicFile music = musicRepository.findById(upload.musicId()).orElseThrow();
        Files.deleteIfExists(Path.of(music.getStoragePath()));

        mockMvc.perform(get("/api/music/{musicId}/stream", upload.musicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(upload.token())))
                .andExpect(status().isNotFound());
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
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record UploadedMusic(String token, long musicId) {
    }
}
