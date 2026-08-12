package com.chmusic.musicserver.user;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvatarStorageService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    public StoredAvatar store(AppUser user, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file must be smaller than 5MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!isAllowedImage(file.getContentType(), extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are supported");
        }

        try {
            String filename = sanitizeFilename(file.getOriginalFilename(), extension);
            String contentType = file.getContentType() == null ? defaultContentType(extension) : file.getContentType();
            return new StoredAvatar(filename, contentType, file.getSize(), file.getBytes());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read avatar", ex);
        }
    }

    private static boolean isAllowedImage(String contentType, String extension) {
        return (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/"))
                || ALLOWED_EXTENSIONS.contains(extension);
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeFilename(String filename, String extension) {
        String fallback = "avatar." + (extension.isBlank() ? "png" : extension);
        if (filename == null || filename.isBlank()) {
            return fallback;
        }
        String sanitized = filename.replaceAll("[\\\\/\\r\\n]", "").trim();
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private static String defaultContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "image/png";
        };
    }
}
