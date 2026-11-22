package com.caddieapp.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.caddieapp.common.Enums.BagType;
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
        boolean locationRequired,
        /** Set while status is CALLED: which bag, when, and whether they tapped "On my way" */
        BagType calledBagType,
        Instant calledAt,
        boolean callAccepted) {
    }

    /**
     * The club list as a table - everyone can see this. Order and figures both come from the
     * last daily update at `updatesAt` (4pm), which counted bags up to `countsUpTo`.
     *
     * @param totalBags all bags carried at the club on `countsUpTo`
     * @param days      Monday to Friday of the week being shown, one per day column
     * @param dayTotals the whole club's bags on each of `days` (the Total row)
     * @param weekTotal the whole club's bags this week so far, weekend included
     */
    public record ClubListResponse(
        LocalDate countsUpTo,
        String updatesAt,
        int totalBags,
        List<LocalDate> days,
        List<Integer> dayTotals,
        int weekTotal,
        List<ClubListRow> rows) {
    }

    /**
     * One row of the club list.
     *
     * @param bagsByDay bags on each of `days` (Mon-Fri), same order
     * @param weekTotal all bags this week so far, weekend included - this is what orders the list
     */
    public record ClubListRow(
        int position,
        long caddieId,
        String name,
        Grade grade,
        boolean priority,
        List<Integer> bagsByDay,
        int weekTotal) {
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
        Integer lineNumber,
        /** Admin moved them by hand today (holds until their next bag) */
        boolean moved,
        int missedCalls,
        /** While CALLED */
        BagType calledBagType,
        Instant calledAt,
        boolean callAccepted,
        /** While OUT: the bag they're carrying, so the admin can correct single/double */
        Long currentLoopId,
        BagType currentBagType,
        Instant sentOutAt) {
    }

    /** @param awayToday caddies with an approved "not coming in" for today */
    public record BoardResponse(LocalDate date, int totalBagsToday, List<BoardEntry> entries, List<String> awayToday) {
    }
}
