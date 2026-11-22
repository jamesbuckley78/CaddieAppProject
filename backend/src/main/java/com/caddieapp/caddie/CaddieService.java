package com.caddieapp.caddie;

import static com.caddieapp.audit.AuditLog.state;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.audit.AuditLog;
import com.caddieapp.caddie.CaddieDtos.CaddieResponse;
import com.caddieapp.caddie.CaddieDtos.CaddieWithSetupCode;
import com.caddieapp.caddie.CaddieDtos.CreateCaddieRequest;
import com.caddieapp.common.ApiException;
import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.Placement;
import com.caddieapp.common.Enums.Role;
import com.caddieapp.common.PhoneNumbers;
import com.caddieapp.queue.QueueService;

@Service
public class CaddieService {

    private final UserRepository users;
    private final CaddieRepository caddies;
    private final PasswordEncoder passwordEncoder;
    private final QueueService queueService;
    private final AuditLog audit;
    private final Clock clock;
    private final long setupCodeHours;
    private final SecureRandom random = new SecureRandom();

    public CaddieService(UserRepository users, CaddieRepository caddies, PasswordEncoder passwordEncoder,
                         QueueService queueService, AuditLog audit, Clock clock,
                         @Value("${app.setup-code-valid-hours}") long setupCodeHours) {
        this.users = users;
        this.caddies = caddies;
        this.passwordEncoder = passwordEncoder;
        this.queueService = queueService;
        this.audit = audit;
        this.clock = clock;
        this.setupCodeHours = setupCodeHours;
    }

    /**
     * Admin creates a caddie account. The caddie logs in with their phone and the returned
     * 6-digit setup code (valid for 48 hours), then must choose their own password.
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

        // docs/RULES.md: a new caddie goes to the top of their group with 0 bags, unless the
        // admin is copying the paper list in order (bottom of group).
        Placement placement = request.placement() == null ? Placement.TOP_OF_GROUP : request.placement();
        int lastPosition = caddies.findMaxListPosition();
        int position = queueService.newCaddiePosition(request.priority(), request.grade() == Grade.JUNIOR,
            placement, caddies.countByPriorityTrue(), caddies.countByGradeNot(Grade.JUNIOR), lastPosition);
        if (position <= lastPosition) {
            caddies.shiftDownFrom(position);
        }

        String setupCode = newSetupCode();
        User user = new User(request.name().trim(), phone, Role.CADDIE, null, true);
        user.resetToSetupCode(passwordEncoder.encode(setupCode), setupCodeExpiry());
        user = users.save(user);
        Caddie caddie = caddies.save(new Caddie(user, request.grade(), request.priority(), position));
        return new CaddieWithSetupCode(CaddieResponse.from(caddie), setupCode);
    }

    /** Everyone, in club list order. */
    @Transactional(readOnly = true)
    public List<CaddieResponse> list() {
        return caddies.findClubList().stream().map(CaddieResponse::from).toList();
    }

    /** Forgotten password: a new setup code. Their old password and every login token stop working. */
    @Transactional
    public CaddieWithSetupCode resetSetupCode(long caddieId) {
        Caddie caddie = caddies.findById(caddieId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caddie not found"));
        String setupCode = newSetupCode();
        caddie.getUser().resetToSetupCode(passwordEncoder.encode(setupCode), setupCodeExpiry());
        return new CaddieWithSetupCode(CaddieResponse.from(caddie), setupCode);
    }

    /**
     * Admin moves a caddie on the club list, e.g. from 70 to 50 after a mistake. They can only
     * move within their own group (priority / other adults / juniors). Holds until the next
     * daily update re-sorts by bags (docs/RULES.md).
     */
    @Transactional
    public CaddieResponse moveInClubList(long adminId, long caddieId, int newPosition) {
        List<Caddie> list = new ArrayList<>(caddies.findClubList());
        Caddie caddie = list.stream().filter(c -> c.getId() == caddieId).findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caddie not found"));

        // The group's first and last positions (1-based)
        int first = 0;
        int last = 0;
        for (int i = 0; i < list.size(); i++) {
            if (sameGroup(list.get(i), caddie)) {
                if (first == 0) {
                    first = i + 1;
                }
                last = i + 1;
            }
        }
        if (newPosition < first || newPosition > last) {
            throw new ApiException(HttpStatus.BAD_REQUEST, String.format(
                "%s can only move between %d and %d (%s)", caddie.getUser().getName(), first, last, groupName(caddie)));
        }

        int oldPosition = caddie.getListPosition();
        list.remove(caddie);
        list.add(newPosition - 1, caddie);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setListPosition(i + 1); // only changed rows are written
        }
        audit.record(adminId, "MOVE_CLUB_LIST", state("caddie", caddie.getUser().getName(), "position", oldPosition),
            state("position", newPosition));
        return CaddieResponse.from(caddie);
    }

    private static boolean sameGroup(Caddie a, Caddie b) {
        boolean aJunior = a.getGrade() == Grade.JUNIOR;
        boolean bJunior = b.getGrade() == Grade.JUNIOR;
        return aJunior == bJunior && (aJunior || a.isPriority() == b.isPriority());
    }

    private static String groupName(Caddie caddie) {
        if (caddie.getGrade() == Grade.JUNIOR) {
            return "juniors";
        }
        return caddie.isPriority() ? "priority caddies" : "non-priority caddies";
    }

    private LocalDateTime setupCodeExpiry() {
        return LocalDateTime.now(clock).plusHours(setupCodeHours);
    }

    private String newSetupCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
