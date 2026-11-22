package com.caddieapp.absence;

import static com.caddieapp.audit.AuditLog.state;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.audit.AuditLog;
import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.caddie.UserRepository;
import com.caddieapp.common.ApiException;

/**
 * "Not coming in" (docs/PLAN.md): a caddie asks at least 24 hours before the day starts, with or
 * without a reason, and the admin approves or rejects it from the Inbox.
 */
@Service
public class AbsenceService {

    public enum Status { PENDING, APPROVED, REJECTED }

    public record AbsenceResponse(long id, long caddieId, String caddieName, LocalDate date, String reason,
                                  Status status, Instant submittedAt, Instant decidedAt) {
    }

    public record InboxResponse(List<AbsenceResponse> pending, List<AbsenceResponse> recentlyDecided) {
    }

    private final AbsenceRepository absences;
    private final CaddieRepository caddies;
    private final UserRepository users;
    private final AuditLog audit;
    private final Clock clock;
    private final long minNoticeHours;

    public AbsenceService(AbsenceRepository absences, CaddieRepository caddies, UserRepository users,
                          AuditLog audit, Clock clock, @Value("${app.absence.min-notice-hours}") long minNoticeHours) {
        this.absences = absences;
        this.caddies = caddies;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
        this.minNoticeHours = minNoticeHours;
    }

    /** The first date a caddie can ask for right now (the start of that day is 24h+ away). */
    public LocalDate earliestDate() {
        return earliestDate(LocalDateTime.now(clock), minNoticeHours);
    }

    static LocalDate earliestDate(LocalDateTime now, long minNoticeHours) {
        LocalDateTime cutoff = now.plusHours(minNoticeHours);
        LocalDate day = cutoff.toLocalDate();
        return cutoff.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? day : day.plusDays(1);
    }

    @Transactional
    public AbsenceResponse request(long userId, LocalDate date, String reason) {
        Caddie caddie = caddieFor(userId);
        LocalDate earliest = earliestDate();
        if (date.isBefore(earliest)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You need to ask at least " + minNoticeHours
                + " hours ahead - the earliest you can pick now is " + earliest);
        }
        if (absences.existsByCaddieIdAndDate(caddie.getId(), date)) {
            throw new ApiException(HttpStatus.CONFLICT, "You've already asked about that day");
        }
        String trimmed = reason == null || reason.isBlank() ? null : reason.trim();
        Absence absence = absences.save(new Absence(caddie, date, trimmed, LocalDateTime.now(clock)));
        return toResponse(absence);
    }

    /** The caddie's own requests from today on. */
    @Transactional(readOnly = true)
    public List<AbsenceResponse> mine(long userId) {
        Caddie caddie = caddieFor(userId);
        return absences.findByCaddieIdAndDateGreaterThanEqualOrderByDateAsc(caddie.getId(), LocalDate.now(clock))
            .stream().map(this::toResponse).toList();
    }

    /** A caddie can withdraw a request until the admin has answered it. */
    @Transactional
    public void cancel(long userId, long absenceId) {
        Caddie caddie = caddieFor(userId);
        Absence absence = absences.findById(absenceId)
            .filter(a -> a.getCaddie().getId().equals(caddie.getId()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!absence.isPending()) {
            throw new ApiException(HttpStatus.CONFLICT, "The caddie master has already answered this one");
        }
        absences.delete(absence);
    }

    /** Admin Inbox: waiting requests, plus what was answered in the last 14 days. */
    @Transactional(readOnly = true)
    public InboxResponse inbox() {
        LocalDate today = LocalDate.now(clock);
        return new InboxResponse(
            absences.findPending(today).stream().map(this::toResponse).toList(),
            absences.findDecidedSince(LocalDateTime.now(clock).minusDays(14)).stream().map(this::toResponse).toList());
    }

    @Transactional
    public AbsenceResponse decide(long adminId, long absenceId, boolean approve) {
        Absence absence = absences.findById(absenceId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!absence.isPending()) {
            throw new ApiException(HttpStatus.CONFLICT, "This request has already been answered");
        }
        absence.decide(approve, users.getReferenceById(adminId), LocalDateTime.now(clock));
        audit.record(adminId, approve ? "APPROVE_ABSENCE" : "REJECT_ABSENCE",
            state("caddie", absence.getCaddie().getUser().getName(), "date", absence.getDate().toString()),
            state("approved", approve));
        return toResponse(absence);
    }

    private Caddie caddieFor(long userId) {
        return caddies.findByUserId(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Only caddies can do this"));
    }

    private AbsenceResponse toResponse(Absence a) {
        Status status = a.getApproved() == null ? Status.PENDING : a.getApproved() ? Status.APPROVED : Status.REJECTED;
        return new AbsenceResponse(a.getId(), a.getCaddie().getId(), a.getCaddie().getUser().getName(), a.getDate(),
            a.getReason(), status, toInstant(a.getSubmittedAt()), toInstant(a.getDecidedAt()));
    }

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(clock.getZone()).toInstant();
    }
}
