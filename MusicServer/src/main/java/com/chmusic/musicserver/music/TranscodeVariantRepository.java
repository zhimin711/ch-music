package com.chmusic.musicserver.music;

import com.chmusic.musicserver.user.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscodeVariantRepository extends JpaRepository<TranscodeVariant, Long> {
    @EntityGraph(attributePaths = "music")
    List<TranscodeVariant> findByOwnerAndMusicId(AppUser owner, Long musicId);

    Optional<TranscodeVariant> findByOwnerAndMusicAndProfileId(AppUser owner, MusicFile music, String profileId);

    Optional<TranscodeVariant> findByOwnerAndMusicIdAndProfileId(AppUser owner, Long musicId, String profileId);

    List<TranscodeVariant> findByOwnerAndStatusOrderByLastAccessedAtAsc(AppUser owner, TranscodeVariant.Status status);

    List<TranscodeVariant> findByStatusOrderByLastAccessedAtAsc(TranscodeVariant.Status status);

    boolean existsByOwnerAndMusicAndProfileId(AppUser owner, MusicFile music, String profileId);

    void deleteByOwnerAndMusic(AppUser owner, MusicFile music);
}
