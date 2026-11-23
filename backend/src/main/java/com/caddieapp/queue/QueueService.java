package com.caddieapp.queue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import com.caddieapp.common.Enums.Placement;

/**
 * The only place list-ordering rules live (docs/RULES.md). Pure logic: no database, no
 * clock, so every rule is easy to unit test in QueueServiceTest.
 */
@Service
public class QueueService {

    /**
     * One caddie as far as ordering is concerned.
     *
     * @param junior   junior grade - they go after every adult in today's line
     * @param bags     bags carried this week (a double counts as 2)
     * @param position tie-breaker: the previous club list position
     */
    public record Candidate(long caddieId, boolean priority, boolean junior, int bags, int position) {
    }

    /**
     * Three groups, in this order: priority caddies, other adults, juniors (juniors are never
     * priority). Within a group: fewest bags, then the previous order. So juniors stay together
     * at the bottom and only move around each other. The caddie id is only there so two
     * caddies never compare as equal (positions should already be unique).
     */
    static final Comparator<Candidate> ORDER = Comparator
        .comparing(Candidate::junior) // false (adult) sorts before true (junior)
        .thenComparing((Candidate c) -> !c.priority()) // then priority before non-priority
        .thenComparingInt(Candidate::bags)
        .thenComparingInt(Candidate::position)
        .thenComparingLong(Candidate::caddieId);

    /** Daily club list: everyone, in their new order. Position 1 is index 0. */
    public List<Candidate> clubList(Collection<Candidate> allCaddies) {
        return allCaddies.stream().sorted(ORDER).toList();
    }

    /**
     * Today's line: the caddies who checked in today, with `bags` including today's bags and
     * `position` = their club list position. Same rule as the club list, applied live.
     */
    public List<Candidate> todaysLine(Collection<Candidate> checkedIn) {
        return checkedIn.stream().sorted(ORDER).toList();
    }

    /**
     * Applies the admin's manual moves to a line that's already in rule order: everyone else
     * keeps their relative order, and each moved caddie is slotted in at their chosen line
     * number (or the end, if the line is now shorter). A move holds until the caddie's next bag.
     *
     * @param manual caddie id -> line number the admin moved them to
     */
    public List<Candidate> applyManualMoves(List<Candidate> line, Map<Long, Integer> manual) {
        if (manual.isEmpty()) {
            return line;
        }
        List<Candidate> result = new ArrayList<>(line.stream().filter(c -> !manual.containsKey(c.caddieId())).toList());
        line.stream()
            .filter(c -> manual.containsKey(c.caddieId()))
            .sorted(Comparator.comparingInt((Candidate c) -> manual.get(c.caddieId())))
            .forEach(c -> {
                int index = Math.max(0, Math.min(manual.get(c.caddieId()) - 1, result.size()));
                result.add(index, c);
            });
        return result;
    }

    /**
     * Everyone in one list, for the printed list (docs/RULES.md, "Printed list"): the waiting line
     * as it stands (manual moves included), with each caddie out on course slotted in where the
     * rules expect them once they're back - just before the first waiting caddie the rules put
     * after them. Their `bags` already include the bag they're carrying.
     */
    public List<Candidate> withOutOnCourse(List<Candidate> waitingLine, Collection<Candidate> outOnCourse) {
        List<Candidate> result = new ArrayList<>(waitingLine);
        outOnCourse.stream().sorted(ORDER).forEach(out -> {
            int index = 0;
            while (index < result.size() && ORDER.compare(result.get(index), out) < 0) {
                index++;
            }
            result.add(index, out);
        });
        return result;
    }

    /**
     * Numbers the people in line 1, 2, 3... skipping anyone `notInLine` says to skip (out on
     * course). Skipped caddies get no number. Keeps the order of `line`.
     */
    public Map<Long, Integer> lineNumbers(List<Candidate> line, Predicate<Candidate> notInLine) {
        Map<Long, Integer> numbers = new LinkedHashMap<>();
        int next = 1;
        for (Candidate candidate : line) {
            if (!notInLine.test(candidate)) {
                numbers.put(candidate.caddieId(), next++);
            }
        }
        return numbers;
    }

    /**
     * Club list position for a caddie being added, at the top or bottom of their group. The
     * groups always sit in order: priority (1..priorityCount), other adults (..adultCount),
     * juniors (..lastPosition).
     *
     * @param priorityCount how many priority caddies exist already
     * @param adultCount    how many non-junior caddies exist already (priority included)
     * @param lastPosition  the current bottom position (0 if the list is empty)
     */
    public int newCaddiePosition(boolean priority, boolean junior, Placement placement,
                                 long priorityCount, long adultCount, int lastPosition) {
        int topOfAdults = (int) priorityCount + 1;
        int topOfJuniors = (int) adultCount + 1;
        boolean bottom = placement == Placement.BOTTOM_OF_GROUP;
        if (junior) {
            return bottom ? lastPosition + 1 : topOfJuniors;
        }
        if (priority) {
            return bottom ? topOfAdults : 1;
        }
        return bottom ? topOfJuniors : topOfAdults;
    }

    /**
     * The club list updates once a day at `updateTime` (4pm): before then it shows up to
     * yesterday, from then on it includes today (docs/RULES.md, "Club list screen").
     */
    public static LocalDate clubListCountsUpTo(LocalDateTime now, LocalTime updateTime) {
        LocalDate today = now.toLocalDate();
        return now.toLocalTime().isBefore(updateTime) ? today.minusDays(1) : today;
    }

    /** Bag counts run Monday to Sunday (docs/RULES.md). */
    public static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
