package com.chmusic.musicserver.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    void avatarUploadStoresAndServesBlobFromDatabase() throws Exception {
        AuthResult owner = register("avatar-owner");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png",
                new byte[] { (byte) 0x89, 'P', 'N', 'G' });

        mockMvc.perform(multipart("/api/auth/me/avatar")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("http://localhost/api/auth/avatars/" + owner.userId()));

        mockMvc.perform(get("/api/auth/avatars/{userId}", owner.userId()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 4));

        mockMvc.perform(get("/api/auth/avatars/{userId}/{filename}", owner.userId(), "legacy.png"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
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
    void uploadReadsServerSideId3Metadata() throws Exception {
        AuthResult owner = register("metadata-owner");
        byte[] audio = id3v23Audio();
        MockMultipartFile file = new MockMultipartFile("file", "fallback.mp3", "audio/mpeg", audio);

        MvcResult upload = mockMvc.perform(multipart("/api/music")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Tagged Title")))
                .andExpect(jsonPath("$.artist", is("Tagged Artist")))
                .andExpect(jsonPath("$.album", is("Tagged Album")))
                .andExpect(jsonPath("$.duration", is(123456)))
                .andExpect(jsonPath("$.picUrl").exists())
                .andReturn();
        long musicId = objectMapper.readTree(upload.getResponse().getContentAsString()).get("musicId").asLong();

        mockMvc.perform(get("/api/music/{musicId}/cover", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
    }

    @Test
    void uploadRepairsChineseMetadataWithWrongIsoEncoding() throws Exception {
        AuthResult owner = register("metadata-encoding-owner");
        MockMultipartFile file = new MockMultipartFile("file", "mojibake.mp3", "audio/mpeg",
                id3v23GbkMislabeledAudio());

        mockMvc.perform(multipart("/api/music")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("中文歌名")))
                .andExpect(jsonPath("$.artist", is("中文歌手")))
                .andExpect(jsonPath("$.album", is("中文专辑")));
    }

    @Test
    void uploadReadsFlacVorbisCommentMetadata() throws Exception {
        AuthResult owner = register("metadata-flac-owner");
        MockMultipartFile file = new MockMultipartFile("file", "tagged.flac", "audio/flac", flacAudio());

        MvcResult upload = mockMvc.perform(multipart("/api/music")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("云里的歌")))
                .andExpect(jsonPath("$.artist", is("朝华歌手")))
                .andExpect(jsonPath("$.album", is("无损专辑")))
                .andExpect(jsonPath("$.duration", is(10000)))
                .andExpect(jsonPath("$.picUrl").exists())
                .andReturn();
        long musicId = objectMapper.readTree(upload.getResponse().getContentAsString()).get("musicId").asLong();

        mockMvc.perform(get("/api/music/{musicId}/cover", musicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
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

    private static byte[] id3v23Audio() throws Exception {
        ByteArrayOutputStream frames = new ByteArrayOutputStream();
        writeTextFrame(frames, "TIT2", "Tagged Title");
        writeTextFrame(frames, "TPE1", "Tagged Artist");
        writeTextFrame(frames, "TALB", "Tagged Album");
        writeTextFrame(frames, "TLEN", "123456");
        writeCoverFrame(frames);
        byte[] tag = frames.toByteArray();

        ByteArrayOutputStream audio = new ByteArrayOutputStream();
        audio.write(new byte[] { 'I', 'D', '3', 3, 0, 0 });
        audio.write(synchsafe(tag.length));
        audio.write(tag);
        audio.write("audio-frame-bytes".getBytes(StandardCharsets.UTF_8));
        return audio.toByteArray();
    }

    private static void writeTextFrame(ByteArrayOutputStream output, String id, String value) throws Exception {
        byte[] text = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(3);
        payload.write(text);
        writeFrame(output, id, payload.toByteArray());
    }

    private static byte[] id3v23GbkMislabeledAudio() throws Exception {
        ByteArrayOutputStream frames = new ByteArrayOutputStream();
        writeGbkAsIsoTextFrame(frames, "TIT2", "中文歌名");
        writeGbkAsIsoTextFrame(frames, "TPE1", "中文歌手");
        writeGbkAsIsoTextFrame(frames, "TALB", "中文专辑");
        byte[] tag = frames.toByteArray();

        ByteArrayOutputStream audio = new ByteArrayOutputStream();
        audio.write(new byte[] { 'I', 'D', '3', 3, 0, 0 });
        audio.write(synchsafe(tag.length));
        audio.write(tag);
        audio.write("audio-frame-bytes".getBytes(StandardCharsets.UTF_8));
        return audio.toByteArray();
    }

    private static void writeGbkAsIsoTextFrame(ByteArrayOutputStream output, String id, String value) throws Exception {
        byte[] text = value.getBytes(Charset.forName("GB18030"));
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(0);
        payload.write(text);
        writeFrame(output, id, payload.toByteArray());
    }

    private static byte[] flacAudio() throws Exception {
        ByteArrayOutputStream audio = new ByteArrayOutputStream();
        audio.write(new byte[] { 'f', 'L', 'a', 'C' });
        writeFlacBlock(audio, 0, false, flacStreamInfoBlock());
        writeFlacBlock(audio, 4, true, flacVorbisCommentBlock());
        audio.write("flac-frame-bytes".getBytes(StandardCharsets.UTF_8));
        return audio.toByteArray();
    }

    private static byte[] flacStreamInfoBlock() {
        byte[] block = new byte[34];
        int sampleRate = 44_100;
        long totalSamples = 441_000L;
        long packed = ((long) sampleRate << 44)
                | (1L << 41)
                | (15L << 36)
                | totalSamples;
        for (int index = 0; index < 8; index += 1) {
            block[10 + index] = (byte) (packed >>> (56 - (index * 8)));
        }
        return block;
    }

    private static byte[] flacVorbisCommentBlock() throws Exception {
        ByteArrayOutputStream block = new ByteArrayOutputStream();
        byte[] vendor = "MusicServerTest".getBytes(StandardCharsets.UTF_8);
        writeLittleEndianInt(block, vendor.length);
        block.write(vendor);
        String[] comments = {
                "TITLE=云里的歌",
                "ARTIST=朝华歌手",
                "ALBUM=无损专辑",
                "METADATA_BLOCK_PICTURE=" + Base64.getEncoder().encodeToString(flacPictureBlock())
        };
        writeLittleEndianInt(block, comments.length);
        for (String comment : comments) {
            byte[] bytes = comment.getBytes(StandardCharsets.UTF_8);
            writeLittleEndianInt(block, bytes.length);
            block.write(bytes);
        }
        return block.toByteArray();
    }

    private static byte[] flacPictureBlock() throws Exception {
        ByteArrayOutputStream block = new ByteArrayOutputStream();
        writeBigEndianInt(block, 3);
        byte[] mime = "image/png".getBytes(StandardCharsets.ISO_8859_1);
        writeBigEndianInt(block, mime.length);
        block.write(mime);
        writeBigEndianInt(block, 0);
        writeBigEndianInt(block, 1);
        writeBigEndianInt(block, 1);
        writeBigEndianInt(block, 24);
        writeBigEndianInt(block, 0);
        byte[] image = new byte[] { (byte) 0x89, 'P', 'N', 'G' };
        writeBigEndianInt(block, image.length);
        block.write(image);
        return block.toByteArray();
    }

    private static void writeFlacBlock(ByteArrayOutputStream output, int type, boolean last, byte[] payload)
            throws Exception {
        output.write((last ? 0x80 : 0) | (type & 0x7f));
        output.write((payload.length >> 16) & 0xff);
        output.write((payload.length >> 8) & 0xff);
        output.write(payload.length & 0xff);
        output.write(payload);
    }

    private static void writeLittleEndianInt(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 24) & 0xff);
    }

    private static void writeBigEndianInt(ByteArrayOutputStream output, int value) {
        output.write((value >> 24) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 8) & 0xff);
        output.write(value & 0xff);
    }

    private static void writeCoverFrame(ByteArrayOutputStream output) throws Exception {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(3);
        payload.write("image/png".getBytes(StandardCharsets.ISO_8859_1));
        payload.write(0);
        payload.write(3);
        payload.write(0);
        payload.write(new byte[] { (byte) 0x89, 'P', 'N', 'G' });
        writeFrame(output, "APIC", payload.toByteArray());
    }

    private static void writeFrame(ByteArrayOutputStream output, String id, byte[] payload) throws Exception {
        output.write(id.getBytes(StandardCharsets.ISO_8859_1));
        output.write(ByteBuffer.allocate(4).putInt(payload.length).array());
        output.write(new byte[] { 0, 0 });
        output.write(payload);
    }

    private static byte[] synchsafe(int value) {
        return new byte[] {
                (byte) ((value >> 21) & 0x7f),
                (byte) ((value >> 14) & 0x7f),
                (byte) ((value >> 7) & 0x7f),
                (byte) (value & 0x7f)
        };
    }

    private record AuthResult(String token, long userId, String username) {
    }

    private record UploadedMusic(long musicId) {
    }
}
