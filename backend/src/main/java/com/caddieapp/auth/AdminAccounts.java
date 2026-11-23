package com.caddieapp.auth;

import static com.caddieapp.audit.AuditLog.state;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.audit.AuditLog;
import com.caddieapp.caddie.User;
import com.caddieapp.caddie.UserRepository;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.common.PhoneNumbers;

/**
 * The caddie masters' own accounts (docs/PLAN.md: up to 5 admins). Any admin can add another,
 * give one a new setup code (forgotten password) or remove one who has left. Works like caddie
 * accounts: a 6-digit setup code, then they choose a password. Every change is audit logged.
 */
@Service
public class AdminAccounts {

    public record AdminResponse(long id, String name, String phone, boolean passwordSet) {

        static AdminResponse from(User user) {
            return new AdminResponse(user.getId(), user.getName(), user.getPhone(), !user.isMustChangePassword());
        }
    }

    public record AdminWithSetupCode(AdminResponse admin, String setupCode) {
    }

    private final UserRepository users;
    private final SetupCodes setupCodes;
    private final AuditLog audit;

    public AdminAccounts(UserRepository users, SetupCodes setupCodes, AuditLog audit) {
        this.users = users;
        this.setupCodes = setupCodes;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> list() {
        return users.findByRoleAndActiveTrueOrderByName(Role.ADMIN).stream().map(AdminResponse::from).toList();
    }

    @Transactional
    public AdminWithSetupCode create(long adminId, String name, String rawPhone) {
        String phone = PhoneNumbers.normalise(rawPhone);
        if (users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }
        User user = new User(name.trim(), phone, Role.ADMIN, null, true);
        String setupCode = setupCodes.issue(user);
        user = users.save(user);
        audit.record(adminId, "ADD_ADMIN", null, state("admin", user.getName(), "phone", phone));
        return new AdminWithSetupCode(AdminResponse.from(user), setupCode);
    }

    /** Forgotten password: a new setup code. Their old password and logins stop working. */
    @Transactional
    public AdminWithSetupCode resetSetupCode(long adminId, long id) {
        User user = find(id);
        String setupCode = setupCodes.issue(user);
        audit.record(adminId, "RESET_ADMIN_CODE", state("admin", user.getName()), null);
        return new AdminWithSetupCode(AdminResponse.from(user), setupCode);
    }

    /** They've left: logged out everywhere and can't log in. Not yourself, so one admin always remains. */
    @Transactional
    public void remove(long adminId, long id) {
        if (adminId == id) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can't remove your own account");
        }
        User user = find(id);
        user.markLeft();
        audit.record(adminId, "REMOVE_ADMIN", state("admin", user.getName()), state("active", false));
    }

    private User find(long id) {
        return users.findById(id)
            .filter(u -> u.getRole() == Role.ADMIN && u.isActive())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin not found"));
    }
}
