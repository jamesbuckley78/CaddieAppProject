package com.caddieapp.auth;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.auth.AuthDtos.LoginResponse;
import com.caddieapp.auth.AuthDtos.UserResponse;
import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.caddie.User;
import com.caddieapp.caddie.UserRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.common.PhoneNumbers;

@Service
public class AuthService {

    /** Same message for unknown phone and wrong password, so nobody can probe which numbers exist. */
    static final String BAD_LOGIN = "Wrong phone number or password";

    static final String CODE_EXPIRED = "Your setup code has expired - ask the caddie master for a new one";

    private final UserRepository users;
    private final CaddieRepository caddies;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttempts attempts;
    private final Clock clock;
    /** Checked when the phone number has no account, so that answer takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(UserRepository users, CaddieRepository caddies, PasswordEncoder passwordEncoder,
                       JwtService jwtService, LoginAttempts attempts, Clock clock) {
        this.users = users;
        this.caddies = caddies;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.attempts = attempts;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("no-such-account");
    }

    /**
     * Phone + password (or the one-time setup code) -> token. Five wrong tries locks that phone
     * number for 15 minutes (LoginAttempts); setup codes stop working after 48 hours.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String phone, String password) {
        String normalised = PhoneNumbers.normalise(phone);
        attempts.checkAllowed(normalised);
        User user = users.findByPhone(normalised)
            .filter(u -> u.getPasswordHash() != null && u.isActive())
            .orElse(null);
        // Always run one password check: answering unknown numbers faster would show which numbers have accounts
        boolean matches = passwordEncoder.matches(password, user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !matches) {
            attempts.failed(normalised);
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_LOGIN);
        }
        LocalDateTime expires = user.getSetupCodeExpiresAt();
        if (user.isMustChangePassword() && expires != null && LocalDateTime.now(clock).isAfter(expires)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, CODE_EXPIRED);
        }
        attempts.succeeded(normalised);
        return new LoginResponse(jwtService.createToken(user), toResponse(user));
    }

    /**
     * The app registers where to send push notifications ("EXPO" token from expo-notifications).
     * The token belongs to the phone, so it's taken off anyone who used this phone before -
     * otherwise their calls would still buzz it.
     */
    @Transactional
    public void savePushToken(long userId, String provider, String token) {
        users.clearPushTokenFromOthers(token, userId);
        users.findById(userId).ifPresent(u -> u.setPushToken(provider, token));
    }

    /**
     * A fresh 90-day token, so a caddie who opens the app now and then never has to log in again
     * ("log in once and stay logged in"). Admins aren't renewed: their 12-hour limit is on
     * purpose, because the board may be on a shared PC.
     */
    @Transactional(readOnly = true)
    public LoginResponse renew(long userId) {
        User user = users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        if (user.getRole() != Role.CADDIE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin logins aren't renewed - they last 12 hours");
        }
        return new LoginResponse(jwtService.createToken(user), toResponse(user));
    }

    /**
     * Replaces the setup code (or the current password) with a new one. Returns a fresh token,
     * because the old one still says "must change password".
     */
    @Transactional
    public LoginResponse setPassword(long userId, String newPassword) {
        User user = users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a password different from your setup code");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        // No save() needed: inside a transaction, JPA writes changes to loaded entities on commit.
        return new LoginResponse(jwtService.createToken(user), toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(long userId) {
        return users.findById(userId)
            .map(this::toResponse)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
    }

    private UserResponse toResponse(User user) {
        Caddie caddie = caddies.findByUserId(user.getId()).orElse(null);
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getPhone(),
            user.getRole(),
            user.isMustChangePassword(),
            caddie == null ? null : caddie.getGrade(),
            caddie == null ? null : caddie.isPriority());
    }
}
