package com.chmusic.musicserver.auth;

import com.chmusic.musicserver.api.dto.AuthRequest;
import com.chmusic.musicserver.api.dto.AuthResponse;
import com.chmusic.musicserver.api.dto.UserResponse;
import com.chmusic.musicserver.user.AppUser;
import com.chmusic.musicserver.user.AppUserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        String displayName = blankToDefault(request.displayName(), username);
        AppUser user = userRepository.save(new AppUser(username, passwordEncoder.encode(request.password()), displayName));
        TokenService.IssuedToken token = tokenService.issue(user);
        return new AuthResponse("Bearer", token.token(), token.expiresAt(), UserResponse.from(user));
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String username = normalizeUsername(request.username());
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        TokenService.IssuedToken token = tokenService.issue(user);
        return new AuthResponse("Bearer", token.token(), token.expiresAt(), UserResponse.from(user));
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
