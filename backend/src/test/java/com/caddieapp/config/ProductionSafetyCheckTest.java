package com.caddieapp.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The deploy-day check: production refuses to start while development defaults are still set. */
class ProductionSafetyCheckTest {

    private static final String REAL_SECRET = "a-real-secret-that-is-definitely-longer-than-32-bytes";
    private static final List<String> REAL_SITE = List.of("https://caddies.example.ie");
    private static final List<String> LOCALHOST_ONLY = List.of("http://localhost:*", "http://127.0.0.1:*");

    @Test
    void productionWithRealSettingsStarts() {
        var check = new ProductionSafetyCheck("production", REAL_SECRET, "a-real-password", true, REAL_SITE);

        assertThatCode(() -> check.run(null)).doesNotThrowAnyException();
    }

    @Test
    void productionRefusesWhenOnlyLocalhostMayCallTheApi() {
        var check = new ProductionSafetyCheck("production", REAL_SECRET, "a-real-password", true, LOCALHOST_ONLY);

        assertThatThrownBy(() -> check.run(null)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }

    @Test
    void developmentStartsWithAllTheDefaults() {
        var check = new ProductionSafetyCheck("development", ProductionSafetyCheck.DEV_JWT_SECRET,
            ProductionSafetyCheck.DEV_ADMIN_PASSWORD, false, LOCALHOST_ONLY);

        assertThatCode(() -> check.run(null)).doesNotThrowAnyException();
    }
}
