package com.caddieapp.queue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.availability.GeoFence;
import com.caddieapp.availability.QueueEntry;
import com.caddieapp.availability.QueueEntryRepository;
import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.QueueStatus;
import com.caddieapp.loop.BagCounts;
import com.caddieapp.queue.QueueDtos.BoardEntry;
import com.caddieapp.queue.QueueDtos.BoardResponse;
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
    private final Clock clock;

    public QueueQueryService(CaddieRepository caddies, QueueEntryRepository entries, BagCounts bagCounts,
                             QueueService queueService, GeoFence geoFence, Clock clock) {
        this.caddies = caddies;
        this.entries = entries;
        this.bagCounts = bagCounts;
        this.queueService = queueService;
        this.geoFence = geoFence;
        this.clock = clock;
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

        List<Candidate> line = queueService.todaysLine(checkedIn.values().stream()
            .map(e -> candidate(e.getCaddie(), bags))
            .toList());
        // Out on course = not waiting in line, so no number
        Map<Long, Integer> numbers = queueService.lineNumbers(line,
            c -> checkedIn.get(c.caddieId()).getStatus() == QueueStatus.OUT);

        List<QueueEntry> ordered = line.stream().map(c -> checkedIn.get(c.caddieId())).toList();
        return new Today(date, ordered, bags, numbers, bagCounts.total(date));
    }

    private static Candidate candidate(Caddie caddie, Map<Long, Integer> bags) {
        return new Candidate(caddie.getId(), caddie.isPriority(), bags.getOrDefault(caddie.getId(), 0),
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
            today.totalBags(),
            geoFence.isRequired());
    }

    @Transactional(readOnly = true)
    public List<ClubListRow> clubList() {
        LocalDate date = LocalDate.now(clock);
        Map<Long, Integer> bags = bagCounts.perCaddie(QueueService.weekStart(date), date);
        return caddies.findClubList().stream()
            .map(c -> new ClubListRow(c.getListPosition(), c.getId(), c.getUser().getName(), c.getGrade(),
                c.isPriority(), bags.getOrDefault(c.getId(), 0)))
            .toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse board() {
        Today today = today();
        List<BoardEntry> rows = today.line().stream()
            .map(e -> {
                Caddie c = e.getCaddie();
                return new BoardEntry(c.getId(), c.getUser().getName(), c.getGrade(), c.isPriority(),
                    e.getStatus(), toInstant(e.getBackAt()), toInstant(e.getCheckedInAt()),
                    today.bags().getOrDefault(c.getId(), 0), c.getListPosition(),
                    today.lineNumbers().get(c.getId()));
            })
            .toList();
        return new BoardResponse(today.date(), today.totalBags(), rows);
    }

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(clock.getZone()).toInstant();
    }
}
