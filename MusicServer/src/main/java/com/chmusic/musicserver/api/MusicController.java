package com.chmusic.musicserver.api;

import com.chmusic.musicserver.api.dto.MusicResponse;
import com.chmusic.musicserver.api.dto.TranscodeStatusResponse;
import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.music.MusicService;
import com.chmusic.musicserver.music.StreamingService;
import com.chmusic.musicserver.music.TranscodeProfileCatalog;
import com.chmusic.musicserver.music.TranscodeService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/music")
public class MusicController {
    private final MusicService musicService;
    private final StreamingService streamingService;
    private final TranscodeService transcodeService;
    private final TranscodeProfileCatalog profileCatalog;

    public MusicController(MusicService musicService, StreamingService streamingService,
            TranscodeService transcodeService, TranscodeProfileCatalog profileCatalog) {
        this.musicService = musicService;
        this.streamingService = streamingService;
        this.transcodeService = transcodeService;
        this.profileCatalog = profileCatalog;
    }

    @GetMapping
    public List<MusicResponse> list(Authentication authentication) {
        return musicService.list(CurrentUser.from(authentication));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MusicResponse upload(Authentication authentication, @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title, @RequestParam(required = false) String artist,
            @RequestParam(required = false) String album) {
        return musicService.upload(CurrentUser.from(authentication), file, title, artist, album);
    }

    @GetMapping("/{musicId}")
    public MusicResponse details(Authentication authentication, @PathVariable Long musicId) {
        MusicFile music = musicService.requireOwnedMusic(CurrentUser.from(authentication), musicId);
        return MusicResponse.from(music);
    }

    @GetMapping("/{musicId}/stream")
    public ResponseEntity<?> stream(Authentication authentication, @PathVariable Long musicId,
            @RequestParam(defaultValue = "original") String profile, @RequestHeader HttpHeaders headers) {
        return streamingService.streamVariant(CurrentUser.from(authentication), musicId, profile, headers);
    }

    @GetMapping("/transcode-capabilities")
    public TranscodeStatusResponse.CapabilitiesResponse transcodeCapabilities() {
        return TranscodeStatusResponse.CapabilitiesResponse.from(profileCatalog.capabilities());
    }

    @PostMapping("/{musicId}/transcodes/{profileId}")
    public ResponseEntity<TranscodeStatusResponse> prepareTranscode(Authentication authentication,
            @PathVariable Long musicId, @PathVariable String profileId) {
        TranscodeStatusResponse response = TranscodeStatusResponse.from(
                transcodeService.prepare(CurrentUser.from(authentication), musicId, profileId));
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{musicId}/transcodes/{profileId}")
    public TranscodeStatusResponse transcodeStatus(Authentication authentication, @PathVariable Long musicId,
            @PathVariable String profileId) {
        return TranscodeStatusResponse.from(transcodeService.status(CurrentUser.from(authentication), musicId, profileId));
    }

    @DeleteMapping("/{musicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long musicId) {
        musicService.delete(CurrentUser.from(authentication), musicId);
    }
}
