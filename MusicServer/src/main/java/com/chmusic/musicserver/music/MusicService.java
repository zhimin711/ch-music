package com.chmusic.musicserver.music;

import com.chmusic.musicserver.api.dto.MusicResponse;
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

    public MusicService(MusicFileRepository musicRepository, MusicStorageService storageService) {
        this.musicRepository = musicRepository;
        this.storageService = storageService;
    }

    @Transactional
    public MusicResponse upload(AppUser owner, MultipartFile file, String title, String artist, String album) {
        StoredMusicFile stored = storageService.store(file);
        String resolvedTitle = title == null || title.isBlank() ? stripExtension(stored.originalFilename()) : title.trim();
        MusicFile music = new MusicFile(owner, stored.originalFilename(), stored.storagePath(), resolvedTitle,
                blankToNull(artist), blankToNull(album), stored.contentType(), stored.fileSize(), stored.checksum());
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
        musicRepository.delete(music);
        storageService.delete(music);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
