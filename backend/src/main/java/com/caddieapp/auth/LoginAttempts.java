package com.caddieapp.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.caddieapp.common.ApiException;

/**
 * Stops anyone guessing a password or a 6-digit setup code: after `maxAttempts` wrong tries on
 * one phone number within `lockMinutes`, that number is locked for `lockMinutes`.
 * Kept in memory - fine for one server; it resets if the backend restarts.
 */
@Component
public class LoginAttempts {

    private final int maxAttempts;
    private final Duration lockTime;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockedUntil = new ConcurrentHashMap<>();

    public LoginAttempts(@Value("${app.login.max-attempts}") int maxAttempts,
                         @Value("${app.login.lock-minutes}") long lockMinutes, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockTime = Duration.ofMinutes(lockMinutes);
        this.clock = clock;
    }

    /** Throws 429 Too Many Requests if this phone number is locked. */
    public void checkAllowed(String phone) {
        Instant until = lockedUntil.get(phone);
        Instant now = clock.instant();
        if (until != null && now.isBefore(until)) {
            long minutes = Math.max(1, Duration.between(now, until).toMinutes() + 1);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Too many wrong attempts. Try again in " + minutes + " minute" + (minutes == 1 ? "" : "s") + ".");
        }
        if (until != null) {
            lockedUntil.remove(phone);
        }
    }

    public void failed(String phone) {
        Instant now = clock.instant();
        Deque<Instant> recent = failures.computeIfAbsent(phone, p -> new ArrayDeque<>());
        synchronized (recent) {
            recent.addLast(now);
            while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(lockTime))) {
                recent.removeFirst();
            }
            if (recent.size() >= maxAttempts) {
                lockedUntil.put(phone, now.plus(lockTime));
                recent.clear();
            }
        }
    }

    public void succeeded(String phone) {
        failures.remove(phone);
        lockedUntil.remove(phone);
    }
}
