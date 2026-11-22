package com.caddieapp.audit;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Records every admin change to the list (CLAUDE.md): calls, sending out, moves, bag edits,
 * absence decisions. `before` and `after` are small maps stored as JSON so they can be read
 * back later ("who moved Sean from 12 to 3, and when?").
 */
@Repository
public class AuditLog {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Clock clock;

    public AuditLog(JdbcTemplate jdbc, ObjectMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    /** state("caddie", "Sean Murphy", "line", 12) - like Map.of but allows null values. */
    public static Map<String, Object> state(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            map.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return map;
    }

    public void record(long adminUserId, String action, Map<String, ?> before, Map<String, ?> after) {
        jdbc.update("INSERT INTO audit_log (admin_id, action, before_state, after_state, created_at) VALUES (?, ?, ?, ?, ?)",
            adminUserId, action, toJson(before), toJson(after), LocalDateTime.now(clock));
    }

    private String toJson(Map<String, ?> state) {
        if (state == null) {
            return null;
        }
        try {
            return json.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write audit entry", e);
        }
    }
}
