package com.caddieapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.caddieapp.TestUsers;
import com.caddieapp.common.Enums.Role;

import io.jsonwebtoken.security.WeakKeyException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-definitely-longer-than-32-bytes";
    private final JwtService jwtService = new JwtService(SECRET, 90);

    @Test
    void tokenRoundTripKeepsIdRoleAndPasswordFlag() {
        String token = jwtService.createToken(TestUsers.user(42, Role.ADMIN, "hash", false));

        assertThat(jwtService.parse(token)).contains(new AuthUser(42, Role.ADMIN, false));
    }

    @Test
    void tokenRemembersThatPasswordMustBeChanged() {
        String token = jwtService.createToken(TestUsers.user(7, Role.CADDIE, "hash", true));

        assertThat(jwtService.parse(token)).hasValueSatisfying(u -> assertThat(u.mustChangePassword()).isTrue());
    }

    @Test
    void editedTokenIsRejected() {
        String token = jwtService.createToken(TestUsers.user(42, Role.CADDIE, "hash", false));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-also-over-32-bytes", 90);
        String token = other.createToken(TestUsers.user(42, Role.ADMIN, "hash", false));

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiresImmediately = new JwtService(SECRET, -1);
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
        assertThatThrownBy(() -> new JwtService("too-short", 90)).isInstanceOf(WeakKeyException.class);
    }
}
