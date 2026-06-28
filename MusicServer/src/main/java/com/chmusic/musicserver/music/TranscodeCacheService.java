package com.chmusic.musicserver.music;

import com.chmusic.musicserver.config.MusicServerProperties;
import com.chmusic.musicserver.user.AppUser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TranscodeCacheService {
    private final TranscodeVariantRepository variantRepository;
    private final Path cacheRoot;
    private final Path tempRoot;
    private final long maxCacheBytes;

    public TranscodeCacheService(TranscodeVariantRepository variantRepository, MusicServerProperties properties) {
        this.variantRepository = variantRepository;
        MusicServerProperties.Transcoding transcoding = properties.getTranscoding();
        this.cacheRoot = Path.of(transcoding.getCacheRoot()).toAbsolutePath().normalize();
        this.tempRoot = Path.of(transcoding.getTempRoot()).toAbsolutePath().normalize();
        this.maxCacheBytes = transcoding.getCacheMaxSize() == null ? Long.MAX_VALUE
                : transcoding.getCacheMaxSize().toBytes();
    }

    public Path cachePathFor(MusicFile music, TranscodeProfileCatalog.ProfileDescriptor profile) {
        String extension = sanitizeExtension(profile.extension());
        Path path = cacheRoot
                .resolve(safeSegment(music.getOwner().getId()))
                .resolve(safeSegment(music.getId()))
                .resolve(safeSegment(profile.id()) + "." + extension)
                .normalize();
        return requireInsideCache(path);
    }

    public Path tempPathFor(MusicFile music, TranscodeProfileCatalog.ProfileDescriptor profile) {
        String extension = sanitizeExtension(profile.extension());
        Path path = tempRoot
                .resolve(safeSegment(music.getOwner().getId()))
                .resolve(safeSegment(music.getId()))
                .resolve(safeSegment(profile.id()) + "-" + UUID.randomUUID() + "." + extension + ".part")
                .normalize();
        return requireInsideTemp(path);
    }

    public Path pathOf(TranscodeVariant variant) {
        if (!StringUtils.hasText(variant.getStoragePath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcoded file is not ready");
        }
        return requireInsideCache(Path.of(variant.getStoragePath()).toAbsolutePath().normalize());
    }

    public Resource resourceOf(TranscodeVariant variant) {
        Path path = pathOf(variant);
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcoded file is missing");
        }
        return resource;
    }

    @Transactional
    public void markReady(TranscodeVariant variant, Path cachePath) {
        Path normalizedPath = requireInsideCache(cachePath.toAbsolutePath().normalize());
        try {
            long fileSize = Files.size(normalizedPath);
            variant.markReady(normalizedPath.toString(), fileSize, checksum(normalizedPath));
            variantRepository.save(variant);
            pruneToConfiguredSize();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to inspect transcode cache", ex);
        }
    }

    @Transactional
    public boolean validateReady(TranscodeVariant variant) {
        if (variant.getStatus() != TranscodeVariant.Status.READY) {
            return false;
        }
        try {
            Path path = pathOf(variant);
            if (!Files.isRegularFile(path)) {
                variant.markStale("CACHE_FILE_MISSING");
                variantRepository.save(variant);
                return false;
            }
            if (variant.getFileSize() != null && Files.size(path) != variant.getFileSize()) {
                variant.markStale("CACHE_SIZE_MISMATCH");
                variantRepository.save(variant);
                return false;
            }
            if (StringUtils.hasText(variant.getChecksum()) && !variant.getChecksum().equals(checksum(path))) {
                variant.markStale("CACHE_CHECKSUM_MISMATCH");
                variantRepository.save(variant);
                return false;
            }
            variant.touchAccessedAt();
            variantRepository.save(variant);
            return true;
        } catch (IOException | ResponseStatusException ex) {
            variant.markStale("CACHE_VALIDATION_FAILED");
            variantRepository.save(variant);
            return false;
        }
    }

    @Transactional
    public void deleteForMusic(AppUser owner, MusicFile music) {
        List<TranscodeVariant> variants = variantRepository.findByOwnerAndMusicId(owner, music.getId());
        variants.forEach(this::deleteCachedFile);
        variantRepository.deleteAll(variants);
    }

    @Transactional
    public void deleteVariant(TranscodeVariant variant) {
        deleteCachedFile(variant);
        variantRepository.delete(variant);
    }

    @Transactional
    public void pruneToConfiguredSize() {
        if (maxCacheBytes < 0 || !Files.exists(cacheRoot)) {
            return;
        }
        long currentSize = totalCacheBytes();
        if (currentSize <= maxCacheBytes) {
            return;
        }

        List<TranscodeVariant> readyVariants =
                variantRepository.findByStatusOrderByLastAccessedAtAsc(TranscodeVariant.Status.READY);
        for (TranscodeVariant variant : readyVariants) {
            if (currentSize <= maxCacheBytes) {
                return;
            }
            long variantSize = variant.getFileSize() == null ? fileSizeOrZero(variant) : variant.getFileSize();
            deleteCachedFile(variant);
            variantRepository.delete(variant);
            currentSize -= variantSize;
        }
    }

    private void deleteCachedFile(TranscodeVariant variant) {
        if (!StringUtils.hasText(variant.getStoragePath())) {
            return;
        }
        Path path = pathOf(variant);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete transcode cache", ex);
        }
    }

    private long fileSizeOrZero(TranscodeVariant variant) {
        try {
            return Files.size(pathOf(variant));
        } catch (IOException | ResponseStatusException ex) {
            return 0;
        }
    }

    private long totalCacheBytes() {
        try (Stream<Path> files = Files.walk(cacheRoot)) {
            return files
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException ex) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to inspect transcode cache", ex);
        }
    }

    private static String checksum(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return toHex(digest.digest());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to checksum transcode cache", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private Path requireInsideCache(Path path) {
        if (!path.startsWith(cacheRoot)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transcode cache path is invalid");
        }
        return path;
    }

    private Path requireInsideTemp(Path path) {
        if (!path.startsWith(tempRoot)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transcode temp path is invalid");
        }
        return path;
    }

    private static String safeSegment(Object value) {
        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) {
            return "unknown";
        }
        return text.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String sanitizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return "bin";
        }
        String cleaned = extension.trim().replaceFirst("^\\.+", "");
        return safeSegment(cleaned.isBlank() ? "bin" : cleaned);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

}
