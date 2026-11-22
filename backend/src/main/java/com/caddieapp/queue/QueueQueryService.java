package com.caddieapp.queue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.absence.AbsenceRepository;
import com.caddieapp.availability.GeoFence;
import com.caddieapp.availability.QueueEntry;
import com.caddieapp.availability.QueueEntryRepository;
import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.QueueStatus;
import com.caddieapp.loop.BagCounts;
import com.caddieapp.loop.Loop;
import com.caddieapp.loop.LoopRepository;
import com.caddieapp.queue.QueueDtos.BoardEntry;
import com.caddieapp.queue.QueueDtos.BoardResponse;
import com.caddieapp.queue.ClubListSnapshots.Snapshot;
import com.caddieapp.queue.QueueDtos.ClubListResponse;
import com.caddieapp.queue.QueueDtos.ClubListRow;
import com.caddieapp.queue.QueueDtos.MyDayResponse;
import com.caddieapp.queue.QueueService.Candidate;

/** Builds the read-only views: the caddie's day, the club list and the admin board. */
@Service
public class QueueQueryService {

    private final CaddieRepository caddies;
    private final QueueEntryRepository entries;
    private final BagCounts bagCounts;
    private final QueueService queueService;
    private final GeoFence geoFence;
    private final ClubListSnapshots snapshots;
    private final LoopRepository loops;
    private final AbsenceRepository absences;
    private final Clock clock;
    private final LocalTime clubListUpdateTime;

    public QueueQueryService(CaddieRepository caddies, QueueEntryRepository entries, BagCounts bagCounts,
                             QueueService queueService, GeoFence geoFence, ClubListSnapshots snapshots,
                             LoopRepository loops, AbsenceRepository absences, Clock clock,
                             @Value("${app.club-list.daily-update-time}") String clubListUpdateTime) {
        this.caddies = caddies;
        this.entries = entries;
        this.bagCounts = bagCounts;
        this.snapshots = snapshots;
        this.loops = loops;
        this.absences = absences;
        this.queueService = queueService;
        this.geoFence = geoFence;
        this.clock = clock;
        this.clubListUpdateTime = LocalTime.parse(clubListUpdateTime);
    }

    /** Today's checked-in caddies in line order, with week-to-date bags (including today). */
    private record Today(LocalDate date, List<QueueEntry> line, Map<Long, Integer> bags,
                         Map<Long, Integer> lineNumbers, int totalBags) {
    }

    private Today today() {
        LocalDate date = LocalDate.now(clock);
        Map<Long, Integer> bags = bagCounts.perCaddie(QueueService.weekStart(date), date);
        Map<Long, QueueEntry> checkedIn = entries.findDay(date).stream()
            .filter(QueueEntry::isCheckedIn)
            .collect(Collectors.toMap(e -> e.getCaddie().getId(), Function.identity()));

        List<Candidate> sorted = queueService.todaysLine(checkedIn.values().stream()
            .map(e -> candidate(e.getCaddie(), bags))
            .toList());
        // Waiting caddies (with the admin's manual moves applied) get numbers; out on course don't
        Predicate<Candidate> out = c -> checkedIn.get(c.caddieId()).getStatus() == QueueStatus.OUT;
        Map<Long, Integer> manual = new HashMap<>();
        checkedIn.values().stream()
            .filter(e -> e.getManualPosition() != null && e.getStatus() != QueueStatus.OUT)
            .forEach(e -> manual.put(e.getCaddie().getId(), e.getManualPosition()));
        List<Candidate> line = new ArrayList<>(queueService.applyManualMoves(
            sorted.stream().filter(out.negate()).toList(), manual));
        line.addAll(sorted.stream().filter(out).toList());

        Map<Long, Integer> numbers = queueService.lineNumbers(line, out);
        List<QueueEntry> ordered = line.stream().map(c -> checkedIn.get(c.caddieId())).toList();
        return new Today(date, ordered, bags, numbers, bagCounts.total(date));
    }

    private static Candidate candidate(Caddie caddie, Map<Long, Integer> bags) {
        return new Candidate(caddie.getId(), caddie.isPriority(), caddie.getGrade() == Grade.JUNIOR,
            bags.getOrDefault(caddie.getId(), 0),
            caddie.getListPosition());
    }

