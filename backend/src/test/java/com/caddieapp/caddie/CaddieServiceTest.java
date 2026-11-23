package com.caddieapp.caddie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.caddieapp.audit.AuditLog;
import com.caddieapp.auth.SetupCodes;
import com.caddieapp.caddie.CaddieDtos.CaddieWithSetupCode;
import com.caddieapp.caddie.CaddieDtos.CreateCaddieRequest;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.Placement;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.queue.QueueService;

class CaddieServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CaddieRepository caddies = mock(CaddieRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AuditLog audit = mock(AuditLog.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("Europe/Dublin"));
    private final CaddieService service = new CaddieService(users, caddies, new SetupCodes(encoder, clock, 48),
        new QueueService(), audit);

    /** Make the mocked save() behave like the database: hand back the entity with an id. */
    private void saveAssignsIds() {
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 10L);
            return u;
        });
        when(caddies.save(any(Caddie.class))).thenAnswer(inv -> {
            Caddie c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 20L);
            return c;
        });
    }

    private static CreateCaddieRequest request(String phone, Grade grade, boolean priority, Placement placement) {
        return new CreateCaddieRequest("A", phone, grade, priority, placement);
    }

    @Test
    void createsCaddieWithSixDigitSetupCodeStoredHashed() {
        saveAssignsIds();

        CaddieWithSetupCode result = service.create(
            new CreateCaddieRequest("  Sean Murphy ", "087 123 4567", Grade.DOUBLE, true, null));

        assertThat(result.setupCode()).matches("\\d{6}");
        assertThat(result.caddie().name()).isEqualTo("Sean Murphy");
        assertThat(result.caddie().phone()).isEqualTo("0871234567");
        assertThat(result.caddie().grade()).isEqualTo(Grade.DOUBLE);
        assertThat(result.caddie().priority()).isTrue();
        assertThat(result.caddie().passwordSet()).isFalse();
    }

    @Test
    void setupCodeIsNotStoredInPlainText() {
        saveAssignsIds();
        CaddieWithSetupCode result = service.create(request("0871234567", Grade.SINGLE, false, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.CADDIE);
        assertThat(saved.getPasswordHash()).isNotEqualTo(result.setupCode());
        assertThat(encoder.matches(result.setupCode(), saved.getPasswordHash())).isTrue();
    }

    /** A list of 120: 50 priority (1-50), 62 other adults (51-112), 8 juniors (113-120). */
    private List<Caddie> listOf120() {
        List<Caddie> list = new java.util.ArrayList<>();
        for (int position = 1; position <= 120; position++) {
            Grade grade = position > 112 ? Grade.JUNIOR : Grade.SINGLE;
            list.add(listed(1000 + position, grade, position <= 50, position));
        }
        when(caddies.findClubListForUpdate()).thenReturn(list);
        return list;
    }

    @Test
    void newCaddieGoesToTopOfNonPriorityGroupAndOthersShiftDown() {
        saveAssignsIds();
        List<Caddie> list = listOf120();

        CaddieWithSetupCode result = service.create(request("0871234567", Grade.SINGLE, false, null));

        assertThat(result.caddie().listPosition()).isEqualTo(51);
        assertThat(list.get(50).getListPosition()).isEqualTo(52); // the old 51 moved down one
        assertThat(list.get(49).getListPosition()).isEqualTo(50); // priority caddies untouched
    }

    @Test
    void paperListAdultGoesAboveTheJuniors() {
        saveAssignsIds();
        List<Caddie> list = listOf120();

        CaddieWithSetupCode result = service.create(
            request("0871234567", Grade.SINGLE, false, Placement.BOTTOM_OF_GROUP));

        assertThat(result.caddie().listPosition()).isEqualTo(113);
        assertThat(list.get(112).getListPosition()).isEqualTo(114); // first junior moved down one
    }

    @Test
    void paperListJuniorAddsAtTheVeryBottomWithoutShifting() {
        saveAssignsIds();
        List<Caddie> list = listOf120();

        CaddieWithSetupCode result = service.create(
            request("0871234567", Grade.JUNIOR, false, Placement.BOTTOM_OF_GROUP));

        assertThat(result.caddie().listPosition()).isEqualTo(121);
        assertThat(list).extracting(Caddie::getListPosition).containsExactlyElementsOf(
            java.util.stream.IntStream.rangeClosed(1, 120).boxed().toList()); // nobody moved
    }

    @Test
    void newJuniorGoesToTopOfJuniorGroup() {
        saveAssignsIds();
        listOf120();

        CaddieWithSetupCode result = service.create(request("0871234567", Grade.JUNIOR, false, null));

        assertThat(result.caddie().listPosition()).isEqualTo(113);
    }

    @Test
    void duplicatePhoneIsRejected() {
        when(users.existsByPhone("0871234567")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("087-123-4567", Grade.SINGLE, false, null)))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(users, never()).save(any());
    }

    @Test
    void juniorCannotBePriority() {
        assertThatThrownBy(() -> service.create(request("0871234567", Grade.JUNIOR, true, null)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Juniors");
    }

    @Test
    void resetGivesNewCodeAndForcesPasswordChange() {
        User user = new User("A", "0871234567", Role.CADDIE, encoder.encode("old-password"), false);
        ReflectionTestUtils.setField(user, "id", 10L);
        Caddie caddie = new Caddie(user, Grade.SINGLE, false, 1);
        ReflectionTestUtils.setField(caddie, "id", 20L);
        when(caddies.findById(20L)).thenReturn(Optional.of(caddie));

        int versionBefore = user.getTokenVersion();

        CaddieWithSetupCode result = service.resetSetupCode(20);

        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(encoder.matches(result.setupCode(), user.getPasswordHash())).isTrue();
        assertThat(encoder.matches("old-password", user.getPasswordHash())).isFalse();
        assertThat(user.getTokenVersion()).isEqualTo(versionBefore + 1); // logged out everywhere
        assertThat(user.getSetupCodeExpiresAt()).isEqualTo(LocalDateTime.now(clock).plusHours(48));
    }

    @Test
    void newCaddieSetupCodeExpiresIn48Hours() {
        saveAssignsIds();

        service.create(request("0871234567", Grade.SINGLE, false, null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getSetupCodeExpiresAt()).isEqualTo(LocalDateTime.now(clock).plusHours(48));
    }

    // ---- Moving on the club list ----

    private static Caddie listed(long id, Grade grade, boolean priority, int position) {
        User user = new User("C" + id, "08600000" + id, Role.CADDIE, "hash", false);
        ReflectionTestUtils.setField(user, "id", 100 + id);
        Caddie caddie = new Caddie(user, grade, priority, position);
        ReflectionTestUtils.setField(caddie, "id", id);
        return caddie;
    }

    @Test
    void moveWithinGroupShiftsOthersAndIsAudited() {
        Caddie p1 = listed(1, Grade.SINGLE, true, 1);
        Caddie a1 = listed(2, Grade.SINGLE, false, 2);
        Caddie a2 = listed(3, Grade.DOUBLE, false, 3);
        Caddie a3 = listed(4, Grade.SINGLE, false, 4);
        Caddie j1 = listed(5, Grade.JUNIOR, false, 5);
        when(caddies.findClubListForUpdate()).thenReturn(List.of(p1, a1, a2, a3, j1));

        service.moveInClubList(99, 4, 2); // a3 from 4 up to 2

        assertThat(List.of(p1, a3, a1, a2, j1)).extracting(Caddie::getListPosition).containsExactly(1, 2, 3, 4, 5);
        verify(audit).record(org.mockito.ArgumentMatchers.eq(99L), org.mockito.ArgumentMatchers.eq("MOVE_CLUB_LIST"),
            any(), any());
    }

    @Test
    void cannotMoveOutOfTheirGroup() {
        Caddie p1 = listed(1, Grade.SINGLE, true, 1);
        Caddie a1 = listed(2, Grade.SINGLE, false, 2);
        Caddie j1 = listed(3, Grade.JUNIOR, false, 3);
        when(caddies.findClubListForUpdate()).thenReturn(List.of(p1, a1, j1));

        assertThatThrownBy(() -> service.moveInClubList(99, 3, 2))
            .isInstanceOf(ApiException.class).hasMessageContaining("juniors");
        assertThatThrownBy(() -> service.moveInClubList(99, 2, 1))
            .isInstanceOf(ApiException.class).hasMessageContaining("between 2 and 2");
    }

    // ---- Editing, promote / demote, leaving ----

    private static CaddieDtos.EditCaddieRequest edit(Caddie c, Grade grade, boolean priority) {
        return new CaddieDtos.EditCaddieRequest(c.getUser().getName(), c.getUser().getPhone(), grade, priority);
    }

    @Test
    void promotedCaddieGoesToTopOfPriorityGroupAndIsAudited() {
        Caddie p1 = listed(1, Grade.SINGLE, true, 1);
        Caddie a1 = listed(2, Grade.SINGLE, false, 2);
        Caddie a2 = listed(3, Grade.DOUBLE, false, 3);
        Caddie j1 = listed(4, Grade.JUNIOR, false, 4);
        when(caddies.findById(3L)).thenReturn(Optional.of(a2));
        when(caddies.findClubListForUpdate()).thenReturn(List.of(p1, a1, a2, j1));

        service.edit(99, 3, edit(a2, Grade.DOUBLE, true));

        assertThat(List.of(a2, p1, a1, j1)).extracting(Caddie::getListPosition).containsExactly(1, 2, 3, 4);
        verify(audit).record(org.mockito.ArgumentMatchers.eq(99L), org.mockito.ArgumentMatchers.eq("EDIT_CADDIE"),
            any(), any());
    }

    @Test
    void juniorBecomingAnAdultGoesToTopOfNonPriorityAdults() {
        Caddie p1 = listed(1, Grade.SINGLE, true, 1);
        Caddie a1 = listed(2, Grade.SINGLE, false, 2);
        Caddie j1 = listed(3, Grade.JUNIOR, false, 3);
        Caddie j2 = listed(4, Grade.JUNIOR, false, 4);
        when(caddies.findById(4L)).thenReturn(Optional.of(j2));
        when(caddies.findClubListForUpdate()).thenReturn(List.of(p1, a1, j1, j2));

        service.edit(99, 4, edit(j2, Grade.SINGLE, false));

        assertThat(List.of(p1, j2, a1, j1)).extracting(Caddie::getListPosition).containsExactly(1, 2, 3, 4);
    }

    @Test
    void renameWithoutChangingGroupKeepsTheirPlace() {
        Caddie p1 = listed(1, Grade.SINGLE, true, 1);
        Caddie a1 = listed(2, Grade.SINGLE, false, 2);
        when(caddies.findById(2L)).thenReturn(Optional.of(a1));

        service.edit(99, 2, new CaddieDtos.EditCaddieRequest("New Name", "086 000 0002", Grade.DOUBLE, false));

        assertThat(a1.getUser().getName()).isEqualTo("New Name");
        assertThat(a1.getListPosition()).isEqualTo(2);
        verify(caddies, never()).findClubList();
    }

    @Test
    void leaverIsTakenOffTheListAndLoggedOut() {
        Caddie a1 = listed(1, Grade.SINGLE, false, 1);
        Caddie a2 = listed(2, Grade.SINGLE, false, 2);
        Caddie a3 = listed(3, Grade.SINGLE, false, 3);
        int tokenVersion = a2.getUser().getTokenVersion();
        when(caddies.findById(2L)).thenReturn(Optional.of(a2));
        when(caddies.findClubListForUpdate()).thenReturn(List.of(a1, a2, a3));

        service.markLeft(99, 2);

        assertThat(a2.getUser().isActive()).isFalse();
        assertThat(a2.getUser().getTokenVersion()).isEqualTo(tokenVersion + 1);
        assertThat(a2.getListPosition()).isZero();
        assertThat(List.of(a1, a3)).extracting(Caddie::getListPosition).containsExactly(1, 2);
    }

    @Test
    void caddieBroughtBackGoesToTopOfTheirGroup() {
        Caddie p1 = listed(1, Grade.SINGLE, true, 1);
        Caddie a1 = listed(2, Grade.SINGLE, false, 2);
        Caddie back = listed(3, Grade.DOUBLE, false, 0);
        back.getUser().markLeft();
        when(caddies.findById(3L)).thenReturn(Optional.of(back));
        when(caddies.findClubListForUpdate()).thenReturn(List.of(p1, a1));

        service.bringBack(99, 3);

        assertThat(back.getUser().isActive()).isTrue();
        assertThat(List.of(p1, back, a1)).extracting(Caddie::getListPosition).containsExactly(1, 2, 3);
    }
}
