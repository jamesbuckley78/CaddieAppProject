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
 */
@Repository
public class BagCounts {

    private static final String BAGS = "CASE WHEN bag_type = 'DOUBLE' THEN 2 ELSE 1 END";

    private final NamedParameterJdbcTemplate jdbc;

    public BagCounts(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Bags per caddie between two dates, both included. Caddies with no bags are left out. */
    public Map<Long, Integer> perCaddie(LocalDate from, LocalDate to) {
        Map<Long, Integer> result = new HashMap<>();
        jdbc.query("SELECT caddie_id, SUM(" + BAGS + ") AS bags FROM loops "
                + "WHERE date BETWEEN :from AND :to GROUP BY caddie_id",
            new MapSqlParameterSource().addValue("from", from).addValue("to", to),
            rs -> {
                result.put(rs.getLong("caddie_id"), rs.getInt("bags"));
            });
        return result;
    }

    /** All bags carried at the club on one day, e.g. "120 bags today". */
    public int total(LocalDate day) {
        Integer total = jdbc.queryForObject("SELECT COALESCE(SUM(" + BAGS + "), 0) FROM loops WHERE date = :day",
            new MapSqlParameterSource("day", day), Integer.class);
        return total == null ? 0 : total;
    }
}
