package com.caddieapp.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.caddie.User;
import com.caddieapp.caddie.UserRepository;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.common.PhoneNumbers;

/**
 * There's no public sign-up, so somebody has to be the first admin. On start-up, if the
 * database has no admin at all, this creates one from app.bootstrap-admin.* (application.yml).
 * That admin must choose a new password on first login, so the default one can't linger.
 */
@Component
public class BootstrapAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String phone;
    private final String password;

    public BootstrapAdmin(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap-admin.name}") String name,
                          @Value("${app.bootstrap-admin.phone}") String phone,
                          @Value("${app.bootstrap-admin.password}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.phone = phone;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) {
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("No admin account exists and app.bootstrap-admin.password is empty - nobody can log in as admin.");
            return;
        }
        users.save(new User(name, PhoneNumbers.normalise(phone), Role.ADMIN, passwordEncoder.encode(password), true));
        log.info("Created first admin account '{}' (phone {}). It must set a new password on first login.", name, phone);
    }
}
