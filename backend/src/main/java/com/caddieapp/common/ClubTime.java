package com.caddieapp.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Times are stored as the club's local time (Europe/Dublin, no zone in the database). The app
 * gets them as Instants, which turn into the right time on any phone. null stays null.
 */
public final class ClubTime {

    private ClubTime() {
    }

    public static Instant toInstant(LocalDateTime time, Clock clock) {
        return time == null ? null : time.atZone(clock.getZone()).toInstant();
    }
}
