package com.caddieapp.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.caddieapp.caddie.User;

/**
 * One-time 6-digit setup codes for new accounts (caddies and admins) and forgotten passwords.
 * Stored hashed like a password and valid for app.setup-code-valid-hours (48 h).
 */
@Component
public class SetupCodes {

    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final long validHours;
    private final SecureRandom random = new SecureRandom();

    public SetupCodes(PasswordEncoder passwordEncoder, Clock clock,
                      @Value("${app.setup-code-valid-hours}") long validHours) {
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.validHours = validHours;
    }

    /**
     * Gives the account a new code, which also logs it out everywhere. Returns the code: the only
     * time it can be read, so the admin must pass it on straight away.
     */
    public String issue(User user) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        user.resetToSetupCode(passwordEncoder.encode(code), LocalDateTime.now(clock).plusHours(validHours));
        return code;
    }
}
