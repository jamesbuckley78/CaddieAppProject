package com.caddieapp.caddie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
    private final CaddieService service = new CaddieService(users, caddies, encoder, new QueueService());

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

    /** A list of 120: 50 priority, 62 other adults, 8 juniors (113-120). */
    private void listOf120() {
        when(caddies.countByPriorityTrue()).thenReturn(50L);
        when(caddies.countByGradeNot(Grade.JUNIOR)).thenReturn(112L);
        when(caddies.findMaxListPosition()).thenReturn(120);
    }

    @Test
    void newCaddieGoesToTopOfNonPriorityGroupAndOthersShiftDown() {
        saveAssignsIds();
        listOf120();

        CaddieWithSetupCode result = service.create(request("0871234567", Grade.SINGLE, false, null));

        assertThat(result.caddie().listPosition()).isEqualTo(51);
        verify(caddies).shiftDownFrom(51);
    }

    @Test
    void paperListAdultGoesAboveTheJuniors() {
        saveAssignsIds();
        listOf120();

        CaddieWithSetupCode result = service.create(
            request("0871234567", Grade.SINGLE, false, Placement.BOTTOM_OF_GROUP));

        assertThat(result.caddie().listPosition()).isEqualTo(113);
        verify(caddies).shiftDownFrom(113);
    }

    @Test
    void paperListJuniorAddsAtTheVeryBottomWithoutShifting() {
        saveAssignsIds();
        listOf120();

        CaddieWithSetupCode result = service.create(
            request("0871234567", Grade.JUNIOR, false, Placement.BOTTOM_OF_GROUP));

        assertThat(result.caddie().listPosition()).isEqualTo(121);
        verify(caddies, never()).shiftDownFrom(anyInt());
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

        CaddieWithSetupCode result = service.resetSetupCode(20);

        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(encoder.matches(result.setupCode(), user.getPasswordHash())).isTrue();
        assertThat(encoder.matches("old-password", user.getPasswordHash())).isFalse();
    }
}
