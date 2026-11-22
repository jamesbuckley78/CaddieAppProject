package com.caddieapp.auth;

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
import com.caddieapp.common.PhoneNumbers;

@Service
public class AuthService {

    /** Same message for unknown phone and wrong password, so nobody can probe which numbers exist. */
    static final String BAD_LOGIN = "Wrong phone number or password";

    private final UserRepository users;
    private final CaddieRepository caddies;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, CaddieRepository caddies,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.caddies = caddies;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Phone + password (or the one-time setup code) -> token. */
    @Transactional(readOnly = true)
    public LoginResponse login(String phone, String password) {
        User user = users.findByPhone(PhoneNumbers.normalise(phone))
            .filter(u -> u.getPasswordHash() != null)
            .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, BAD_LOGIN));
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
