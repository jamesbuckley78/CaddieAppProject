package com.caddieapp.queue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.loop.BagCounts;
import com.caddieapp.queue.ClubListSnapshots.Snapshot;
import com.caddieapp.queue.QueueService.Candidate;

/**
 * Re-orders the club list once a day at app.club-list.daily-update-time (4pm, docs/RULES.md).
 * This is the only time positions move automatically, which keeps database writes to a
 * minimum during the day. Each run saves a snapshot so the club list screen shows exactly the
 * figures the order was worked out from.
 */
@Component
public class ClubListRecalculation implements SchedulingConfigurer, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClubListRecalculation.class);

    private final CaddieRepository caddies;
    private final BagCounts bagCounts;
    private final ClubListSnapshots snapshots;
    private final QueueService queueService;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final LocalTime updateTime;

    public ClubListRecalculation(CaddieRepository caddies, BagCounts bagCounts, ClubListSnapshots snapshots,
                                 QueueService queueService, TransactionTemplate transaction, Clock clock,
                                 @Value("${app.club-list.daily-update-time}") String updateTime) {
        this.caddies = caddies;
        this.bagCounts = bagCounts;
        this.snapshots = snapshots;
        this.queueService = queueService;
        this.transaction = transaction;
        this.clock = clock;
        this.updateTime = LocalTime.parse(updateTime);
    }

    /** Registers the daily job at the configured time, in the club's time zone. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        String cron = String.format("0 %d %d * * *", updateTime.getMinute(), updateTime.getHour());
        registrar.addCronTask(new CronTask(() -> recalculate(LocalDate.now(clock)),
            new CronTrigger(cron, clock.getZone())));
    }

    /**
     * On start-up: if the server was off when the last update was due (or this is the first
     * run), catch up now so the club list isn't stale.
     */
    @Override
    public void run(ApplicationArguments args) {
        LocalDate due = QueueService.clubListCountsUpTo(LocalDateTime.now(clock), updateTime);
        boolean upToDate = snapshots.latest().map(s -> !s.countsUpTo().isBefore(due)).orElse(false);
        if (!upToDate) {
            log.info("Club list update for {} was missed - running it now", due);
            recalculate(due);
        }
    }

    /**
     * Orders everyone by the bags they carried from Monday of `day`'s week up to `day`, counting
     * every loop entered so far, and saves a snapshot. Returns how many caddies moved.
     *
     * Uses TransactionTemplate rather than @Transactional: @Transactional only works when the
     * method is called from another bean, and the scheduled job calls this from inside the class.
     */
    public int recalculate(LocalDate day) {
        Integer moved = transaction.execute(status -> {
            long maxLoopId = bagCounts.maxLoopId();
            List<Caddie> all = caddies.findClubListForUpdate(); // locked: admins' moves wait for this
            Map<Long, Integer> bags = bagCounts.perCaddie(QueueService.weekStart(day), day, maxLoopId);
            Map<Long, Caddie> byId = all.stream().collect(Collectors.toMap(Caddie::getId, Function.identity()));

            List<Candidate> ordered = queueService.clubList(all.stream()
                .map(c -> new Candidate(c.getId(), c.isPriority(), c.getGrade() == Grade.JUNIOR, bags.getOrDefault(c.getId(), 0),
                    c.getListPosition()))
                .toList());

            int changed = 0;
            for (int i = 0; i < ordered.size(); i++) {
                Caddie caddie = byId.get(ordered.get(i).caddieId());
                if (caddie.getListPosition() != i + 1) {
                    caddie.setListPosition(i + 1); // JPA only writes the rows that actually changed
                    changed++;
                }
            }
            snapshots.save(new Snapshot(LocalDateTime.now(clock), day, maxLoopId));
            return changed;
        });
        log.info("Club list updated for {}: {} caddies moved", day, moved);
        return moved == null ? 0 : moved;
    }
}
