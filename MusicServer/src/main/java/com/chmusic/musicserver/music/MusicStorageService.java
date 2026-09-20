package com.chmusic.musicserver.music;

import com.chmusic.musicserver.config.MusicServerProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MusicStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "flac", "m4a", "aac", "wav", "ogg", "ape");
    private static final Set<String> ALLOWED_COVER_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final Path root;

    public MusicStorageService(MusicServerProperties properties) {
        this.root = Path.of(properties.getStorage().getRoot()).toAbsolutePath().normalize();
    }

    public StoredMusicFile store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Music file is empty");
        }
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        if (!isAllowedMusic(file.getContentType(), extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only common audio files are supported");
        }

        try {
            Files.createDirectories(root);
            String storedFilename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
            Path target = root.resolve(storedFilename).normalize();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(input, target);
            }
            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            return new StoredMusicFile(originalFilename, target.toString(), contentType, Files.size(target),
                    toHex(digest.digest()));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store music file", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public Path pathOf(MusicFile music) {
        Path path = Path.of(music.getStoragePath()).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored file path is invalid");
        }
        return path;
    }

    public Path pathOf(StoredMusicFile music) {
        Path path = Path.of(music.storagePath()).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored file path is invalid");
        }
        return path;
    }

    public StoredCover storeCover(StoredMusicFile music, EmbeddedCover cover) {
        if (cover == null || cover.bytes() == null || cover.bytes().length == 0) {
            return null;
        }
        String contentType = cover.contentType() == null ? "image/jpeg" : cover.contentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_COVER_TYPES.contains(contentType)) {
            return null;
        }
        try {
            Path coverRoot = root.resolve("covers").normalize();
            Files.createDirectories(coverRoot);
            String extension = coverExtension(contentType);
            String musicStem = stripExtension(Path.of(music.storagePath()).getFileName().toString());
            Path target = coverRoot.resolve(musicStem + extension).normalize();
            if (!target.startsWith(coverRoot)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cover path is invalid");
            }
            Files.write(target, cover.bytes());
            return new StoredCover(target.toString(), contentType);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store music cover", ex);
        }
    }

    public Resource coverOf(MusicFile music) {
        if (music.getCoverPath() == null || music.getCoverPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Music cover not found");
        }
        Path path = Path.of(music.getCoverPath()).toAbsolutePath().normalize();
        if (!path.startsWith(root.resolve("covers").toAbsolutePath().normalize())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored cover path is invalid");
        }
        Resource resource = new org.springframework.core.io.FileSystemResource(path);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored music cover is missing");
        }
        return resource;
    }

    public void delete(MusicFile music) {
        try {
            Files.deleteIfExists(pathOf(music));
            if (music.getCoverPath() != null && !music.getCoverPath().isBlank()) {
                Path coverPath = Path.of(music.getCoverPath()).toAbsolutePath().normalize();
                Path coverRoot = root.resolve("covers").toAbsolutePath().normalize();
                if (coverPath.startsWith(coverRoot)) {
                    Files.deleteIfExists(coverPath);
                }
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete music file", ex);
        }
    }

    private static boolean isAllowedMusic(String contentType, String extension) {
        return (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("audio/"))
                || ALLOWED_EXTENSIONS.contains(extension);
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unknown-audio";
        }
        return Path.of(filename).getFileName().toString().replaceAll("[\\r\\n]", "").trim();
    }

    private static String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String coverExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
