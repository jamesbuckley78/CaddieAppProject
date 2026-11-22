package com.caddieapp.queue;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.auth.AuthUser;
import com.caddieapp.queue.QueueDtos.BoardResponse;
import com.caddieapp.queue.QueueDtos.ClubListRow;
import com.caddieapp.queue.QueueDtos.MyDayResponse;

@RestController
public class QueueController {

    private final QueueQueryService queries;
    private final ClubListRecalculation recalculation;
    private final Clock clock;

    public QueueController(QueueQueryService queries, ClubListRecalculation recalculation, Clock clock) {
        this.queries = queries;
        this.recalculation = recalculation;
        this.clock = clock;
    }

    /** Caddie: my status, my number in line, my bags, today's total. */
    @GetMapping("/api/queue/me")
    public MyDayResponse me(@AuthenticationPrincipal AuthUser user) {
        return queries.myDay(user.id());
    }

    /** Caddies and admins: the club list (changes once a night). */
    @GetMapping("/api/queue/club-list")
    public List<ClubListRow> clubList() {
        return queries.clubList();
    }

    /** Admin: everyone checked in today, in line order. */
    @GetMapping("/api/admin/board")
    public BoardResponse board() {
        return queries.board();
    }

    /**
     * Admin: re-run the club list order now, using this week's bags up to today. Normally the
     * midnight job does this; this is for testing and for fixing the list after setup.
     */
    @PostMapping("/api/admin/club-list/recalculate")
    public Map<String, Integer> recalculate() {
        return Map.of("moved", recalculation.recalculate(LocalDate.now(clock)));
    }
}
