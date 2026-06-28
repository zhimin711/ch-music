package com.chmusic.musicserver.playlist;

import com.chmusic.musicserver.music.MusicFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {
    boolean existsByPlaylistAndMusic(Playlist playlist, MusicFile music);

    boolean existsByPlaylistAndExternalSourceAndExternalId(Playlist playlist, String externalSource, String externalId);

    Optional<PlaylistTrack> findByPlaylistAndMusicId(Playlist playlist, Long musicId);

    Optional<PlaylistTrack> findByIdAndPlaylist(Long id, Playlist playlist);
}
