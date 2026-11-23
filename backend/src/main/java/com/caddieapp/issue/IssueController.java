package com.caddieapp.issue;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.auth.AuthUser;
import com.caddieapp.issue.IssueReports.Issue;
import com.caddieapp.issue.IssueReports.IssueType;
import com.caddieapp.issue.IssueReports.Summary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
public class IssueController {

    /** hole and message are optional. */
    public record IssueRequest(@NotNull IssueType type, @Min(1) @Max(18) Integer hole, @Size(max = 1000) String message) {
    }

    private final IssueReports issues;

    public IssueController(IssueReports issues) {
        this.issues = issues;
    }

    /** Caddie: report an issue. The admin board shows a banner within 10 seconds. */
    @PostMapping("/api/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public void report(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody IssueRequest request) {
        String message = request.message() == null || request.message().isBlank() ? null : request.message().trim();
        issues.create(user.id(), request.type(), request.hole(), message);
    }

    /** Admin Inbox: issues nobody has dealt with yet. */
    @GetMapping("/api/admin/issues")
    public List<Issue> open() {
        return issues.open();
    }

    /**
     * Admin: every report in the week containing `date` (e.g. 2026-11-25), or all time if left out,
     * with counts per type and per caddie (who reports the most).
     */
    @GetMapping("/api/admin/issues/summary")
    public Summary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return issues.summary(date);
    }

    @PostMapping("/api/admin/issues/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@AuthenticationPrincipal AuthUser admin, @PathVariable long id) {
        issues.resolve(admin.id(), id);
    }
}
