package com.caddieapp.availability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The club's check-in area, from app.check-in.* in application.yml. Set in the code on
 * purpose: admins can't change it from the app.
 *
 * @param requireLocation false skips the GPS check (local testing only)
 * @param latitude        centre of the area, e.g. the caddie shack
 * @param longitude       centre of the area
 * @param radiusMeters    how far from the centre still counts as "at the club"
 */
@ConfigurationProperties(prefix = "app.check-in")
public record CheckInProperties(boolean requireLocation, double latitude, double longitude, double radiusMeters) {
}
