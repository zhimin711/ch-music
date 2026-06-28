package com.chmusic.musicserver.music;

import com.chmusic.musicserver.user.AppUser;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TranscodeService {
    private static final int RETRY_AFTER_SECONDS = 3;

    private final MusicService musicService;
    private final MusicStorageService storageService;
    private final TranscodeProfileCatalog profileCatalog;
    private final TranscodeVariantRepository variantRepository;
    private final TranscodeCacheService cacheService;
    private final TranscodeExecutor transcodeExecutor;
    private final ConcurrentHashMap<String, CompletableFuture<TranscodeExecutor.TranscodeResult>> jobs =
            new ConcurrentHashMap<>();

    public TranscodeService(MusicService musicService, MusicStorageService storageService,
            TranscodeProfileCatalog profileCatalog, TranscodeVariantRepository variantRepository,
            TranscodeCacheService cacheService, TranscodeExecutor transcodeExecutor) {
        this.musicService = musicService;
        this.storageService = storageService;
        this.profileCatalog = profileCatalog;
        this.variantRepository = variantRepository;
        this.cacheService = cacheService;
        this.transcodeExecutor = transcodeExecutor;
    }

    @Transactional
    public TranscodeStatus prepare(AppUser owner, Long musicId, String profileId) {
        MusicFile music = musicService.requireOwnedMusic(owner, musicId);
        TranscodeProfileCatalog.ProfileResolution resolution = profileCatalog.resolve(profileId);
        if (resolution.status() != TranscodeProfileCatalog.CatalogStatus.AVAILABLE) {
            return TranscodeStatus.unavailable(music, profileId, resolution.reason());
        }

        TranscodeProfileCatalog.ProfileDescriptor profile = resolution.profile().orElseThrow();
        TranscodeVariant variant = variantRepository.findByOwnerAndMusicAndProfileId(owner, music, profile.id())
                .orElseGet(() -> new TranscodeVariant(owner, music, profile.id()));
        if (variant.getStatus() == TranscodeVariant.Status.READY && cacheService.validateReady(variant)) {
            return TranscodeStatus.from(variant, streamUrl(music.getId(), profile.id()), null);
        }

        String jobKey = jobKey(owner, music, profile.id());
        variant.markQueued();
        TranscodeVariant saved = variantRepository.save(variant);
        scheduleIfAbsent(jobKey, saved.getId(), music, profile);
        return TranscodeStatus.from(saved, streamUrl(music.getId(), profile.id()), RETRY_AFTER_SECONDS);
    }

    @Transactional
    public TranscodeStatus status(AppUser owner, Long musicId, String profileId) {
        MusicFile music = musicService.requireOwnedMusic(owner, musicId);
        Optional<TranscodeVariant> variant =
                variantRepository.findByOwnerAndMusicIdAndProfileId(owner, musicId, profileId);
        if (variant.isEmpty()) {
            return TranscodeStatus.notPrepared(music, profileId);
        }
        TranscodeVariant current = variant.get();
        if (current.getStatus() == TranscodeVariant.Status.READY) {
            cacheService.validateReady(current);
        }
        Integer retryAfter = isRunning(owner, music, profileId) ? RETRY_AFTER_SECONDS : null;
        return TranscodeStatus.from(current, streamUrl(musicId, profileId), retryAfter);
    }

    @Transactional
    public Path requireReadyVariant(AppUser owner, Long musicId, String profileId) {
        MusicFile music = musicService.requireOwnedMusic(owner, musicId);
        TranscodeVariant variant = variantRepository.findByOwnerAndMusicAndProfileId(owner, music, profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Transcode is not prepared"));
        if (variant.getStatus() != TranscodeVariant.Status.READY || !cacheService.validateReady(variant)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transcode is not ready");
        }
        return cacheService.pathOf(variant);
    }

    private void scheduleIfAbsent(String jobKey, Long variantId, MusicFile music,
            TranscodeProfileCatalog.ProfileDescriptor profile) {
        jobs.computeIfAbsent(jobKey, ignored -> {
            Path sourcePath = storageService.pathOf(music);
            Path tempPath = cacheService.tempPathFor(music, profile);
            Path cachePath = cacheService.cachePathFor(music, profile);
            markProcessing(variantId);
            CompletableFuture<TranscodeExecutor.TranscodeResult> future =
                    transcodeExecutor.submit(sourcePath, profile, tempPath);
            future.whenComplete((result, throwable) -> {
                try {
                    completeJob(variantId, cachePath, tempPath, result, throwable);
                } finally {
                    jobs.remove(jobKey);
                    deleteTempQuietly(tempPath);
                }
            });
            return future;
        });
    }

    private void markProcessing(Long variantId) {
        variantRepository.findById(variantId).ifPresent(variant -> {
            variant.markProcessing();
            variantRepository.save(variant);
        });
    }

    private void completeJob(Long variantId, Path cachePath, Path tempPath,
            TranscodeExecutor.TranscodeResult result, Throwable throwable) {
        variantRepository.findById(variantId).ifPresent(variant -> {
            if (throwable != null) {
                variant.markFailed("TRANSCODE_EXECUTION_FAILED");
                variantRepository.save(variant);
                return;
            }
            if (result == null || !result.ready()) {
                variant.markFailed(result == null ? "TRANSCODE_FAILED" : result.errorCode());
                variantRepository.save(variant);
                return;
            }
            try {
                Files.createDirectories(cachePath.getParent());
                moveIntoCache(result.outputPath(), cachePath);
                cacheService.markReady(variant, cachePath);
            } catch (IOException | ResponseStatusException ex) {
                variant.markFailed("TRANSCODE_CACHE_WRITE_FAILED");
                variantRepository.save(variant);
            }
        });
        deleteTempQuietly(tempPath);
    }

    private static void moveIntoCache(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isRunning(AppUser owner, MusicFile music, String profileId) {
        return jobs.containsKey(jobKey(owner, music, profileId));
    }

    private static String jobKey(AppUser owner, MusicFile music, String profileId) {
        return owner.getId() + ":" + music.getId() + ":" + profileId;
    }

    private static String streamUrl(Long musicId, String profileId) {
        return "/api/music/" + musicId + "/stream?profile=" + profileId;
    }

    private static void deleteTempQuietly(Path tempPath) {
        try {
            Files.deleteIfExists(tempPath);
        } catch (IOException ignored) {
        }
    }

    public record TranscodeStatus(Long musicId, String profileId, TranscodeVariant.Status state, String reason,
            String streamUrl, Integer retryAfterSeconds, Long fileSize, String checksum) {
        private static TranscodeStatus from(TranscodeVariant variant, String streamUrl, Integer retryAfterSeconds) {
            return new TranscodeStatus(variant.getMusic().getId(), variant.getProfileId(), variant.getStatus(),
                    variant.getErrorMessage(), streamUrl, retryAfterSeconds, variant.getFileSize(),
                    variant.getChecksum());
        }

        private static TranscodeStatus unavailable(MusicFile music, String profileId, String reason) {
            return new TranscodeStatus(music.getId(), profileId, TranscodeVariant.Status.FAILED, reason,
                    TranscodeService.streamUrl(music.getId(), profileId), null, null, null);
        }

        private static TranscodeStatus notPrepared(MusicFile music, String profileId) {
            return new TranscodeStatus(music.getId(), profileId, TranscodeVariant.Status.QUEUED, "NOT_PREPARED",
                    TranscodeService.streamUrl(music.getId(), profileId), null, null, null);
        }
    }
}
