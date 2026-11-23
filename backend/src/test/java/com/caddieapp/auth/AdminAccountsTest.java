package com.caddieapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.caddieapp.TestUsers;
import com.caddieapp.audit.AuditLog;
import com.caddieapp.caddie.User;
import com.caddieapp.caddie.UserRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Role;

class AdminAccountsTest {

    private final UserRepository users = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("Europe/Dublin"));
    private final AdminAccounts admins = new AdminAccounts(users,
        new SetupCodes(new BCryptPasswordEncoder(4), clock, 48), mock(AuditLog.class));

    @Test
    void newAdminGetsASetupCodeAndMustChooseAPassword() {
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(u, "id", 5L); // as the database would
            return u;
        });

        var result = admins.create(1, " Mary ", "087 555 1234");

        assertThat(result.setupCode()).matches("\\d{6}");
        assertThat(result.admin().phone()).isEqualTo("0875551234");
        assertThat(result.admin().passwordSet()).isFalse();
    }

    @Test
    void anotherAdminCanBeRemovedButNotYourself() {
        User other = TestUsers.user(2, Role.ADMIN, "hash", false);
        when(users.findById(2L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> admins.remove(2, 2)).isInstanceOf(ApiException.class);
        admins.remove(1, 2);
        assertThat(other.isActive()).isFalse();
    }

    @Test
    void caddieAccountsCantBeManagedHere() {
        when(users.findById(3L)).thenReturn(Optional.of(TestUsers.user(3, Role.CADDIE, "hash", false)));

        assertThatThrownBy(() -> admins.resetSetupCode(1, 3)).isInstanceOf(ApiException.class);
    }
}
