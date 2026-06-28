package com.chmusic.musicserver.auth;

import com.chmusic.musicserver.config.MusicServerProperties;
import com.chmusic.musicserver.user.AppUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
    private final AuthTokenRepository tokenRepository;
    private final MusicServerProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(AuthTokenRepository tokenRepository, MusicServerProperties properties) {
        this.tokenRepository = tokenRepository;
        this.properties = properties;
    }

    @Transactional
    public IssuedToken issue(AppUser user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(properties.getAuth().getTokenTtlDays(), ChronoUnit.DAYS);
        tokenRepository.save(new AuthToken(hash(rawToken), user, expiresAt));
        return new IssuedToken(rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public AppUser authenticate(String rawToken) {
        return tokenRepository.findByTokenHash(hash(rawToken))
                .filter(AuthToken::isActive)
                .map(AuthToken::getUser)
                .orElse(null);
    }

    @Transactional
    public void revoke(String rawToken) {
        tokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.revoke();
            tokenRepository.save(token);
        });
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
