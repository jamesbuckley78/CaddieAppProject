package com.caddieapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.caddieapp.TestUsers;
import com.caddieapp.auth.AuthDtos.LoginResponse;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.caddie.User;
import com.caddieapp.caddie.UserRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Role;

/** Repositories are mocked (Mockito), so no database is needed. */
class AuthServiceTest {

    private static final ZoneId DUBLIN = ZoneId.of("Europe/Dublin");
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), DUBLIN);

    private final UserRepository users = mock(UserRepository.class);
    private final CaddieRepository caddies = mock(CaddieRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost = fast tests
    private final JwtService jwtService = new JwtService("test-secret-that-is-definitely-longer-than-32-bytes", 90, 12);
    private final LoginAttempts attempts = new LoginAttempts(5, 15, clock);
    private final AuthService authService = new AuthService(users, caddies, encoder, jwtService, attempts, clock);

    private User savedUser(String password, boolean mustChange) {
        User user = TestUsers.user(1, Role.CADDIE, encoder.encode(password), mustChange);
        when(users.findByPhone("0871234567")).thenReturn(Optional.of(user));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(caddies.findByUserId(1L)).thenReturn(Optional.empty());
        return user;
    }

    @Test
    void correctPasswordLogsIn() {
        savedUser("secret1", false);

        LoginResponse response = authService.login("0871234567", "secret1");

        assertThat(response.user().id()).isEqualTo(1);
        assertThat(jwtService.parse(response.token())).contains(new AuthUser(1, Role.CADDIE, false, 0));
    }

    @Test
    void phoneWithSpacesStillMatches() {
        savedUser("secret1", false);

        assertThat(authService.login("087 123 4567", "secret1").user().id()).isEqualTo(1);
    }

    @Test
    void wrongPasswordAndUnknownPhoneGiveTheSameError() {
        savedUser("secret1", false);
        when(users.findByPhone("0860000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("0871234567", "wrong"))
            .isInstanceOf(ApiException.class).hasMessage(AuthService.BAD_LOGIN);
        assertThatThrownBy(() -> authService.login("0860000000", "secret1"))
            .isInstanceOf(ApiException.class).hasMessage(AuthService.BAD_LOGIN);
    }

    @Test
    void fiveWrongTriesLocksThePhoneNumberEvenForTheRightPassword() {
        savedUser("secret1", false);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login("0871234567", "guess")).isInstanceOf(ApiException.class);
        }

        assertThatThrownBy(() -> authService.login("0871234567", "secret1"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Too many wrong attempts")
            .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void setupCodeLoginIsFlaggedAsMustChangePassword() {
        savedUser("123456", true);

        LoginResponse response = authService.login("0871234567", "123456");

        assertThat(response.user().mustChangePassword()).isTrue();
        assertThat(jwtService.parse(response.token()).orElseThrow().mustChangePassword()).isTrue();
    }

    @Test
    void expiredSetupCodeIsRefused() {
        User user = savedUser("ignored", false);
        user.resetToSetupCode(encoder.encode("123456"), LocalDateTime.now(clock).minusMinutes(1));

        assertThatThrownBy(() -> authService.login("0871234567", "123456"))
            .isInstanceOf(ApiException.class).hasMessage(AuthService.CODE_EXPIRED);
    }

    @Test
    void settingPasswordClearsTheFlagAndOldCodeStopsWorking() {
        User user = savedUser("123456", true);

        LoginResponse response = authService.setPassword(1, "my-new-password");

        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(jwtService.parse(response.token()).orElseThrow().mustChangePassword()).isFalse();
        assertThat(authService.login("0871234567", "my-new-password").user().id()).isEqualTo(1);
        assertThatThrownBy(() -> authService.login("0871234567", "123456")).isInstanceOf(ApiException.class);
    }

    @Test
    void changingPasswordBumpsTheTokenVersionSoOldTokensStopWorking() {
        User user = savedUser("123456", true);
        int before = user.getTokenVersion();

        LoginResponse response = authService.setPassword(1, "my-new-password");

        assertThat(user.getTokenVersion()).isEqualTo(before + 1);
        assertThat(jwtService.parse(response.token()).orElseThrow().tokenVersion()).isEqualTo(before + 1);
    }

    @Test
    void newPasswordCannotBeTheSetupCode() {
        savedUser("123456", true);

        assertThatThrownBy(() -> authService.setPassword(1, "123456")).isInstanceOf(ApiException.class);
    }
}
