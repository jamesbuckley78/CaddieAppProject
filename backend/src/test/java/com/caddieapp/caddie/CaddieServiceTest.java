package com.caddieapp.caddie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.caddieapp.caddie.CaddieDtos.CaddieWithSetupCode;
import com.caddieapp.caddie.CaddieDtos.CreateCaddieRequest;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.Role;

class CaddieServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CaddieRepository caddies = mock(CaddieRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final CaddieService service = new CaddieService(users, caddies, encoder);

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

    @Test
    void createsCaddieWithSixDigitSetupCodeStoredHashed() {
        saveAssignsIds();

        CaddieWithSetupCode result = service.create(
            new CreateCaddieRequest("  Sean Murphy ", "087 123 4567", Grade.DOUBLE, true));

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
        CaddieWithSetupCode result = service.create(new CreateCaddieRequest("A", "0871234567", Grade.SINGLE, false));

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.CADDIE);
        assertThat(saved.getPasswordHash()).isNotEqualTo(result.setupCode());
        assertThat(encoder.matches(result.setupCode(), saved.getPasswordHash())).isTrue();
    }

    @Test
    void duplicatePhoneIsRejected() {
        when(users.existsByPhone("0871234567")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCaddieRequest("A", "087-123-4567", Grade.SINGLE, false)))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(users, never()).save(any());
    }

    @Test
    void juniorCannotBePriority() {
        assertThatThrownBy(() -> service.create(new CreateCaddieRequest("A", "0871234567", Grade.JUNIOR, true)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Juniors");
    }

    @Test
    void resetGivesNewCodeAndForcesPasswordChange() {
        User user = new User("A", "0871234567", Role.CADDIE, encoder.encode("old-password"), false);
        ReflectionTestUtils.setField(user, "id", 10L);
        Caddie caddie = new Caddie(user, Grade.SINGLE, false);
        ReflectionTestUtils.setField(caddie, "id", 20L);
        when(caddies.findById(20L)).thenReturn(Optional.of(caddie));

        CaddieWithSetupCode result = service.resetSetupCode(20);

        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(encoder.matches(result.setupCode(), user.getPasswordHash())).isTrue();
        assertThat(encoder.matches("old-password", user.getPasswordHash())).isFalse();
    }
}
