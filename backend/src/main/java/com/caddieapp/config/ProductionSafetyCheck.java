package com.caddieapp.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Refuses to start in production (app.environment=production) while any development default is
 * still set: the dev JWT secret, the default admin password, the GPS check switched off, or a
 * CORS list with only localhost in it (the hosted admin board couldn't reach the API).
 * Locally (the default, "development") it does nothing.
 */
@Component
@Order(0) // before BootstrapAdmin creates an admin with the default password
public class ProductionSafetyCheck implements ApplicationRunner {

    static final String DEV_JWT_SECRET = "dev-only-secret-change-me-please-at-least-32-chars";
    static final String DEV_ADMIN_PASSWORD = "admin1234";

    private final String environment;
    private final String jwtSecret;
    private final String adminPassword;
    private final boolean requireLocation;
    private final List<String> corsOrigins;

    public ProductionSafetyCheck(@Value("${app.environment}") String environment,
                                 @Value("${app.jwt.secret}") String jwtSecret,
                                 @Value("${app.bootstrap-admin.password}") String adminPassword,
                                 @Value("${app.check-in.require-location}") boolean requireLocation,
                                 @Value("${app.cors.allowed-origins}") List<String> corsOrigins) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.adminPassword = adminPassword;
        this.requireLocation = requireLocation;
        this.corsOrigins = corsOrigins;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"production".equalsIgnoreCase(environment)) {
            return;
        }
        List<String> problems = new ArrayList<>();
        if (DEV_JWT_SECRET.equals(jwtSecret)) {
            problems.add("JWT_SECRET is still the development default");
        }
        if (DEV_ADMIN_PASSWORD.equals(adminPassword)) {
            problems.add("BOOTSTRAP_ADMIN_PASSWORD is still admin1234");
        }
        if (!requireLocation) {
            problems.add("CHECKIN_REQUIRE_LOCATION is false - anyone could check in from home");
        }
        if (corsOrigins.stream().allMatch(o -> o.contains("localhost") || o.contains("127.0.0.1"))) {
            problems.add("CORS_ALLOWED_ORIGINS only allows localhost - set it to the admin site's address");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Not safe to run in production: " + String.join("; ", problems));
        }
    }
}
