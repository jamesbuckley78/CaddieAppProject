package com.caddieapp.availability;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.availability.AttendanceReport.Week;

/** Admin-only (SecurityConfig: /api/admin/**). */
@RestController
public class AttendanceController {

    private final AttendanceReport report;

    public AttendanceController(AttendanceReport report) {
        this.report = report;
    }

    /** The week containing `date` (any day of it, e.g. 2026-11-23); this week if left out. */
    @GetMapping("/api/admin/attendance")
    public Week week(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return date == null ? report.thisWeek() : report.week(date);
    }
}
