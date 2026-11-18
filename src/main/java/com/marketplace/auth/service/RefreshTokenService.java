package com.marketplace.auth.service;

import com.marketplace.auth.entity.RefreshToken;
import com.marketplace.auth.exception.InvalidCredentialsException;
import com.marketplace.auth.repository.RefreshTokenRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.entity.UserStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository repository;

    @Value("${jwt.refresh-token-expiration:PT720H}")
    private Duration expiration;

    @Transactional(propagation = Propagation.MANDATORY)
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(expiration));
        repository.save(token);
        return raw;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public User consume(String raw) {
        RefreshToken token = repository.findByHashForUpdate(hash(raw))
                .orElseThrow(InvalidCredentialsException::new);
        if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(Instant.now())
                || token.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }
        token.setRevokedAt(Instant.now());
        return token.getUser();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revoke(String raw) {
        RefreshToken token = repository.findByHashForUpdate(hash(raw))
                .orElseThrow(InvalidCredentialsException::new);
        if (token.getRevokedAt() == null) token.setRevokedAt(Instant.now());
    }

    private String hash(String raw) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
