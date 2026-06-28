package com.chmusic.musicserver.playlist;

import com.chmusic.musicserver.api.dto.PlaylistRequest;
import com.chmusic.musicserver.api.dto.PlaylistResponse;
import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.music.MusicService;
import com.chmusic.musicserver.user.AppUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository trackRepository;
    private final MusicService musicService;

    public PlaylistService(PlaylistRepository playlistRepository, PlaylistTrackRepository trackRepository,
            MusicService musicService) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.musicService = musicService;
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> list(AppUser owner) {
        return playlistRepository.findWithTracksByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(PlaylistResponse::details)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistResponse details(AppUser owner, Long playlistId) {
        return PlaylistResponse.details(requireOwnedPlaylistWithTracks(owner, playlistId));
    }

    @Transactional
    public PlaylistResponse create(AppUser owner, PlaylistRequest request) {
        Playlist playlist = playlistRepository.save(new Playlist(owner, request.name().trim(), blankToNull(request.description())));
        return PlaylistResponse.summary(playlist);
    }

    @Transactional
    public PlaylistResponse update(AppUser owner, Long playlistId, PlaylistRequest request) {
        Playlist playlist = requireOwnedPlaylist(owner, playlistId);
        playlist.rename(request.name().trim(), blankToNull(request.description()));
        return PlaylistResponse.summary(playlist);
    }

    @Transactional
    public void delete(AppUser owner, Long playlistId) {
        playlistRepository.delete(requireOwnedPlaylist(owner, playlistId));
    }

    @Transactional
    public PlaylistResponse addTrack(AppUser owner, Long playlistId, PlaylistRequestPayload payload) {
        Playlist playlist = requireOwnedPlaylist(owner, playlistId);
        if (payload.musicId() != null) {
            MusicFile music = musicService.requireOwnedMusic(owner, payload.musicId());
            if (!trackRepository.existsByPlaylistAndMusic(playlist, music)) {
                playlist.getTracks().add(new PlaylistTrack(playlist, music, playlist.getTracks().size()));
                playlistRepository.save(playlist);
            }
        } else {
            String source = blankToDefault(payload.source(), "netease");
            String externalId = blankToNull(payload.externalId());
            if (externalId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "External song id is required");
            }
            if (!trackRepository.existsByPlaylistAndExternalSourceAndExternalId(playlist, source, externalId)) {
                playlist.getTracks().add(new PlaylistTrack(playlist, source, externalId,
                        blankToDefault(payload.title(), "未知歌曲"),
                        blankToNull(payload.artist()),
                        blankToNull(payload.album()),
                        blankToNull(payload.picUrl()),
                        payload.duration(),
                        playlist.getTracks().size()));
                playlistRepository.save(playlist);
            }
        }
        return PlaylistResponse.details(requireOwnedPlaylistWithTracks(owner, playlistId));
    }

    @Transactional
    public PlaylistResponse removeTrack(AppUser owner, Long playlistId, Long trackId) {
        Playlist playlist = requireOwnedPlaylist(owner, playlistId);
        trackRepository.findByIdAndPlaylist(trackId, playlist).ifPresentOrElse(trackRepository::delete,
                () -> trackRepository.findByPlaylistAndMusicId(playlist, trackId).ifPresent(trackRepository::delete));
        return PlaylistResponse.details(requireOwnedPlaylistWithTracks(owner, playlistId));
    }

    private Playlist requireOwnedPlaylist(AppUser owner, Long playlistId) {
        return playlistRepository.findByIdAndOwner(playlistId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
    }

    private Playlist requireOwnedPlaylistWithTracks(AppUser owner, Long playlistId) {
        return playlistRepository.findWithTracksByIdAndOwner(playlistId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record PlaylistRequestPayload(Long musicId, String source, String externalId, String title, String artist,
            String album, String picUrl, Long duration) {
    }
}
