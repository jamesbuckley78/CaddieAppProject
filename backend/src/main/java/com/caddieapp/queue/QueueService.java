package com.caddieapp.queue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
     * @param bags     bags carried this week (a double counts as 2)
     * @param position tie-breaker: the previous club list position
     */
    public record Candidate(long caddieId, boolean priority, int bags, int position) {
    }

    /**
     * Priority caddies first, then fewest bags, then the previous order. The caddie id is only
     * there so two caddies never compare as equal (positions should already be unique).
     */
    static final Comparator<Candidate> ORDER = Comparator
        .comparing((Candidate c) -> !c.priority()) // false sorts before true, so priority first
        .thenComparingInt(Candidate::bags)
        .thenComparingInt(Candidate::position)
        .thenComparingLong(Candidate::caddieId);

    /** Nightly club list: everyone, in their new order. Position 1 is index 0. */
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
     * Club list position for a caddie being added. Priority caddies always occupy positions
     * 1..priorityCount, so the "top of the group" for non-priority is priorityCount + 1.
     *
     * @param priorityCount how many priority caddies exist already
     * @param lastPosition  the current bottom position (0 if the list is empty)
     */
    public int newCaddiePosition(boolean priority, Placement placement, long priorityCount, int lastPosition) {
        int endOfPriorityGroup = (int) priorityCount + 1;
        if (placement == Placement.BOTTOM_OF_GROUP) {
            return priority ? endOfPriorityGroup : lastPosition + 1;
        }
        return priority ? 1 : endOfPriorityGroup;
    }

    /** Bag counts run Monday to Sunday (docs/RULES.md). */
    public static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
