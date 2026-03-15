package com.philxin.interviewos.security;

import com.philxin.interviewos.config.JwtProperties;
import com.philxin.interviewos.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * 统一负责 JWT 生成与解析。
 */
@Service
public class JwtTokenService {
    public static final String TOKEN_TYPE = "Bearer";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = buildSecretKey(jwtProperties.getSecret());
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getExpiresIn());
        return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .issuer(jwtProperties.getIssuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claim("email", user.getEmail())
            .claim("displayName", user.getDisplayName())
            .claim("targetRole", user.getTargetRole() == null ? null : user.getTargetRole().name())
            .signWith(secretKey)
            .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public long getExpiresInSeconds() {
        return jwtProperties.getExpiresIn().toSeconds();
    }

    private SecretKey buildSecretKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        byte[] secretBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }
}
