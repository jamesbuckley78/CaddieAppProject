package com.caddieapp.loop;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.BagType;
import com.caddieapp.queue.QueueService;

/** A caddie's own history: their loops and days worked, week by week (newest first). */
@Service
public class HistoryService {

    public record LoopRow(long id, LocalDate date, BagType bagType, Instant sentOutAt, Instant returnedAt) {
    }

    /** @param bags a double counts as 2 */
    public record Week(LocalDate weekStart, int bags, int loops, int daysWorked, List<LoopRow> loopRows) {
    }

    public record HistoryResponse(int totalBags, int totalDaysWorked, List<Week> weeks) {
    }

    private final CaddieRepository caddies;
    private final LoopRepository loops;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public HistoryService(CaddieRepository caddies, LoopRepository loops, JdbcTemplate jdbc, Clock clock) {
        this.caddies = caddies;
        this.loops = loops;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HistoryResponse forUser(long userId, int weeks) {
        Caddie caddie = caddies.findByUserId(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Only caddies have a history"));
        LocalDate today = LocalDate.now(clock);
        LocalDate from = QueueService.weekStart(today).minusWeeks(weeks - 1L);

        List<Loop> history = loops.findHistory(caddie.getId(), from, today);
        Set<LocalDate> worked = jdbc.queryForList(
                "SELECT date FROM attendance WHERE caddie_id = ? AND date BETWEEN ? AND ?",
                java.sql.Date.class, caddie.getId(), from, today)
            .stream().map(java.sql.Date::toLocalDate).collect(Collectors.toCollection(java.util.HashSet::new));
        // A loop also counts as a day worked, in case attendance wasn't recorded (e.g. old data)
        history.forEach(l -> worked.add(l.getDate()));

        // Group by week, newest week first; only weeks with something in them
        Map<LocalDate, List<Loop>> loopsByWeek = new TreeMap<>(java.util.Comparator.reverseOrder());
        history.forEach(l -> loopsByWeek.computeIfAbsent(QueueService.weekStart(l.getDate()), w -> new ArrayList<>()).add(l));
        worked.forEach(d -> loopsByWeek.computeIfAbsent(QueueService.weekStart(d), w -> new ArrayList<>()));

        List<Week> result = loopsByWeek.entrySet().stream()
            .map(e -> {
                LocalDate weekStart = e.getKey();
                List<Loop> weekLoops = e.getValue();
                int days = (int) worked.stream().filter(d -> QueueService.weekStart(d).equals(weekStart)).count();
                return new Week(weekStart, weekLoops.stream().mapToInt(l -> l.getBagType().bags()).sum(),
                    weekLoops.size(), days,
                    weekLoops.stream().map(l -> new LoopRow(l.getId(), l.getDate(), l.getBagType(),
                        toInstant(l.getSentOutAt()), toInstant(l.getReturnedAt()))).toList());
            })
            .toList();
        return new HistoryResponse(result.stream().mapToInt(Week::bags).sum(), worked.size(), result);
    }

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(clock.getZone()).toInstant();
    }
}
