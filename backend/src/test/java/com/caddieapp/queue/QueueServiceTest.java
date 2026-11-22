package com.caddieapp.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.caddieapp.common.Enums.Placement;
import com.caddieapp.queue.QueueService.Candidate;

/**
 * One test per rule in docs/RULES.md. Plain unit tests - no Spring context or database, so
 * they run in milliseconds.
 */
class QueueServiceTest {

    private final QueueService queueService = new QueueService();

    /** Shorthand: caddie id, priority?, bags this week, previous position. */
    private static Candidate c(long id, boolean priority, int bags, int position) {
        return new Candidate(id, priority, bags, position);
    }

    private static List<Long> ids(List<Candidate> ordered) {
        return ordered.stream().map(Candidate::caddieId).toList();
    }

    @Nested
    class ClubList {

        @Test
        void priorityCaddiesAlwaysComeFirstEvenWithMoreBags() {
            var list = queueService.clubList(List.of(
                c(1, false, 0, 1),
                c(2, true, 4, 2)));

            assertThat(ids(list)).containsExactly(2L, 1L);
        }

        @Test
        void fewestBagsFirstWithinEachGroup() {
            var list = queueService.clubList(List.of(
                c(1, true, 2, 1), c(2, true, 0, 2),
                c(3, false, 3, 3), c(4, false, 1, 4), c(5, false, 0, 5)));

            assertThat(ids(list)).containsExactly(2L, 1L, 5L, 4L, 3L);
        }

        @Test
        void doubleCountsAsTwoSoItRanksBelowASingle() {
            // bag counts already include the double = 2 rule (BagCounts); a single caddie on 1
            // goes above a caddie who carried one double
            var list = queueService.clubList(List.of(c(1, false, 2, 1), c(2, false, 1, 2)));

            assertThat(ids(list)).containsExactly(2L, 1L);
        }

        @Test
        void tiesKeepThePreviousOrderNotAlphabetical() {
            // same bags: the previous position decides, whatever the names are
            var list = queueService.clubList(List.of(c(7, false, 1, 9), c(3, false, 1, 4), c(5, false, 1, 6)));

            assertThat(ids(list)).containsExactly(3L, 5L, 7L);
        }

        @Test
        void caddieWhoStaysHomeKeepsTheirCountAndMovesUpRelativeToWorkers() {
            // Monday: both on 2. Tuesday: A stays home (still 2), B carries 2 more (4).
            var list = queueService.clubList(List.of(c(1, false, 4, 1), c(2, false, 2, 2)));

            assertThat(ids(list)).containsExactly(2L, 1L);
        }

        @Test
        void newWeekWithEveryoneOnZeroKeepsLastWeeksOrder() {
            var list = queueService.clubList(List.of(c(1, false, 0, 3), c(2, false, 0, 1), c(3, false, 0, 2)));

            assertThat(ids(list)).containsExactly(2L, 3L, 1L);
        }
    }

    @Nested
    class TodaysLine {

        @Test
        void someoneWhoJustCarriedABagDropsBelowEveryoneWithFewer() {
            // club list: 1, 2, 3 all on 0. Caddie 1 carries a single today.
            var line = queueService.todaysLine(List.of(c(1, false, 1, 1), c(2, false, 0, 2), c(3, false, 0, 3)));

            assertThat(ids(line)).containsExactly(2L, 3L, 1L);
        }

        @Test
        void lineNumbersSkipCaddiesOutOnCourse() {
            var line = queueService.todaysLine(List.of(c(1, true, 0, 1), c(2, false, 0, 2), c(3, false, 0, 3)));

            Map<Long, Integer> numbers = queueService.lineNumbers(line, cand -> cand.caddieId() == 2L);

            assertThat(numbers).containsExactly(Map.entry(1L, 1), Map.entry(3L, 2));
            assertThat(numbers).doesNotContainKey(2L);
        }
    }

    @Nested
    class NewCaddies {

        @Test
        void newNonPriorityCaddieGoesToTopOfNonPriorityGroup() {
            assertThat(queueService.newCaddiePosition(false, Placement.TOP_OF_GROUP, 50, 120)).isEqualTo(51);
        }

        @Test
        void newPriorityCaddieGoesToTheVeryTop() {
            assertThat(queueService.newCaddiePosition(true, Placement.TOP_OF_GROUP, 50, 120)).isEqualTo(1);
        }

        @Test
        void copyingThePaperListAddsAtTheBottomOfTheGroup() {
            assertThat(queueService.newCaddiePosition(false, Placement.BOTTOM_OF_GROUP, 50, 120)).isEqualTo(121);
            assertThat(queueService.newCaddiePosition(true, Placement.BOTTOM_OF_GROUP, 50, 120)).isEqualTo(51);
        }

        @Test
        void firstCaddieEverIsPositionOne() {
            assertThat(queueService.newCaddiePosition(false, Placement.TOP_OF_GROUP, 0, 0)).isEqualTo(1);
            assertThat(queueService.newCaddiePosition(false, Placement.BOTTOM_OF_GROUP, 0, 0)).isEqualTo(1);
        }
    }

    @Nested
    class Weeks {

        @Test
        void weekStartsOnMonday() {
            LocalDate sunday = LocalDate.of(2026, 11, 22);
            LocalDate monday = LocalDate.of(2026, 11, 23);

            assertThat(QueueService.weekStart(sunday)).isEqualTo(LocalDate.of(2026, 11, 16));
            assertThat(QueueService.weekStart(monday)).isEqualTo(monday);
            assertThat(QueueService.weekStart(LocalDate.of(2026, 11, 25))).isEqualTo(monday);
        }
    }
}
