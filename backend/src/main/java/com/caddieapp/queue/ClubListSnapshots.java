package com.caddieapp.queue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Remembers each daily club list update: when it ran, which day the figures go up to, and the
 * newest loop it counted. The club list screen shows figures from the latest one.
 */
@Repository
public class ClubListSnapshots {

    public record Snapshot(LocalDateTime takenAt, LocalDate countsUpTo, long maxLoopId) {
    }

    private final JdbcTemplate jdbc;

    public ClubListSnapshots(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Snapshot> latest() {
        return jdbc.query("SELECT taken_at, counts_up_to, max_loop_id FROM club_list_snapshots ORDER BY id DESC LIMIT 1",
            (rs, row) -> new Snapshot(rs.getTimestamp("taken_at").toLocalDateTime(),
                rs.getDate("counts_up_to").toLocalDate(), rs.getLong("max_loop_id")))
            .stream().findFirst();
    }

    public void save(Snapshot snapshot) {
        jdbc.update("INSERT INTO club_list_snapshots (taken_at, counts_up_to, max_loop_id) VALUES (?, ?, ?)",
            snapshot.takenAt(), snapshot.countsUpTo(), snapshot.maxLoopId());
    }
}
