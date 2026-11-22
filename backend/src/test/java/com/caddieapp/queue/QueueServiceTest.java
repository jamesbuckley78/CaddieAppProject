package com.caddieapp.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
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

    /** Shorthand for an adult caddie: id, priority?, bags this week, previous position. */
    private static Candidate c(long id, boolean priority, int bags, int position) {
        return new Candidate(id, priority, false, bags, position);
    }

    /** Shorthand for a junior (never priority): id, bags this week, previous position. */
    private static Candidate junior(long id, int bags, int position) {
        return new Candidate(id, false, true, bags, position);
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

        @Test
        void juniorsStayAtTheBottomEvenWithFewerBags() {
            // junior on 0 bags and high on the club list still goes after an adult on 5 bags
            var line = queueService.todaysLine(List.of(junior(9, 0, 1), c(1, false, 5, 2), c(2, true, 3, 3)));

            assertThat(ids(line)).containsExactly(2L, 1L, 9L);
        }

        @Test
        void juniorsAreOrderedAmongThemselvesByTheNormalRules() {
            var line = queueService.todaysLine(List.of(
                junior(7, 2, 4), junior(8, 0, 6), junior(9, 0, 5), c(1, false, 4, 9)));

            assertThat(ids(line)).containsExactly(1L, 9L, 8L, 7L);
        }

        @Test
        void juniorsAreNumberedAfterEveryAdult() {
            var line = queueService.todaysLine(List.of(junior(9, 0, 1), c(1, false, 2, 2), c(2, false, 1, 3)));

            assertThat(queueService.lineNumbers(line, cand -> false)).containsEntry(9L, 3);
        }
    }

    @Nested
    class JuniorsOnTheClubList {

        @Test
        void juniorsStayGroupedAtTheBottomOfTheClubList() {
            // junior on 0 bags still goes below an adult on 6
            var list = queueService.clubList(List.of(junior(9, 0, 1), c(1, false, 6, 2), c(2, true, 2, 3)));

            assertThat(ids(list)).containsExactly(2L, 1L, 9L);
        }

        @Test
        void juniorsOnlyMoveAroundEachOther() {
            // club list 90-93 are juniors; the one with fewest bags moves to the top of the group
            var list = queueService.clubList(List.of(
                c(1, false, 9, 89), junior(90, 3, 90), junior(91, 2, 91), junior(92, 0, 92), junior(93, 2, 93)));

            assertThat(ids(list)).containsExactly(1L, 92L, 91L, 93L, 90L);
        }
    }

    @Nested
    class NewCaddies {

        // A list of 120: 50 priority (1-50), 62 other adults (51-112), 8 juniors (113-120)
        private int place(boolean priority, boolean junior, Placement placement) {
            return queueService.newCaddiePosition(priority, junior, placement, 50, 112, 120);
        }

        @Test
        void newPriorityCaddieGoesToTheVeryTop() {
            assertThat(place(true, false, Placement.TOP_OF_GROUP)).isEqualTo(1);
        }

        @Test
        void newAdultGoesToTopOfNonPriorityGroup() {
            assertThat(place(false, false, Placement.TOP_OF_GROUP)).isEqualTo(51);
        }

        @Test
        void newJuniorGoesToTopOfJuniorGroup() {
            assertThat(place(false, true, Placement.TOP_OF_GROUP)).isEqualTo(113);
        }

        @Test
        void copyingThePaperListAddsAtTheBottomOfEachGroup() {
            assertThat(place(true, false, Placement.BOTTOM_OF_GROUP)).isEqualTo(51);
            assertThat(place(false, false, Placement.BOTTOM_OF_GROUP)).isEqualTo(113); // still above juniors
            assertThat(place(false, true, Placement.BOTTOM_OF_GROUP)).isEqualTo(121);
        }

        @Test
        void firstCaddieEverIsPositionOne() {
            assertThat(queueService.newCaddiePosition(false, false, Placement.TOP_OF_GROUP, 0, 0, 0)).isEqualTo(1);
            assertThat(queueService.newCaddiePosition(false, true, Placement.BOTTOM_OF_GROUP, 0, 0, 0)).isEqualTo(1);
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

    @Nested
    class DailyUpdateAt4pm {

        private final LocalTime fourPm = LocalTime.of(16, 0);
        private final LocalDate wednesday = LocalDate.of(2026, 11, 25);

        @Test
        void beforeFourPmShowsUpToYesterday() {
            assertThat(QueueService.clubListCountsUpTo(wednesday.atTime(15, 59), fourPm))
                .isEqualTo(wednesday.minusDays(1));
        }

        @Test
        void fromFourPmIncludesToday() {
            assertThat(QueueService.clubListCountsUpTo(wednesday.atTime(16, 0), fourPm)).isEqualTo(wednesday);
            assertThat(QueueService.clubListCountsUpTo(wednesday.atTime(23, 30), fourPm)).isEqualTo(wednesday);
        }

        @Test
        void mondayMorningStillShowsLastWeekUntilFourPm() {
            LocalDate monday = LocalDate.of(2026, 11, 23);
            LocalDate shown = QueueService.clubListCountsUpTo(monday.atTime(9, 0), fourPm);

            assertThat(shown).isEqualTo(monday.minusDays(1)); // Sunday
            assertThat(QueueService.weekStart(shown)).isEqualTo(LocalDate.of(2026, 11, 16));
        }
    }
}
