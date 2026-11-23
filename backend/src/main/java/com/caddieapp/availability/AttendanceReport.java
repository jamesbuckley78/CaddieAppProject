package com.caddieapp.availability;

import static com.caddieapp.common.ClubTime.toInstant;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.queue.QueueService;

/** Admin's attendance view (docs/PLAN.md): who came in each day of a week, and totals. */
@Service
public class AttendanceReport {

    /** @param checkedInAt one per day Mon-Sun: first check-in time, or null if they didn't come in */
    public record Row(long caddieId, String name, Grade grade, boolean priority, List<Instant> checkedInAt,
                      int daysWorked) {
    }

    /**
     * @param dayTotals  how many caddies came in each day
     * @param caddiesIn  how many different caddies came in at least once that week
     * @param rows       every caddie on the club list, in club list order
     */
    public record Week(LocalDate weekStart, List<LocalDate> days, List<Integer> dayTotals, int caddiesIn,
                       List<Row> rows) {
    }

    private final CaddieRepository caddies;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AttendanceReport(CaddieRepository caddies, JdbcTemplate jdbc, Clock clock) {
        this.caddies = caddies;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** The Monday-Sunday week containing `day`. */
    @Transactional(readOnly = true)
    public Week week(LocalDate day) {
        LocalDate monday = QueueService.weekStart(day);
        List<LocalDate> days = IntStream.range(0, 7).mapToObj(monday::plusDays).toList();

        // caddie id -> day -> first check-in time
        Map<Long, Map<LocalDate, LocalDateTime>> checkIns = new HashMap<>();
        jdbc.query("SELECT caddie_id, date, checked_in_at FROM attendance WHERE date BETWEEN ? AND ?",
            rs -> {
                java.sql.Timestamp at = rs.getTimestamp("checked_in_at");
                if (at != null) {
                    checkIns.computeIfAbsent(rs.getLong("caddie_id"), id -> new HashMap<>())
                        .put(rs.getDate("date").toLocalDate(), at.toLocalDateTime());
                }
            },
            monday, monday.plusDays(6));

        List<Row> rows = caddies.findClubList().stream()
            .map(c -> {
                Map<LocalDate, LocalDateTime> mine = checkIns.getOrDefault(c.getId(), Map.of());
                // Stream.toList() allows nulls, which mean "didn't come in"
                List<Instant> times = days.stream().map(d -> toInstant(mine.get(d), clock)).toList();
                return new Row(c.getId(), c.getUser().getName(), c.getGrade(), c.isPriority(), times, mine.size());
            })
            .toList();
        List<Integer> dayTotals = IntStream.range(0, days.size())
            .mapToObj(i -> (int) rows.stream().filter(r -> r.checkedInAt().get(i) != null).count())
            .toList();
        int caddiesIn = (int) rows.stream().filter(r -> r.daysWorked() > 0).count();
        return new Week(monday, days, dayTotals, caddiesIn, rows);
    }

    public Week thisWeek() {
        return week(LocalDate.now(clock));
    }
}
