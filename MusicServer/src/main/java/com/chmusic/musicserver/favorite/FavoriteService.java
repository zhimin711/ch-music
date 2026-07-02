package com.chmusic.musicserver.favorite;

import com.chmusic.musicserver.api.dto.FavoriteResponse;
import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.music.MusicService;
import com.chmusic.musicserver.user.AppUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FavoriteService {
    private final FavoriteTrackRepository favoriteRepository;
    private final MusicService musicService;

    public FavoriteService(FavoriteTrackRepository favoriteRepository, MusicService musicService) {
        this.favoriteRepository = favoriteRepository;
        this.musicService = musicService;
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> list(AppUser owner) {
        return favoriteRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    @Transactional
    public List<FavoriteResponse> add(AppUser owner, Long musicId) {
        MusicFile music = musicService.requireOwnedMusic(owner, musicId);
        if (!favoriteRepository.existsByOwnerAndMusic(owner, music)) {
            favoriteRepository.save(new FavoriteTrack(owner, music));
        }
        return list(owner);
    }

    @Transactional
    public List<FavoriteResponse> addExternal(AppUser owner, FavoriteRequestPayload payload) {
        if (payload.musicId() != null) {
            return add(owner, payload.musicId());
        }

        String source = blankToDefault(payload.source(), "netease");
        String externalId = blankToNull(payload.externalId());
        if (externalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "External song id is required");
        }

        if (!favoriteRepository.existsByOwnerAndExternalSourceAndExternalId(owner, source, externalId)) {
            favoriteRepository.save(new FavoriteTrack(owner, source, externalId,
                    blankToDefault(payload.title(), "未知歌曲"),
                    blankToNull(payload.artist()),
                    blankToNull(payload.album()),
                    blankToNull(payload.picUrl()),
                    payload.duration()));
        }
        return list(owner);
    }

    @Transactional
    public List<FavoriteResponse> remove(AppUser owner, Long musicId) {
        favoriteRepository.findByOwnerAndMusicId(owner, musicId).ifPresent(favoriteRepository::delete);
        return list(owner);
    }

    @Transactional
    public List<FavoriteResponse> removeExternal(AppUser owner, String source, String externalId) {
        String normalizedSource = blankToDefault(source, "netease");
        String normalizedExternalId = blankToNull(externalId);
        if (normalizedExternalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "External song id is required");
        }
        favoriteRepository.findByOwnerAndExternalSourceAndExternalId(owner, normalizedSource, normalizedExternalId)
                .ifPresent(favoriteRepository::delete);
        return list(owner);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record FavoriteRequestPayload(Long musicId, String source, String externalId, String title, String artist,
            String album, String picUrl, Long duration) {
    }
}
