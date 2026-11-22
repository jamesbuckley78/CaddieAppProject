package com.caddieapp.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.caddieapp.caddie.User;
import com.caddieapp.common.Enums.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Creates and checks login tokens. A token holds the user id, their role, whether they still
 * have to set a password and their token version, signed with the server's secret so it can't
 * be edited. Caddies stay logged in for 90 days; admins for 12 hours, because the admin board
 * may be on a shared PC in the caddie shack.
 */
@Service
public class JwtService {

    private static final String ROLE = "role";
    private static final String MUST_CHANGE_PASSWORD = "pwdChange";
    private static final String VERSION = "ver";

    private final SecretKey key;
    private final Duration caddieLifetime;
    private final Duration adminLifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiry-days}") long expiryDays,
                      @Value("${app.jwt.admin-expiry-hours}") long adminExpiryHours) {
        // Throws WeakKeyException at startup if the secret is shorter than 32 bytes.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.caddieLifetime = Duration.ofDays(expiryDays);
        this.adminLifetime = Duration.ofHours(adminExpiryHours);
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        Duration lifetime = user.getRole() == Role.ADMIN ? adminLifetime : caddieLifetime;
        return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .claim(ROLE, user.getRole().name())
            .claim(MUST_CHANGE_PASSWORD, user.isMustChangePassword())
            .claim(VERSION, user.getTokenVersion())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(lifetime)))
            .signWith(key)
            .compact();
    }

    /** Empty if the token is expired, edited, signed with another key or not a JWT at all. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Integer version = claims.get(VERSION, Integer.class);
            return Optional.of(new AuthUser(
                Long.parseLong(claims.getSubject()),
                Role.valueOf(claims.get(ROLE, String.class)),
                Boolean.TRUE.equals(claims.get(MUST_CHANGE_PASSWORD, Boolean.class)),
                version == null ? 0 : version));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
