package com.caddieapp.issue;

import static com.caddieapp.common.ClubTime.toInstant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.caddieapp.common.ApiException;
import com.caddieapp.queue.QueueService;

/**
 * Issue reports from caddies (docs/PLAN.md): slow play, request a ranger, golfer problem,
 * golfer wants to go in, missing item. They wait as OPEN until an admin resolves them.
 * Plain SQL (JdbcTemplate), like AttendanceRecorder: no entity needed for one small table.
 */
@Repository
public class IssueReports {

    /** Stored as their names (issue_reports.type), so never rename a value. */
    public enum IssueType { SLOW_PLAY, RANGER, GOLFER_PROBLEM, GOLFER_GOING_IN, MISSING_ITEM }

    /** @param resolvedBy the admin's name, null while open */
    public record Issue(long id, String caddieName, IssueType type, Integer hole, String message, Instant createdAt,
                        boolean resolved, String resolvedBy, Instant resolvedAt) {
    }

    public record CaddieCount(long caddieId, String name, int reports) {
    }

    /**
     * Issue history for one week, or all time (weekStart null).
     *
     * @param byCaddie who reported how many, most first - spots someone reporting far more than others
     * @param issues   newest first, at most HISTORY_LIMIT
     */
    public record Summary(LocalDate weekStart, int total, Map<IssueType, Integer> byType, List<CaddieCount> byCaddie,
                          List<Issue> issues) {
    }

    // shortcut: all-time history lists only the newest 200 (the counts include everything); add paging if that's too few
    static final int HISTORY_LIMIT = 200;

    private static final String SELECT_ISSUES = "SELECT i.id, u.name, i.type, i.hole, i.message, i.created_at, "
        + "i.status, r.name AS resolved_by, i.resolved_at FROM issue_reports i "
        + "JOIN caddies c ON c.id = i.caddie_id JOIN users u ON u.id = c.user_id "
        + "LEFT JOIN users r ON r.id = i.resolved_by ";

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public IssueReports(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Throws 403 if the user isn't a caddie (the INSERT finds no caddie row for them). */
    public void create(long userId, IssueType type, Integer hole, String message) {
        int rows = jdbc.update("INSERT INTO issue_reports (caddie_id, type, hole, message, created_at) "
                + "SELECT c.id, ?, ?, ?, ? FROM caddies c WHERE c.user_id = ?",
            type.name(), hole, message, LocalDateTime.now(clock), userId);
        if (rows == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only caddies can report issues");
        }
    }

    /** Waiting for an admin, oldest first. */
    public List<Issue> open() {
        return jdbc.query(SELECT_ISSUES + "WHERE i.status = 'OPEN' ORDER BY i.created_at, i.id", this::issue);
    }

    public int countOpen() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM issue_reports WHERE status = 'OPEN'", Integer.class);
        return count == null ? 0 : count;
    }

    /** Throws 409 if it's already resolved (e.g. by another admin a moment ago) or doesn't exist. */
    public void resolve(long adminId, long issueId) {
        int rows = jdbc.update("UPDATE issue_reports SET status = 'RESOLVED', resolved_by = ?, resolved_at = ? "
            + "WHERE id = ? AND status = 'OPEN'", adminId, LocalDateTime.now(clock), issueId);
        if (rows == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "That issue has already been resolved");
        }
    }

    /** Every report (open and resolved) in the Monday-Sunday week containing `day`, or all time if `day` is null. */
    public Summary summary(LocalDate day) {
        LocalDate weekStart = day == null ? null : QueueService.weekStart(day);
        // Week: from Monday 00:00 up to (not including) the next Monday 00:00
        String range = weekStart == null ? "" : "WHERE i.created_at >= ? AND i.created_at < ? ";
        Object[] params = weekStart == null ? new Object[0]
            : new Object[] {weekStart.atStartOfDay(), weekStart.plusWeeks(1).atStartOfDay()};

        Map<IssueType, Integer> byType = new EnumMap<>(IssueType.class);
        jdbc.query("SELECT i.type, COUNT(*) AS n FROM issue_reports i " + range + "GROUP BY i.type",
            rs -> {
                byType.put(IssueType.valueOf(rs.getString("type")), rs.getInt("n"));
            }, params);
        List<CaddieCount> byCaddie = jdbc.query("SELECT c.id, u.name, COUNT(*) AS n FROM issue_reports i "
                + "JOIN caddies c ON c.id = i.caddie_id JOIN users u ON u.id = c.user_id " + range
                + "GROUP BY c.id, u.name ORDER BY n DESC, u.name",
            (rs, row) -> new CaddieCount(rs.getLong("id"), rs.getString("name"), rs.getInt("n")), params);
        List<Object> listParams = new ArrayList<>(List.of(params));
        listParams.add(HISTORY_LIMIT);
        List<Issue> issues = jdbc.query(SELECT_ISSUES + range + "ORDER BY i.created_at DESC, i.id DESC LIMIT ?",
            this::issue, listParams.toArray());

        int total = byType.values().stream().mapToInt(Integer::intValue).sum();
        return new Summary(weekStart, total, byType, byCaddie, issues);
    }

    private Issue issue(ResultSet rs, int row) throws SQLException {
        return new Issue(rs.getLong("id"), rs.getString("name"), IssueType.valueOf(rs.getString("type")),
            (Integer) rs.getObject("hole"), rs.getString("message"), toInstant(rs.getObject("created_at", LocalDateTime.class), clock),
            "RESOLVED".equals(rs.getString("status")), rs.getString("resolved_by"),
            toInstant(rs.getObject("resolved_at", LocalDateTime.class), clock));
    }
}
