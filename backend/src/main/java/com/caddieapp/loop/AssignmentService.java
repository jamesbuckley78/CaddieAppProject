package com.caddieapp.loop;

import static com.caddieapp.audit.AuditLog.state;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.audit.AuditLog;
import com.caddieapp.availability.QueueEntry;
import com.caddieapp.availability.QueueEntryRepository;
import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.UserRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.BagType;
import com.caddieapp.common.Enums.QueueStatus;
import com.caddieapp.notification.NotificationService;

/**
 * The admin's actions on the board (docs/RULES.md, "Calling"):
 * Call -> (caddie: On my way) -> Sent out -> Back in, or Call -> No reply.
 *
 * Every action locks the caddie's queue row first (findForUpdate), so two admins tapping at
 * the same moment can't both call or send out the same caddie, and every action is written to
 * the audit log.
 */
@Service
public class AssignmentService {

    private final QueueEntryRepository entries;
    private final LoopRepository loops;
    private final UserRepository users;
    private final AuditLog audit;
    private final NotificationService notifications;
    private final Clock clock;

    public AssignmentService(QueueEntryRepository entries, LoopRepository loops, UserRepository users,
                             AuditLog audit, NotificationService notifications, Clock clock) {
        this.entries = entries;
        this.loops = loops;
        this.users = users;
        this.audit = audit;
        this.notifications = notifications;
        this.clock = clock;
    }

    /** Calls a waiting caddie for a single or double. They get a notification. */
    @Transactional
    public void call(long adminId, long caddieId, BagType bagType) {
        QueueEntry entry = lockToday(caddieId);
        if (!entry.isWaiting()) {
            throw new ApiException(HttpStatus.CONFLICT, name(entry) + " can't be called - they're "
                + describe(entry.getStatus()));
        }
        QueueStatus before = entry.getStatus();
        entry.call(bagType, now());
        audit.record(adminId, "CALL", state("caddie", name(entry), "status", before),
            state("status", QueueStatus.CALLED, "bag", bagType));
        notifications.caddieCalled(entry.getCaddie().getUser(), bagType);
    }

    /** The called caddie has turned up: record the bag and send them out on course. */
    @Transactional
    public void sendOut(long adminId, long caddieId) {
        QueueEntry entry = lockToday(caddieId);
        if (entry.getStatus() != QueueStatus.CALLED) {
            throw new ApiException(HttpStatus.CONFLICT, name(entry) + " hasn't been called");
        }
        BagType bag = entry.getCalledBagType();
        Loop loop = loops.save(new Loop(entry.getCaddie(), entry.getDate(), bag, entry.getCalledAt(), now(),
            users.getReferenceById(adminId)));
        entry.sendOut();
        audit.record(adminId, "SEND_OUT", state("caddie", name(entry)),
            state("status", QueueStatus.OUT, "bag", bag, "loopId", loop.getId()));
    }

    /** Called caddie didn't reply: they keep their place and are next for the following call. */
    @Transactional
    public void noReply(long adminId, long caddieId) {
        QueueEntry entry = lockToday(caddieId);
        if (entry.getStatus() != QueueStatus.CALLED) {
            throw new ApiException(HttpStatus.CONFLICT, name(entry) + " hasn't been called");
        }
        BagType bag = entry.getCalledBagType();
        entry.missCall();
        audit.record(adminId, "NO_REPLY", state("caddie", name(entry), "bag", bag),
            state("status", QueueStatus.AVAILABLE, "missedCalls", entry.getMissedCalls()));
    }

    /** Back from a loop - by the admin (adminId) or the caddie themselves (adminId null). */
    @Transactional
    public void backIn(Long adminId, long caddieId) {
        QueueEntry entry = lockToday(caddieId);
        if (entry.getStatus() != QueueStatus.OUT) {
            throw new ApiException(HttpStatus.CONFLICT, name(entry) + " isn't out on course");
        }
        entry.backIn();
        loops.findFirstByCaddieIdAndDateAndReturnedAtIsNullOrderByIdDesc(caddieId, entry.getDate())
            .ifPresent(loop -> loop.markReturned(now()));
        if (adminId != null) {
            audit.record(adminId, "BACK_IN", state("caddie", name(entry), "status", QueueStatus.OUT),
                state("status", QueueStatus.AVAILABLE));
        }
    }

    /** Fix a mistake: the bag was really a single / double. Changes their bag count straight away. */
    @Transactional
    public void changeBagType(long adminId, long loopId, BagType bagType) {
        Loop loop = loops.findById(loopId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bag not found"));
        BagType before = loop.getBagType();
        loop.changeBagType(bagType);
        audit.record(adminId, "EDIT_BAG",
            state("caddie", loop.getCaddie().getUser().getName(), "loopId", loopId, "bag", before),
            state("bag", bagType));
    }

    /**
     * Manual move in today's line, e.g. back from another job, or fixing a mistake. Holds until
     * the caddie's next bag (docs/RULES.md). lineNumber null = undo the move.
     */
    @Transactional
    public void moveInLine(long adminId, long caddieId, Integer lineNumber, Integer currentLineNumber) {
        QueueEntry entry = lockToday(caddieId);
        if (entry.getStatus() == QueueStatus.OUT || !entry.isCheckedIn()) {
            throw new ApiException(HttpStatus.CONFLICT, name(entry) + " isn't waiting in line");
        }
        Integer before = entry.getManualPosition();
        entry.moveTo(lineNumber);
        audit.record(adminId, lineNumber == null ? "UNDO_MOVE" : "MOVE",
            state("caddie", name(entry), "line", currentLineNumber, "manual", before),
            state("line", lineNumber));
    }

    private QueueEntry lockToday(long caddieId) {
        return entries.findForUpdate(LocalDate.now(clock), caddieId)
            .filter(QueueEntry::isCheckedIn)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "That caddie isn't checked in today"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String name(QueueEntry entry) {
        Caddie caddie = entry.getCaddie();
        return caddie.getUser().getName();
    }

    private static String describe(QueueStatus status) {
        return switch (status) {
            case CALLED -> "already called";
            case OUT -> "out on course";
            default -> "not available";
        };
    }
}
