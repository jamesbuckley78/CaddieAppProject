package com.caddieapp.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.caddie.User;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.loop.BagCounts;
import com.caddieapp.queue.ClubListSnapshots.Snapshot;

class ClubListRecalculationTest {

    private static final ZoneId DUBLIN = ZoneId.of("Europe/Dublin");
    // Wednesday 25 Nov 2026, 16:00
    private final Clock clock = Clock.fixed(LocalDate.of(2026, 11, 25).atTime(16, 0).atZone(DUBLIN).toInstant(), DUBLIN);

    private final CaddieRepository caddies = mock(CaddieRepository.class);
    private final BagCounts bagCounts = mock(BagCounts.class);
    private final ClubListSnapshots snapshots = mock(ClubListSnapshots.class);
    // A transaction manager that does nothing - enough to run the code inside the template
    private final TransactionTemplate transaction = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final ClubListRecalculation job = new ClubListRecalculation(caddies, bagCounts, snapshots,
        new QueueService(), transaction, clock, "16:00");

    private static Caddie caddie(long id, boolean priority, int position) {
        User user = new User("C" + id, "08600000" + id, Role.CADDIE, "hash", false);
        Caddie caddie = new Caddie(user, Grade.SINGLE, priority, position);
        ReflectionTestUtils.setField(caddie, "id", id);
        return caddie;
    }

    @Test
    void reordersBySnapshotBagsAndSavesTheSnapshot() {
        Caddie a = caddie(1, false, 1);
        Caddie b = caddie(2, false, 2);
        when(caddies.findClubListForUpdate()).thenReturn(List.of(a, b));
        when(bagCounts.maxLoopId()).thenReturn(42L);
        LocalDate wednesday = LocalDate.of(2026, 11, 25);
        when(bagCounts.perCaddie(LocalDate.of(2026, 11, 23), wednesday, 42L)).thenReturn(Map.of(1L, 2));

        int moved = job.recalculate(wednesday);

        // a carried 2 bags, b none: b moves above a
        assertThat(moved).isEqualTo(2);
        assertThat(b.getListPosition()).isEqualTo(1);
        assertThat(a.getListPosition()).isEqualTo(2);
        ArgumentCaptor<Snapshot> saved = ArgumentCaptor.forClass(Snapshot.class);
        verify(snapshots).save(saved.capture());
        assertThat(saved.getValue().countsUpTo()).isEqualTo(wednesday);
        assertThat(saved.getValue().maxLoopId()).isEqualTo(42L);
    }

    @Test
    void startupCatchesUpWhenTodaysUpdateWasMissed() {
        when(snapshots.latest()).thenReturn(Optional.of(
            new Snapshot(LocalDate.of(2026, 11, 24).atTime(16, 0), LocalDate.of(2026, 11, 24), 10)));
        when(caddies.findClubListForUpdate()).thenReturn(List.of());

        job.run(null); // it's 16:00 Wednesday and the last update was Tuesday

        verify(snapshots).save(any(Snapshot.class));
    }

    @Test
    void startupDoesNothingWhenAlreadyUpToDate() {
        when(snapshots.latest()).thenReturn(Optional.of(
            new Snapshot(LocalDate.of(2026, 11, 25).atTime(16, 0), LocalDate.of(2026, 11, 25), 10)));

        job.run(null);

        verify(snapshots, never()).save(any());
        verify(bagCounts, never()).perCaddie(any(), any(), eq(10L));
    }
}
