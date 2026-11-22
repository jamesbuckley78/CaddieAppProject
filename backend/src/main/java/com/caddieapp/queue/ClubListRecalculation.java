package com.caddieapp.queue;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.loop.BagCounts;
import com.caddieapp.queue.QueueService.Candidate;

/**
 * Re-orders the club list once a night (docs/RULES.md). This is the only time positions move
 * automatically, which keeps database writes to a minimum during the day.
 */
@Component
public class ClubListRecalculation {

    private static final Logger log = LoggerFactory.getLogger(ClubListRecalculation.class);

    private final CaddieRepository caddies;
    private final BagCounts bagCounts;
    private final QueueService queueService;
    private final Clock clock;

    public ClubListRecalculation(CaddieRepository caddies, BagCounts bagCounts, QueueService queueService,
                                 Clock clock) {
        this.caddies = caddies;
        this.bagCounts = bagCounts;
        this.queueService = queueService;
        this.clock = clock;
    }

    /**
     * Midnight, club time. Uses the day that just ended, so on Monday 00:00 the final order still
     * counts Sunday's bags; after that the new week starts everyone on 0.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "${app.timezone}")
    public void nightly() {
        recalculate(LocalDate.now(clock).minusDays(1));
    }

    /**
     * Orders everyone by the bags they carried from Monday of `lastDay`'s week up to `lastDay`.
     * Returns how many caddies changed position.
     */
    @Transactional
    public int recalculate(LocalDate lastDay) {
        List<Caddie> all = caddies.findClubList();
        Map<Long, Integer> bags = bagCounts.perCaddie(QueueService.weekStart(lastDay), lastDay);
        Map<Long, Caddie> byId = all.stream().collect(Collectors.toMap(Caddie::getId, Function.identity()));

        List<Candidate> ordered = queueService.clubList(all.stream()
            .map(c -> new Candidate(c.getId(), c.isPriority(), bags.getOrDefault(c.getId(), 0), c.getListPosition()))
            .toList());

        int changed = 0;
        for (int i = 0; i < ordered.size(); i++) {
            Caddie caddie = byId.get(ordered.get(i).caddieId());
            if (caddie.getListPosition() != i + 1) {
                caddie.setListPosition(i + 1); // JPA only writes the rows that actually changed
                changed++;
            }
        }
        log.info("Club list recalculated for week up to {}: {} of {} caddies moved", lastDay, changed, all.size());
        return changed;
    }
}
