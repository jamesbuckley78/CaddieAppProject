package com.caddieapp.availability;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.caddieapp.common.ApiException;

/** Checks a phone's position is inside the club's check-in area. */
@Component
public class GeoFence {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final CheckInProperties properties;

    public GeoFence(CheckInProperties properties) {
        this.properties = properties;
    }

    public boolean isRequired() {
        return properties.requireLocation();
    }

    /** Throws a 400/403 with a message the app can show if the caddie isn't at the club. */
    public void check(Double latitude, Double longitude) {
        if (!properties.requireLocation()) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Your location is needed to check in. Allow location access and try again.");
        }
        double meters = distanceMeters(latitude, longitude, properties.latitude(), properties.longitude());
        if (meters > properties.radiusMeters()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                "You need to be at the club to check in (you're about " + describe(meters) + " away).");
        }
    }

    /** Great-circle ("haversine") distance between two points on Earth, in metres. */
    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    private static String describe(double meters) {
        return meters < 1000 ? Math.round(meters) + " m" : String.format("%.1f km", meters / 1000);
    }
}
