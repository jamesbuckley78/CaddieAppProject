package com.caddieapp.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.caddieapp.audit.AuditLog;
import com.caddieapp.availability.QueueEntry;
import com.caddieapp.availability.QueueEntryRepository;
import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.User;
import com.caddieapp.caddie.UserRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.BagType;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.QueueStatus;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.notification.NotificationService;

/** The board's buttons: call, sent out, no reply, back in, move. Database mocked. */
class AssignmentServiceTest {

    private static final ZoneId DUBLIN = ZoneId.of("Europe/Dublin");
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), DUBLIN);
    private final LocalDate today = LocalDate.now(clock);

    private final QueueEntryRepository entries = mock(QueueEntryRepository.class);
    private final LoopRepository loops = mock(LoopRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AuditLog audit = mock(AuditLog.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final AssignmentService service = new AssignmentService(entries, loops, users, audit, notifications, clock);

    private QueueEntry entry;

    @BeforeEach
    void caddieCheckedInToday() {
        User user = new User("Sean Murphy", "0871234567", Role.CADDIE, "hash", false);
        Caddie caddie = new Caddie(user, Grade.SINGLE, false, 5);
        ReflectionTestUtils.setField(caddie, "id", 7L);
        entry = new QueueEntry(today, caddie, LocalDateTime.now(clock));
        when(entries.findForUpdate(today, 7L)).thenReturn(Optional.of(entry));
        when(loops.save(any(Loop.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void callingMarksThemCalledNotifiesAndAudits() {
        service.call(1, 7, BagType.DOUBLE);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.CALLED);
        assertThat(entry.getCalledBagType()).isEqualTo(BagType.DOUBLE);
        verify(notifications).caddieCalled(entry.getCaddie().getUser(), BagType.DOUBLE);
        verify(audit).record(eq(1L), eq("CALL"), any(), any());
        verify(loops, never()).save(any()); // no bag until they're actually sent out
    }

    @Test
    void canCallSomeoneGettingFoodButNotSomeoneAlreadyCalled() {
        entry.changeStatus(QueueStatus.FOOD, null);
        service.call(1, 7, BagType.SINGLE);

        assertThatThrownBy(() -> service.call(2, 7, BagType.SINGLE))
            .isInstanceOf(ApiException.class).hasMessageContaining("already called");
    }

    @Test
    void sendingOutRecordsTheBagAndClearsAManualMove() {
        entry.moveTo(2);
        service.call(1, 7, BagType.DOUBLE);

        service.sendOut(1, 7);

        ArgumentCaptor<Loop> loop = ArgumentCaptor.forClass(Loop.class);
        verify(loops).save(loop.capture());
        assertThat(loop.getValue().getBagType()).isEqualTo(BagType.DOUBLE);
        assertThat(entry.getStatus()).isEqualTo(QueueStatus.OUT);
        assertThat(entry.getManualPosition()).isNull(); // a move holds until their next bag
    }

    @Test
    void cannotSendOutSomeoneWhoWasntCalled() {
        assertThatThrownBy(() -> service.sendOut(1, 7)).isInstanceOf(ApiException.class);
    }

    @Test
    void noReplyKeepsTheirPlaceAndCountsAMissedCall() {
        entry.moveTo(3);
        service.call(1, 7, BagType.SINGLE);

        service.noReply(1, 7);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.AVAILABLE);
        assertThat(entry.getMissedCalls()).isEqualTo(1);
        assertThat(entry.getManualPosition()).isEqualTo(3); // still where they were
        verify(loops, never()).save(any());
    }

    @Test
    void backInMakesThemAvailableAndRecordsTheReturnTime() {
        service.call(1, 7, BagType.SINGLE);
        service.sendOut(1, 7);
        Loop out = new Loop(entry.getCaddie(), today, BagType.SINGLE, null, null, null);
        when(loops.findFirstByCaddieIdAndDateAndReturnedAtIsNullOrderByIdDesc(7L, today)).thenReturn(Optional.of(out));

        service.backIn(null, 7); // the caddie themselves

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.AVAILABLE);
        assertThat(out.getReturnedAt()).isEqualTo(LocalDateTime.now(clock));
        verify(audit, never()).record(anyLong(), eq("BACK_IN"), any(), any()); // only admin actions are audited
    }

    @Test
    void changingBagTypeIsAudited() {
        Loop loop = new Loop(entry.getCaddie(), today, BagType.SINGLE, null, null, null);
        when(loops.findById(40L)).thenReturn(Optional.of(loop));

        service.changeBagType(1, 40, BagType.DOUBLE);

        assertThat(loop.getBagType()).isEqualTo(BagType.DOUBLE);
        verify(audit).record(eq(1L), eq("EDIT_BAG"), any(), any());
    }

    @Test
    void moveCannotBeUsedOnSomeoneOutOnCourse() {
        service.call(1, 7, BagType.SINGLE);
        service.sendOut(1, 7);

        assertThatThrownBy(() -> service.moveInLine(1, 7, 1, null)).isInstanceOf(ApiException.class);
    }

    @Test
    void caddieNotCheckedInCantBeCalled() {
        when(entries.findForUpdate(today, 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.call(1, 8, BagType.SINGLE))
            .isInstanceOf(ApiException.class).hasMessageContaining("isn't checked in");
    }
}