    @Transactional(readOnly = true)
    public MyDayResponse myDay(long userId) {
        Caddie caddie = caddies.findByUserId(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Only caddies have a place in the list"));
        Today today = today();
        QueueEntry mine = today.line().stream()
            .filter(e -> e.getCaddie().getId().equals(caddie.getId()))
            .findFirst().orElse(null);
        return new MyDayResponse(
            caddie.getId(),
            mine != null,
            mine == null ? QueueStatus.OFF : mine.getStatus(),
            mine == null ? null : toInstant(mine.getBackAt()),
            today.lineNumbers().get(caddie.getId()),
            today.bags().getOrDefault(caddie.getId(), 0),
            caddie.getListPosition(),
            geoFence.isRequired(),
            mine == null ? null : mine.getCalledBagType(),
            mine == null ? null : toInstant(mine.getCalledAt()),
            mine != null && mine.getCallAcceptedAt() != null);
    }

    /**
     * The club list table, exactly as of the last daily update (4pm): the same bags the order
     * was worked out from. Bags entered after that wait for the next update.
     */
    @Transactional(readOnly = true)
    public ClubListResponse clubList() {
        // Normally there's always a snapshot (ClubListRecalculation catches up on start-up);
        // the fallback just shows live figures up to the expected day.
        Snapshot snapshot = snapshots.latest().orElseGet(() -> new Snapshot(LocalDateTime.now(clock),
            QueueService.clubListCountsUpTo(LocalDateTime.now(clock), clubListUpdateTime), BagCounts.ALL_LOOPS));
        LocalDate upTo = snapshot.countsUpTo();
        LocalDate monday = QueueService.weekStart(upTo);
        List<LocalDate> weekdays = IntStream.range(0, 5).mapToObj(monday::plusDays).toList(); // Mon-Fri
        Map<Long, Map<LocalDate, Integer>> bags = bagCounts.perCaddiePerDay(monday, upTo, snapshot.maxLoopId());

        List<ClubListRow> rows = caddies.findClubList().stream()
            .map(c -> {
                Map<LocalDate, Integer> mine = bags.getOrDefault(c.getId(), Map.of());
                List<Integer> byDay = weekdays.stream().map(d -> mine.getOrDefault(d, 0)).toList();
                int weekTotal = mine.values().stream().mapToInt(Integer::intValue).sum();
                return new ClubListRow(c.getListPosition(), c.getId(), c.getUser().getName(), c.getGrade(),
                    c.isPriority(), byDay, weekTotal);
            })
            .toList();

        // The Total row: add up each column
        List<Integer> dayTotals = IntStream.range(0, weekdays.size())
            .mapToObj(i -> rows.stream().mapToInt(r -> r.bagsByDay().get(i)).sum())
            .toList();
        int weekTotal = rows.stream().mapToInt(ClubListRow::weekTotal).sum();
        int totalOnLastDay = bags.values().stream().mapToInt(days -> days.getOrDefault(upTo, 0)).sum();

        return new ClubListResponse(upTo, clubListUpdateTime.toString(), totalOnLastDay, weekdays, dayTotals,
            weekTotal, rows);
    }

    @Transactional(readOnly = true)
    public BoardResponse board() {
        Today today = today();
        // The bag each caddie out on course is carrying (so the admin can correct it)
        Map<Long, Loop> outNow = new HashMap<>();
        loops.findByDateAndReturnedAtIsNull(today.date()).forEach(l -> outNow.put(l.getCaddie().getId(), l));

        List<BoardEntry> rows = today.line().stream()
            .map(e -> {
                Caddie c = e.getCaddie();
                Loop loop = e.getStatus() == QueueStatus.OUT ? outNow.get(c.getId()) : null;
                return new BoardEntry(c.getId(), c.getUser().getName(), c.getGrade(), c.isPriority(),
                    e.getStatus(), toInstant(e.getBackAt()), toInstant(e.getCheckedInAt()),
                    today.bags().getOrDefault(c.getId(), 0), c.getListPosition(),
                    today.lineNumbers().get(c.getId()),
                    e.getManualPosition() != null, e.getMissedCalls(),
                    e.getCalledBagType(), toInstant(e.getCalledAt()), e.getCallAcceptedAt() != null,
                    loop == null ? null : loop.getId(), loop == null ? null : loop.getBagType(),
                    loop == null ? null : toInstant(loop.getSentOutAt()));
            })
            .toList();
        return new BoardResponse(today.date(), today.totalBags(), rows, absences.approvedNamesOn(today.date()));
    }

    /** Today's line number for one caddie, or null (for the audit log when moving). */
    @Transactional(readOnly = true)
    public Integer lineNumberOf(long caddieId) {
        return today().lineNumbers().get(caddieId);
    }

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(clock.getZone()).toInstant();
    }
}
