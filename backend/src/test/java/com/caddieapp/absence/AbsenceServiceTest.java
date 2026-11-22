package com.caddieapp.absence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** "At least 24 hours ahead" = the start of the chosen day must be 24+ hours from now. */
class AbsenceServiceTest {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 11, 25);

    @Test
    void wednesdayMorningTheEarliestIsFriday() {
        // Thursday starts in ~14 hours - too soon
        assertThat(AbsenceService.earliestDate(WEDNESDAY.atTime(10, 0), 24)).isEqualTo(WEDNESDAY.plusDays(2));
    }

    @Test
    void exactlyMidnightThursdayIsJustEnough() {
        assertThat(AbsenceService.earliestDate(WEDNESDAY.atStartOfDay(), 24)).isEqualTo(WEDNESDAY.plusDays(1));
    }

    @Test
    void oneMinutePastMidnightIsTooLateForTomorrow() {
        assertThat(AbsenceService.earliestDate(WEDNESDAY.atTime(0, 1), 24)).isEqualTo(WEDNESDAY.plusDays(2));
    }

    @Test
    void noticeIsConfigurable() {
        LocalDateTime wednesdayNoon = WEDNESDAY.atTime(12, 0);

        assertThat(AbsenceService.earliestDate(wednesdayNoon, 0)).isEqualTo(WEDNESDAY.plusDays(1));
        assertThat(AbsenceService.earliestDate(wednesdayNoon, 48)).isEqualTo(WEDNESDAY.plusDays(3));
    }
}
