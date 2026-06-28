package com.chmusic.musicserver.favorite;

import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.user.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteTrackRepository extends JpaRepository<FavoriteTrack, Long> {
    @EntityGraph(attributePaths = "music")
    List<FavoriteTrack> findByOwnerOrderByCreatedAtDesc(AppUser owner);

    boolean existsByOwnerAndMusic(AppUser owner, MusicFile music);

    Optional<FavoriteTrack> findByOwnerAndMusicId(AppUser owner, Long musicId);
}
