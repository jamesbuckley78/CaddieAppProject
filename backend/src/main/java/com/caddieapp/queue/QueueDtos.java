package com.caddieapp.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.QueueStatus;

/** Times are sent as Instants (UTC, e.g. "2026-11-22T10:45:00Z") so phones show them in local time. */
public final class QueueDtos {

    private QueueDtos() {
    }

    /** The caddie's own day. They see their number, never who else is checked in. */
    public record MyDayResponse(
        long caddieId,
        boolean checkedIn,
        QueueStatus status,
        Instant backAt,
        /** "You're 5th in line" - null when not checked in or out on course */
        Integer lineNumber,
        int bagsThisWeek,
        int clubPosition,
        int totalBagsToday,
        boolean locationRequired) {
    }

    /** One row of the club list - everyone can see this. */
    public record ClubListRow(
        int position,
        long caddieId,
        String name,
        Grade grade,
        boolean priority,
        int bagsThisWeek) {
    }

    /** One checked-in caddie on the admin board, already in today's order. */
    public record BoardEntry(
        long caddieId,
        String name,
        Grade grade,
        boolean priority,
        QueueStatus status,
        Instant backAt,
        Instant checkedInAt,
        int bagsThisWeek,
        int clubPosition,
        Integer lineNumber) {
    }

    public record BoardResponse(LocalDate date, int totalBagsToday, List<BoardEntry> entries) {
    }
}
