package com.chmusic.musicserver.playlist;

import com.chmusic.musicserver.user.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByOwnerOrderByCreatedAtDesc(AppUser owner);

    @EntityGraph(attributePaths = { "tracks", "tracks.music" })
    @Query("select distinct p from Playlist p where p.owner = :owner order by p.createdAt desc")
    List<Playlist> findWithTracksByOwnerOrderByCreatedAtDesc(@Param("owner") AppUser owner);

    @EntityGraph(attributePaths = { "tracks", "tracks.music" })
    @Query("select p from Playlist p where p.id = :id and p.owner = :owner")
    Optional<Playlist> findWithTracksByIdAndOwner(@Param("id") Long id, @Param("owner") AppUser owner);

    Optional<Playlist> findByIdAndOwner(Long id, AppUser owner);
}
