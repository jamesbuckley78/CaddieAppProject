package com.caddieapp.queue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Plain unit tests - no Spring context or database needed, so they run in milliseconds. */
class QueueServiceTest {

    private final QueueService queueService = new QueueService();

    @Test
    void firstCheckInOfTheDayIsPositionOne() {
        assertThat(queueService.positionForNewCheckIn(0)).isEqualTo(1);
    }

    @Test
    void newCheckInGoesToTheEndOfTheList() {
        assertThat(queueService.positionForNewCheckIn(12)).isEqualTo(13);
    }
}
