package com.chmusic.musicserver.api;

import com.chmusic.musicserver.api.dto.AuthRequest;
import com.chmusic.musicserver.api.dto.AuthResponse;
import com.chmusic.musicserver.api.dto.ProfileUpdateRequest;
import com.chmusic.musicserver.api.dto.UserResponse;
import com.chmusic.musicserver.auth.AuthService;
import com.chmusic.musicserver.auth.TokenService;
import com.chmusic.musicserver.user.AppUser;
import com.chmusic.musicserver.user.AvatarStorageService;
import com.chmusic.musicserver.user.StoredAvatar;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;
    private final AvatarStorageService avatarStorageService;

    public AuthController(AuthService authService, TokenService tokenService,
            AvatarStorageService avatarStorageService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.avatarStorageService = avatarStorageService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            tokenService.revoke(header.substring(7).trim());
        }
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(CurrentUser.from(authentication));
    }

    @PutMapping("/me")
    public UserResponse updateMe(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(CurrentUser.from(authentication), request);
    }

    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
        AppUser user = CurrentUser.from(authentication);
        StoredAvatar avatar = avatarStorageService.store(user, file);
        String avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/auth/avatars/{userId}/{filename}")
                .buildAndExpand(user.getId(), avatar.filename())
                .toUriString();
        return authService.updateAvatar(user, avatarUrl);
    }

    @GetMapping("/avatars/{userId}/{filename}")
    public ResponseEntity<Resource> avatar(@PathVariable Long userId, @PathVariable String filename) {
        Resource resource = avatarStorageService.load(userId, filename);
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(resource);
    }
}
