package com.caddieapp.absence;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.absence.AbsenceService.AbsenceResponse;
import com.caddieapp.absence.AbsenceService.InboxResponse;
import com.caddieapp.auth.AuthUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
public class AbsenceController {

    public record AbsenceRequest(@NotNull LocalDate date, @Size(max = 500) String reason) {
    }

    public record DecisionRequest(@NotNull Boolean approve) {
    }

    private final AbsenceService absences;

    public AbsenceController(AbsenceService absences) {
        this.absences = absences;
    }

    /** Caddie: my requests, and the earliest date I can ask for now. */
    @GetMapping("/api/absences/mine")
    public Map<String, Object> mine(@AuthenticationPrincipal AuthUser user) {
        return Map.of("earliestDate", absences.earliestDate(), "requests", absences.mine(user.id()));
    }

    @PostMapping("/api/absences")
    @ResponseStatus(HttpStatus.CREATED)
    public AbsenceResponse request(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AbsenceRequest request) {
        return absences.request(user.id(), request.date(), request.reason());
    }

    @DeleteMapping("/api/absences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        absences.cancel(user.id(), id);
    }

    /** Admin Inbox. */
    @GetMapping("/api/admin/absences")
    public InboxResponse inbox() {
        return absences.inbox();
    }

    @PostMapping("/api/admin/absences/{id}/decision")
    public AbsenceResponse decide(@AuthenticationPrincipal AuthUser admin, @PathVariable long id,
                                  @Valid @RequestBody DecisionRequest request) {
        return absences.decide(admin.id(), id, request.approve());
    }
}
