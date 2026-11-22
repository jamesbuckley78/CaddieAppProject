package com.caddieapp.availability;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Writes the attendance table (days worked, for history and the admin's attendance views). */
@Repository
public class AttendanceRecorder {

    private final JdbcTemplate jdbc;

    public AttendanceRecorder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** First check-in of the day creates the row; checking in again just clears the check-out. */
    public void checkedIn(long caddieId, LocalDate date, LocalDateTime at) {
        jdbc.update("INSERT INTO attendance (caddie_id, date, checked_in_at) VALUES (?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE checked_out_at = NULL", caddieId, date, at);
    }

    public void checkedOut(long caddieId, LocalDate date, LocalDateTime at) {
        jdbc.update("UPDATE attendance SET checked_out_at = ? WHERE caddie_id = ? AND date = ?", at, caddieId, date);
    }
}
