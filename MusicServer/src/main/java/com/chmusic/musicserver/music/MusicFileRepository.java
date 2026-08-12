package com.chmusic.musicserver.music;

import com.chmusic.musicserver.user.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MusicFileRepository extends JpaRepository<MusicFile, Long> {
    List<MusicFile> findByOwnerOrderByCreatedAtDesc(AppUser owner);

    Optional<MusicFile> findByIdAndOwner(Long id, AppUser owner);

    @Query("select coalesce(sum(m.fileSize), 0) from MusicFile m where m.owner = :owner")
    long sumFileSizeByOwner(@Param("owner") AppUser owner);
}
