package com.chmusic.musicserver.favorite;

import com.chmusic.musicserver.api.dto.FavoriteResponse;
import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.music.MusicService;
import com.chmusic.musicserver.user.AppUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<FavoriteResponse> remove(AppUser owner, Long musicId) {
        favoriteRepository.findByOwnerAndMusicId(owner, musicId).ifPresent(favoriteRepository::delete);
        return list(owner);
    }
}
