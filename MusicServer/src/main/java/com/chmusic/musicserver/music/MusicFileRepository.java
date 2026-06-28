package com.chmusic.musicserver.music;

import com.chmusic.musicserver.user.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicFileRepository extends JpaRepository<MusicFile, Long> {
    List<MusicFile> findByOwnerOrderByCreatedAtDesc(AppUser owner);

    Optional<MusicFile> findByIdAndOwner(Long id, AppUser owner);
}
