package com.chmusic.musicserver.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chmusic.musicserver.config.MusicServerProperties;
import com.chmusic.musicserver.user.AppUser;
import com.chmusic.musicserver.user.AppUserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class TranscodeServiceTests {
    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();
    private static final Path TEST_ROOT = createTestRoot();
    private static final Path FAKE_FFMPEG = createFakeFfmpeg();

    @Autowired
    private TranscodeService transcodeService;

    @Autowired
    private TranscodeCacheService cacheService;

    @Autowired
    private TranscodeVariantRepository variantRepository;

    @Autowired
    private MusicService musicService;

    @Autowired
    private MusicFileRepository musicRepository;

    @Autowired
    private AppUserRepository userRepository;

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
        registry.add("music.transcoding.profiles[1].id", () -> "broken");
        registry.add("music.transcoding.profiles[1].label", () -> "Broken");
        registry.add("music.transcoding.profiles[1].content-type", () -> "audio/aac");
        registry.add("music.transcoding.profiles[1].extension", () -> "aac");
        registry.add("music.transcoding.profiles[1].args", () -> "\"");
    }

    @Test
    void prepareCreatesReusableReadyVariant() throws Exception {
        MusicFixture fixture = createMusic("ready");

        TranscodeService.TranscodeStatus initial =
                transcodeService.prepare(fixture.owner(), fixture.music().getId(), "aac-128");
        assertEquals("aac-128", initial.profileId());
        assertTrue(initial.state() == TranscodeVariant.Status.QUEUED
                || initial.state() == TranscodeVariant.Status.PROCESSING);

        TranscodeService.TranscodeStatus ready = awaitStatus(fixture.owner(), fixture.music().getId(), "aac-128",
                TranscodeVariant.Status.READY);
        assertEquals("/api/music/" + fixture.music().getId() + "/stream?profile=aac-128", ready.streamUrl());
        assertEquals(18L, ready.fileSize());
        assertNotNull(ready.checksum());

        TranscodeService.TranscodeStatus reused =
                transcodeService.prepare(fixture.owner(), fixture.music().getId(), "aac-128");
        assertEquals(TranscodeVariant.Status.READY, reused.state());
        assertEquals(null, reused.retryAfterSeconds());
    }

    @Test
    void invalidProfileAndUnavailableToolReturnSafeReasons() throws Exception {
        MusicFixture fixture = createMusic("invalid-profile");

        TranscodeService.TranscodeStatus missing =
                transcodeService.prepare(fixture.owner(), fixture.music().getId(), "missing-profile");
        assertEquals(TranscodeVariant.Status.FAILED, missing.state());
        assertEquals("PROFILE_NOT_FOUND", missing.reason());

        MusicServerProperties properties = new MusicServerProperties();
        properties.getTranscoding().setEnabled(true);
        properties.getTranscoding().setFfmpegPath(TEST_ROOT.resolve("missing-ffmpeg").toString());
        MusicServerProperties.Profile profile = new MusicServerProperties.Profile();
        profile.setId("aac-128");
        properties.getTranscoding().setProfiles(java.util.List.of(profile));
        TranscodeProfileCatalog catalog = new TranscodeProfileCatalog(properties, new TranscodeToolProbe());

        assertEquals(TranscodeProfileCatalog.CatalogStatus.TOOL_UNAVAILABLE, catalog.capabilities().status());
        assertEquals("TOOL_NOT_EXECUTABLE", catalog.capabilities().reason());
    }

    @Test
    void failedProfileCanBeRetried() throws Exception {
        MusicFixture fixture = createMusic("failed");

        transcodeService.prepare(fixture.owner(), fixture.music().getId(), "broken");
        TranscodeService.TranscodeStatus failed = awaitStatus(fixture.owner(), fixture.music().getId(), "broken",
                TranscodeVariant.Status.FAILED);
        assertEquals("TRANSCODE_ARGS_INVALID", failed.reason());

        TranscodeService.TranscodeStatus retried =
                transcodeService.prepare(fixture.owner(), fixture.music().getId(), "broken");
        assertTrue(retried.state() == TranscodeVariant.Status.QUEUED
                || retried.state() == TranscodeVariant.Status.PROCESSING
                || retried.state() == TranscodeVariant.Status.FAILED);
    }

    @Test
    void checksumMismatchMarksVariantStale() throws Exception {
        MusicFixture fixture = createMusic("stale");
        transcodeService.prepare(fixture.owner(), fixture.music().getId(), "aac-128");
        awaitStatus(fixture.owner(), fixture.music().getId(), "aac-128", TranscodeVariant.Status.READY);

        TranscodeVariant variant = variantRepository
                .findByOwnerAndMusicAndProfileId(fixture.owner(), fixture.music(), "aac-128")
                .orElseThrow();
        Files.writeString(Path.of(variant.getStoragePath()), "changed-cache");

        assertFalse(cacheService.validateReady(variant));
        TranscodeVariant stale = variantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(TranscodeVariant.Status.STALE, stale.getStatus());
        assertEquals("CACHE_SIZE_MISMATCH", stale.getErrorMessage());
    }

    @Test
    void deleteMusicRemovesTranscodeCacheAndIndex() throws Exception {
        MusicFixture fixture = createMusic("delete");
        transcodeService.prepare(fixture.owner(), fixture.music().getId(), "aac-128");
        awaitStatus(fixture.owner(), fixture.music().getId(), "aac-128", TranscodeVariant.Status.READY);
        TranscodeVariant variant = variantRepository
                .findByOwnerAndMusicAndProfileId(fixture.owner(), fixture.music(), "aac-128")
                .orElseThrow();
        Path cachedFile = Path.of(variant.getStoragePath());
        assertTrue(Files.exists(cachedFile));

        musicService.delete(fixture.owner(), fixture.music().getId());

        assertFalse(Files.exists(cachedFile));
        assertTrue(variantRepository.findByOwnerAndMusicId(fixture.owner(), fixture.music().getId()).isEmpty());
    }

    private TranscodeService.TranscodeStatus awaitStatus(AppUser owner, Long musicId, String profileId,
            TranscodeVariant.Status expected) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        TranscodeService.TranscodeStatus status;
        do {
            status = transcodeService.status(owner, musicId, profileId);
            if (status.state() == expected) {
                return status;
            }
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        return status;
    }

    private MusicFixture createMusic(String prefix) throws IOException {
        int id = USER_SEQUENCE.incrementAndGet();
        AppUser owner = userRepository.save(new AppUser(prefix + "-" + id, "password", "Test User"));
        Path source = TEST_ROOT.resolve("storage").resolve("sources").resolve(prefix + "-" + id + ".mp3");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "source-audio");
        MusicFile music = musicRepository.save(new MusicFile(owner, source.getFileName().toString(),
                source.toString(), "Song", null, null, "audio/mpeg", Files.size(source), "source-checksum"));
        return new MusicFixture(owner, music);
    }

    private static Path createTestRoot() {
        try {
            return Files.createTempDirectory("musicserver-transcode-service-test-");
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

    private record MusicFixture(AppUser owner, MusicFile music) {
    }
}
