package com.chmusic.musicserver.api;

import com.chmusic.musicserver.api.dto.MusicResponse;
import com.chmusic.musicserver.music.MusicFile;
import com.chmusic.musicserver.music.MusicService;
import com.chmusic.musicserver.user.AppUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/music")
public class MusicController {
    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
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
    public ResponseEntity<Resource> stream(Authentication authentication, @PathVariable Long musicId) {
        AppUser owner = CurrentUser.from(authentication);
        MusicFile music = musicService.requireOwnedMusic(owner, musicId);
        Resource resource = musicService.stream(owner, musicId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(music.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(music.getOriginalFilename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(resource);
    }

    @DeleteMapping("/{musicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long musicId) {
        musicService.delete(CurrentUser.from(authentication), musicId);
    }
}
