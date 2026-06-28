package com.chmusic.musicserver.user;

import com.chmusic.musicserver.config.MusicServerProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvatarStorageService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path root;

    public AvatarStorageService(MusicServerProperties properties) {
        this.root = Path.of(properties.getStorage().getRoot()).toAbsolutePath().normalize().resolve("avatars");
    }

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
            Path userDir = root.resolve(String.valueOf(user.getId())).normalize();
            if (!userDir.startsWith(root)) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar path is invalid");
            }
            Files.createDirectories(userDir);
            String storedFilename = UUID.randomUUID() + "." + (extension.isBlank() ? "png" : extension);
            Path target = userDir.resolve(storedFilename).normalize();
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            return new StoredAvatar(storedFilename, file.getContentType(), Files.size(target));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store avatar", ex);
        }
    }

    public Resource load(Long userId, String filename) {
        Path path = root.resolve(String.valueOf(userId)).resolve(filename).normalize();
        if (!path.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar path is invalid");
        }
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found");
        }
        return resource;
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
}
