package com.caddieapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.caddieapp.TestUsers;
import com.caddieapp.caddie.User;
import com.caddieapp.common.Enums.Role;

import io.jsonwebtoken.security.WeakKeyException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-definitely-longer-than-32-bytes";
    private final JwtService jwtService = new JwtService(SECRET, 90, 12);

    @Test
    void tokenRoundTripKeepsIdRoleFlagAndVersion() {
        User user = TestUsers.user(42, Role.ADMIN, "hash", false);
        user.changePassword("hash2"); // version 1

        String token = jwtService.createToken(user);

        assertThat(jwtService.parse(token)).contains(new AuthUser(42, Role.ADMIN, false, 1));
    }

    @Test
    void tokenRemembersThatPasswordMustBeChanged() {
        String token = jwtService.createToken(TestUsers.user(7, Role.CADDIE, "hash", true));

        assertThat(jwtService.parse(token)).hasValueSatisfying(u -> assertThat(u.mustChangePassword()).isTrue());
    }

    @Test
    void adminTokensLast12HoursAndCaddieTokens90Days() {
        long admin = lifetimeSeconds(jwtService.createToken(TestUsers.user(1, Role.ADMIN, "hash", false)));
        long caddie = lifetimeSeconds(jwtService.createToken(TestUsers.user(2, Role.CADDIE, "hash", false)));

        assertThat(admin).isEqualTo(12 * 3600);
        assertThat(caddie).isEqualTo(90L * 24 * 3600);
    }

    /** exp - iat, read straight from the token's JSON payload. */
    private static long lifetimeSeconds(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        long iat = Long.parseLong(payload.replaceAll(".*\"iat\":(\\d+).*", "$1"));
        long exp = Long.parseLong(payload.replaceAll(".*\"exp\":(\\d+).*", "$1"));
        return exp - iat;
    }

    @Test
    void editedTokenIsRejected() {
        String token = jwtService.createToken(TestUsers.user(42, Role.CADDIE, "hash", false));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-also-over-32-bytes", 90, 12);
        String token = other.createToken(TestUsers.user(42, Role.ADMIN, "hash", false));

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiresImmediately = new JwtService(SECRET, -1, -1);
        String token = expiresImmediately.createToken(TestUsers.user(42, Role.ADMIN, "hash", false));

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
        assertThat(jwtService.parse("")).isEmpty();
    }

    @Test
    void shortSecretFailsAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short", 90, 12)).isInstanceOf(WeakKeyException.class);
    }
}
