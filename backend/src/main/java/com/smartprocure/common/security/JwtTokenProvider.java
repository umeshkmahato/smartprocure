package com.smartprocure.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JJWT 0.12.x — JWT access-token and refresh-token provider.
 *
 * <p>Access token: short-lived JWT (default 15 min) signed with HS256.
 * Claims: {@code sub}=username, {@code userId}, {@code email}, {@code roles}.
 *
 * <p>Refresh token: opaque 64-char UUID string stored in {@code refresh_tokens}.
 * Never embedded in the JWT — only used to rotate the access token.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry-seconds:900}")
    private long accessTokenExpirySeconds;

    // ── Generation ────────────────────────────────────────────────────────────

    public String generateAccessToken(UserPrincipal principal) {
        Date now        = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirySeconds * 1_000L);

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("userId", principal.getId())
                .claim("email",  principal.getEmail())
                .claim("roles",  roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey())
                .compact();
    }

    /** Opaque refresh token: two UUID v4s concatenated (64 hex chars). */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
             + UUID.randomUUID().toString().replace("-", "");
    }

    // ── Extraction ────────────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // ── Metadata ──────────────────────────────────────────────────────────────

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
