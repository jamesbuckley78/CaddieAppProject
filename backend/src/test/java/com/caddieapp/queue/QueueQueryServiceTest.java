package com.caddieapp.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.caddieapp.absence.AbsenceRepository;
import com.caddieapp.availability.GeoFence;
import com.caddieapp.availability.QueueEntry;
import com.caddieapp.availability.QueueEntryRepository;
import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.caddie.User;
import com.caddieapp.common.Enums.BagType;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.issue.IssueReports;
import com.caddieapp.loop.BagCounts;
import com.caddieapp.loop.Loop;
import com.caddieapp.loop.LoopRepository;
import com.caddieapp.queue.QueueDtos.BoardEntry;
import com.caddieapp.queue.QueueDtos.BoardResponse;

/**
 * How today's line is put together from the rules, the admin's manual moves and who is out on
 * course (QueueService tests each rule on its own; this checks they're combined right).
 */
class QueueQueryServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneId.of("Europe/Dublin"));
    private final LocalDate today = LocalDate.now(clock);

    private final CaddieRepository caddies = mock(CaddieRepository.class);
    private final QueueEntryRepository entries = mock(QueueEntryRepository.class);
    private final BagCounts bagCounts = mock(BagCounts.class);
    private final LoopRepository loops = mock(LoopRepository.class);
    private final AbsenceRepository absences = mock(AbsenceRepository.class);
    private final IssueReports issues = mock(IssueReports.class);
    private final QueueQueryService service = new QueueQueryService(caddies, entries, bagCounts, new QueueService(),
        mock(GeoFence.class), mock(ClubListSnapshots.class), loops, absences, issues, clock, "16:00");

    private Caddie aoife;

    /**
     * Club list: Aoife 1, Brian 2, Ciaran 3, Junior Dan 4, all on 0 bags except Ciaran (out now).
     * The admin moved Brian to number 1.
     */
    @BeforeEach
    void checkedInToday() {
        aoife = caddie(1, "Aoife", Grade.SINGLE);
        QueueEntry brian = entry(caddie(2, "Brian", Grade.SINGLE));
        brian.moveTo(1);
        QueueEntry ciaran = entry(caddie(3, "Ciaran", Grade.DOUBLE));
        ciaran.call(BagType.DOUBLE, LocalDateTime.now(clock));
        ciaran.sendOut();
        QueueEntry dan = entry(caddie(4, "Dan", Grade.JUNIOR));

        Loop ciaransLoop = new Loop(ciaran.getCaddie(), today, BagType.DOUBLE, null, LocalDateTime.now(clock),
            new User("Admin", "0870000000", Role.ADMIN, "hash", false));
        ReflectionTestUtils.setField(ciaransLoop, "id", 99L);

        when(entries.findDay(today)).thenReturn(List.of(dan, ciaran, brian, entry(aoife)));
        when(bagCounts.perCaddie(QueueService.weekStart(today), today)).thenReturn(Map.of(3L, 2));
        when(bagCounts.total(today)).thenReturn(2);
        when(loops.findByDateAndSentOutAtIsNotNullAndReturnedAtIsNull(today)).thenReturn(List.of(ciaransLoop));
        when(absences.approvedNamesOn(today)).thenReturn(List.of());
        when(caddies.findByUserId(1L)).thenReturn(Optional.of(aoife));
        when(issues.countOpen()).thenReturn(1);
    }

    @Test
    void boardPutsMovedCaddieFirstJuniorsAfterAdultsAndOutOnCourseLastWithoutANumber() {
        BoardResponse board = service.board();

        assertThat(board.entries()).extracting(BoardEntry::name).containsExactly("Brian", "Aoife", "Dan", "Ciaran");
        assertThat(board.entries()).extracting(BoardEntry::lineNumber).containsExactly(1, 2, 3, null);
        assertThat(board.entries().get(0).moved()).isTrue();
        BoardEntry out = board.entries().get(3);
        assertThat(out.currentLoopId()).isEqualTo(99L);
        assertThat(out.bagsThisWeek()).isEqualTo(2);
        assertThat(board.totalBagsToday()).isEqualTo(2);
        assertThat(board.openIssues()).isEqualTo(1);
        // Printed list: Ciaran (out, 2 bags) goes where he'll be when back - after the adults on 0, before juniors
        assertThat(board.entries()).extracting(BoardEntry::name, BoardEntry::printPosition)
            .containsExactly(tuple("Brian", 1), tuple("Aoife", 2), tuple("Dan", 4), tuple("Ciaran", 3));
        assertThat(board.weekDays()).hasSize(5).startsWith(QueueService.weekStart(today));
    }

    @Test
    void caddieSeesTheSameLineNumberAsTheBoardWithoutCountingTheClubTotal() {
        assertThat(service.myDay(1L).lineNumber()).isEqualTo(2);
        verify(bagCounts, never()).total(any());
    }

    private Caddie caddie(long id, String name, Grade grade) {
        User user = new User(name, "08700000" + id, Role.CADDIE, "hash", false);
        ReflectionTestUtils.setField(user, "id", id);
        Caddie caddie = new Caddie(user, grade, false, (int) id);
        ReflectionTestUtils.setField(caddie, "id", id);
        return caddie;
    }

    private QueueEntry entry(Caddie caddie) {
        return new QueueEntry(today, caddie, LocalDateTime.now(clock));
    }
}
