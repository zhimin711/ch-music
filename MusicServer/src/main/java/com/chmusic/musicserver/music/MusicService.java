package com.chmusic.musicserver.music;

import com.chmusic.musicserver.api.dto.MusicResponse;
import com.chmusic.musicserver.config.MusicServerProperties;
import com.chmusic.musicserver.user.AppUser;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MusicService {
    private final MusicFileRepository musicRepository;
    private final MusicStorageService storageService;
    private final TranscodeCacheService transcodeCacheService;
    private final MusicServerProperties properties;

    public MusicService(MusicFileRepository musicRepository, MusicStorageService storageService,
            TranscodeCacheService transcodeCacheService, MusicServerProperties properties) {
        this.musicRepository = musicRepository;
        this.storageService = storageService;
        this.transcodeCacheService = transcodeCacheService;
        this.properties = properties;
    }

    @Transactional
    public MusicResponse upload(AppUser owner, MultipartFile file, String title, String artist, String album) {
        validateUploadQuota(owner, file.getSize());
        StoredMusicFile stored = storageService.store(file);
        ResolvedMetadata metadata = resolveMetadata(stored.originalFilename(), title, artist, album);
        MusicFile music = new MusicFile(owner, stored.originalFilename(), stored.storagePath(), metadata.title(),
                metadata.artist(), metadata.album(), stored.contentType(), stored.fileSize(), stored.checksum());
        return MusicResponse.from(musicRepository.save(music));
    }

    @Transactional(readOnly = true)
    public List<MusicResponse> list(AppUser owner) {
        return musicRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(MusicResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MusicFile requireOwnedMusic(AppUser owner, Long musicId) {
        return musicRepository.findByIdAndOwner(musicId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Music file not found"));
    }

    @Transactional(readOnly = true)
    public Resource stream(AppUser owner, Long musicId) {
        MusicFile music = requireOwnedMusic(owner, musicId);
        Path path = storageService.pathOf(music);
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored music file is missing");
        }
        return resource;
    }

    @Transactional
    public void delete(AppUser owner, Long musicId) {
        MusicFile music = requireOwnedMusic(owner, musicId);
        transcodeCacheService.deleteForMusic(owner, music);
        musicRepository.delete(music);
        storageService.delete(music);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateUploadQuota(AppUser owner, long incomingSize) {
        long maxTotalSize = properties.getUpload().getMaxTotalSize().toBytes();
        if (maxTotalSize <= 0) {
            return;
        }
        long usedSize = musicRepository.sumFileSizeByOwner(owner);
        if (usedSize >= maxTotalSize || incomingSize > maxTotalSize - usedSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Music upload quota exceeded");
        }
    }

    private static ResolvedMetadata resolveMetadata(String filename, String title, String artist, String album) {
        String resolvedTitle = blankToNull(title);
        String resolvedArtist = blankToNull(artist);
        String resolvedAlbum = blankToNull(album);
        if (resolvedTitle != null) {
            return new ResolvedMetadata(resolvedTitle, resolvedArtist, resolvedAlbum);
        }

        FilenameMetadata filenameMetadata = parseFilename(stripExtension(filename));
        if (resolvedArtist == null) {
            resolvedArtist = filenameMetadata.artist();
        }
        return new ResolvedMetadata(filenameMetadata.title(), resolvedArtist, resolvedAlbum);
    }

    private static FilenameMetadata parseFilename(String filenameWithoutExtension) {
        String fallbackTitle = filenameWithoutExtension == null || filenameWithoutExtension.isBlank()
                ? "Untitled"
                : filenameWithoutExtension.trim();
        int separatorIndex = fallbackTitle.indexOf(" - ");
        if (separatorIndex <= 0 || separatorIndex >= fallbackTitle.length() - 3) {
            return new FilenameMetadata(fallbackTitle, null);
        }
        String artist = fallbackTitle.substring(0, separatorIndex).trim();
        String title = fallbackTitle.substring(separatorIndex + 3).trim();
        if (artist.isBlank() || title.isBlank()) {
            return new FilenameMetadata(fallbackTitle, null);
        }
        return new FilenameMetadata(title, artist);
    }

    private static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    private record ResolvedMetadata(String title, String artist, String album) {
    }

    private record FilenameMetadata(String title, String artist) {
    }
}
