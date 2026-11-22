package com.caddieapp.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.caddieapp.common.ApiException;

class GeoFenceTest {

    // A made-up club centre with a 400 m radius
    private static final double LAT = 53.3000;
    private static final double LON = -6.2000;
    private final GeoFence fence = new GeoFence(new CheckInProperties(true, LAT, LON, 400));

    @Test
    void distanceIsRoughlyRight() {
        // 0.01 degrees of latitude is about 1.11 km anywhere on Earth
        assertThat(GeoFence.distanceMeters(LAT, LON, LAT + 0.01, LON)).isBetween(1100.0, 1125.0);
        assertThat(GeoFence.distanceMeters(LAT, LON, LAT, LON)).isZero();
    }

    @Test
    void insideTheAreaIsAllowed() {
        // ~220 m north
        assertThatCode(() -> fence.check(LAT + 0.002, LON)).doesNotThrowAnyException();
    }

    @Test
    void outsideTheAreaIsRefusedWithTheDistance() {
        assertThatThrownBy(() -> fence.check(LAT + 0.02, LON))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("at the club")
            .hasMessageContaining("2.2 km")
            .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void missingLocationIsRefused() {
        assertThatThrownBy(() -> fence.check(null, null))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("location");
    }

    @Test
    void checkIsSkippedWhenSwitchedOff() {
        GeoFence off = new GeoFence(new CheckInProperties(false, LAT, LON, 400));

        assertThatCode(() -> off.check(null, null)).doesNotThrowAnyException();
        assertThatCode(() -> off.check(0.0, 0.0)).doesNotThrowAnyException();
    }
}
