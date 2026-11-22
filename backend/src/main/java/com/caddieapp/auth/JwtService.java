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
 * Creates and checks login tokens. A token holds the user id, their role and whether they
 * still have to set a password, signed with the server's secret so it can't be edited.
 */
@Service
public class JwtService {

    private static final String ROLE = "role";
    private static final String MUST_CHANGE_PASSWORD = "pwdChange";

    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiry-days}") long expiryDays) {
        // Throws WeakKeyException at startup if the secret is shorter than 32 bytes.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofDays(expiryDays);
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .claim(ROLE, user.getRole().name())
            .claim(MUST_CHANGE_PASSWORD, user.isMustChangePassword())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(lifetime)))
            .signWith(key)
            .compact();
    }

    /** Empty if the token is expired, edited, signed with another key or not a JWT at all. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(
                Long.parseLong(claims.getSubject()),
                Role.valueOf(claims.get(ROLE, String.class)),
                Boolean.TRUE.equals(claims.get(MUST_CHANGE_PASSWORD, Boolean.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
