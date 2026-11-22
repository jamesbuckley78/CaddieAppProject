package com.caddieapp.availability;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.QueueStatus;
import com.caddieapp.loop.AssignmentService;

/** A caddie's own status changes: I'm here, getting food, will be back, finished for today. */
@Service
public class AvailabilityService {

    /** What a caddie can choose for themselves. CALLED and OUT are set by the admin. */
    static final Set<QueueStatus> SELF_SERVICE = EnumSet.of(QueueStatus.AVAILABLE, QueueStatus.FOOD,
        QueueStatus.BACK_LATER);

    private final CaddieRepository caddies;
    private final QueueEntryRepository entries;
    private final AttendanceRecorder attendance;
    private final GeoFence geoFence;
    private final AssignmentService assignments;
    private final Clock clock;

    public AvailabilityService(CaddieRepository caddies, QueueEntryRepository entries,
                               AttendanceRecorder attendance, GeoFence geoFence, AssignmentService assignments,
                               Clock clock) {
        this.caddies = caddies;
        this.entries = entries;
        this.attendance = attendance;
        this.geoFence = geoFence;
        this.assignments = assignments;
        this.clock = clock;
    }

    /** "I'm here". Needs the phone to be at the club (unless already checked in today). */
    @Transactional
    public void checkIn(long userId, Double latitude, Double longitude) {
        Caddie caddie = caddieFor(userId);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        QueueEntry entry = entries.findByDateAndCaddieId(today, caddie.getId()).orElse(null);

        if (entry != null && entry.isCheckedIn()) {
            return; // pressed twice - nothing to do
        }
        geoFence.check(latitude, longitude);

        if (entry == null) {
            entries.save(new QueueEntry(today, caddie, now));
        } else {
            entry.checkInAgain(now);
        }
        attendance.checkedIn(caddie.getId(), today, now);
    }

    /**
     * Available / getting food / will be back. They keep their place in line whichever they pick;
     * it just tells the admin whether they can answer straight away.
     */
    @Transactional
    public void changeStatus(long userId, QueueStatus status, Integer backInMinutes) {
        if (!SELF_SERVICE.contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can't set that status yourself");
        }
        QueueEntry entry = todaysEntry(caddieFor(userId));
        if (entry.getStatus() == QueueStatus.OUT || entry.getStatus() == QueueStatus.CALLED) {
            throw new ApiException(HttpStatus.CONFLICT, "You've been given a bag - the caddie master will update you");
        }
        LocalDateTime backAt = null;
        if (status == QueueStatus.BACK_LATER) {
            if (backInMinutes == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Say roughly when you'll be back");
            }
            backAt = LocalDateTime.now(clock).plusMinutes(backInMinutes);
        }
        entry.changeStatus(status, backAt);
    }

    /** "Finished for today": leaves today's line. They can check in again later (GPS again). */
    @Transactional
    public void checkOut(long userId) {
        Caddie caddie = caddieFor(userId);
        QueueEntry entry = todaysEntry(caddie);
        if (entry.getStatus() == QueueStatus.OUT) {
            throw new ApiException(HttpStatus.CONFLICT, "You're out on course - finish your loop first");
        }
        if (entry.getStatus() == QueueStatus.CALLED) {
            throw new ApiException(HttpStatus.CONFLICT, "You've been called - tell the caddie master first");
        }
        entry.changeStatus(QueueStatus.OFF, null);
        attendance.checkedOut(caddie.getId(), entry.getDate(), LocalDateTime.now(clock));
    }

    /** "On my way" after being called - shows on the admin's board. */
    @Transactional
    public void acceptCall(long userId) {
        QueueEntry entry = todaysEntry(caddieFor(userId));
        if (entry.getStatus() != QueueStatus.CALLED) {
            throw new ApiException(HttpStatus.CONFLICT, "You haven't been called");
        }
        entry.acceptCall(LocalDateTime.now(clock));
    }

    /** "Back in" after a loop: available again. */
    @Transactional
    public void backIn(long userId) {
        assignments.backIn(null, caddieFor(userId).getId());
    }

    private Caddie caddieFor(long userId) {
        return caddies.findByUserId(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Only caddies can check in"));
    }

    private QueueEntry todaysEntry(Caddie caddie) {
        return entries.findByDateAndCaddieId(LocalDate.now(clock), caddie.getId())
            .filter(QueueEntry::isCheckedIn)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Tap \"I'm here\" first"));
    }
}
