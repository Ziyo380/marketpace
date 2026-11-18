package com.marketplace.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private  final Duration accessTokenDuration;
    public JwtTokenProvider(
            @Value("${jwt.secret-base64}")
            String secretBase64,
            @Value("${jwt.access-token-expiration}")
            Duration accessTokenDuration) {
        this.accessTokenDuration=accessTokenDuration;
        byte[] keyBytes=Decoders.BASE64.decode(secretBase64);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenDuration);
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String subject= Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload().getSubject();
            if (subject == null ||  subject.isBlank()) {
                return false;
            }
            return true;

        }catch (JwtException  | IllegalArgumentException e) {
            return false;
        }
    }
}
