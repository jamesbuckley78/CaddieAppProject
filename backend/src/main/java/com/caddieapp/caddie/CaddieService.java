package com.caddieapp.caddie;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.caddie.CaddieDtos.CaddieResponse;
import com.caddieapp.caddie.CaddieDtos.CaddieWithSetupCode;
import com.caddieapp.caddie.CaddieDtos.CreateCaddieRequest;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.common.PhoneNumbers;

@Service
public class CaddieService {

    private final UserRepository users;
    private final CaddieRepository caddies;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public CaddieService(UserRepository users, CaddieRepository caddies, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.caddies = caddies;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Admin creates a caddie account. The caddie logs in with their phone and the returned
     * 6-digit setup code, then must choose their own password.
     */
    @Transactional
    public CaddieWithSetupCode create(CreateCaddieRequest request) {
        String phone = PhoneNumbers.normalise(request.phone());
        if (users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }
        // docs/PLAN.md: "A priority caddie is a single or a double."
        if (request.priority() && request.grade() == Grade.JUNIOR) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Juniors can't be priority caddies");
        }

        String setupCode = newSetupCode();
        User user = users.save(new User(request.name().trim(), phone, Role.CADDIE,
            passwordEncoder.encode(setupCode), true));
        Caddie caddie = caddies.save(new Caddie(user, request.grade(), request.priority()));
        return new CaddieWithSetupCode(CaddieResponse.from(caddie), setupCode);
    }

    @Transactional(readOnly = true)
    public List<CaddieResponse> list() {
        return caddies.findAllWithUser().stream().map(CaddieResponse::from).toList();
    }

    /** Forgotten password: give the caddie a new setup code. Their old password stops working. */
    @Transactional
    public CaddieWithSetupCode resetSetupCode(long caddieId) {
        Caddie caddie = caddies.findById(caddieId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caddie not found"));
        String setupCode = newSetupCode();
        caddie.getUser().resetToSetupCode(passwordEncoder.encode(setupCode));
        return new CaddieWithSetupCode(CaddieResponse.from(caddie), setupCode);
    }

    private String newSetupCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
