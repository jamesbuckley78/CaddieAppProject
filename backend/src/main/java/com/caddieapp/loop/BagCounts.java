package com.caddieapp.loop;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Bag totals read straight from the loops table, so there is no separate counter that could
 * drift out of step. A double counts as 2 bags. Plain SQL (JdbcTemplate) because these are
 * simple sums - no entity objects needed.
 *
 * `maxLoopId` limits a count to the loops that existed at a club list update (see
 * ClubListSnapshots). Long.MAX_VALUE = every loop, i.e. live figures.
 */
@Repository
public class BagCounts {

    public static final long ALL_LOOPS = Long.MAX_VALUE;

    private static final String BAGS = "CASE WHEN bag_type = 'DOUBLE' THEN 2 ELSE 1 END";

    private final NamedParameterJdbcTemplate jdbc;

    public BagCounts(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Live bags per caddie between two dates, both included. Caddies with no bags are left out. */
    public Map<Long, Integer> perCaddie(LocalDate from, LocalDate to) {
        return perCaddie(from, to, ALL_LOOPS);
    }

    public Map<Long, Integer> perCaddie(LocalDate from, LocalDate to, long maxLoopId) {
        Map<Long, Integer> result = new HashMap<>();
        jdbc.query("SELECT caddie_id, SUM(" + BAGS + ") AS bags FROM loops "
                + "WHERE date BETWEEN :from AND :to AND id <= :maxId GROUP BY caddie_id",
            params(from, to, maxLoopId),
            rs -> {
                result.put(rs.getLong("caddie_id"), rs.getInt("bags"));
            });
        return result;
    }

    /** Bags per caddie per day between two dates (both included), for the club list table. */
    public Map<Long, Map<LocalDate, Integer>> perCaddiePerDay(LocalDate from, LocalDate to, long maxLoopId) {
        Map<Long, Map<LocalDate, Integer>> result = new HashMap<>();
        jdbc.query("SELECT caddie_id, date, SUM(" + BAGS + ") AS bags FROM loops "
                + "WHERE date BETWEEN :from AND :to AND id <= :maxId GROUP BY caddie_id, date",
            params(from, to, maxLoopId),
            rs -> {
                result.computeIfAbsent(rs.getLong("caddie_id"), id -> new HashMap<>())
                    .put(rs.getDate("date").toLocalDate(), rs.getInt("bags"));
            });
        return result;
    }

    /** All bags carried at the club on one day, live - e.g. "120 bags today" on the admin board. */
    public int total(LocalDate day) {
        Integer total = jdbc.queryForObject("SELECT COALESCE(SUM(" + BAGS + "), 0) FROM loops WHERE date = :day",
            new MapSqlParameterSource("day", day), Integer.class);
        return total == null ? 0 : total;
    }

    /** The newest loop's id (0 if there are none) - recorded with each club list update. */
    public long maxLoopId() {
        Long id = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM loops", new MapSqlParameterSource(), Long.class);
        return id == null ? 0 : id;
    }

    private static MapSqlParameterSource params(LocalDate from, LocalDate to, long maxLoopId) {
        return new MapSqlParameterSource().addValue("from", from).addValue("to", to).addValue("maxId", maxLoopId);
    }
}
