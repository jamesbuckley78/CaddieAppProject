package com.caddieapp.availability;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.auth.AuthUser;
import com.caddieapp.common.Enums.QueueStatus;
import com.caddieapp.queue.QueueDtos.MyDayResponse;
import com.caddieapp.queue.QueueQueryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Caddie-only (SecurityConfig). Every call returns the caddie's updated day. */
@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    /** Latitude/longitude are null if the phone couldn't get a position. */
    public record CheckInRequest(Double latitude, Double longitude) {
    }

    public record StatusRequest(
        @NotNull QueueStatus status,
        @Min(value = 5, message = "must be at least 5 minutes")
        @Max(value = 240, message = "must be at most 4 hours") Integer backInMinutes) {
    }

    private final AvailabilityService availability;
    private final QueueQueryService queries;

    public AvailabilityController(AvailabilityService availability, QueueQueryService queries) {
        this.availability = availability;
        this.queries = queries;
    }

    @PostMapping("/check-in")
    public MyDayResponse checkIn(@AuthenticationPrincipal AuthUser user, @RequestBody CheckInRequest request) {
        availability.checkIn(user.id(), request.latitude(), request.longitude());
        return queries.myDay(user.id());
    }

    @PostMapping("/status")
    public MyDayResponse status(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody StatusRequest request) {
        availability.changeStatus(user.id(), request.status(), request.backInMinutes());
        return queries.myDay(user.id());
    }

    @PostMapping("/check-out")
    public MyDayResponse checkOut(@AuthenticationPrincipal AuthUser user) {
        availability.checkOut(user.id());
        return queries.myDay(user.id());
    }
}
