package com.caddieapp.caddie;

import static com.caddieapp.audit.AuditLog.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caddieapp.audit.AuditLog;
import com.caddieapp.auth.SetupCodes;
import com.caddieapp.caddie.CaddieDtos.CaddieResponse;
import com.caddieapp.caddie.CaddieDtos.CaddieWithSetupCode;
import com.caddieapp.caddie.CaddieDtos.CreateCaddieRequest;
import com.caddieapp.caddie.CaddieDtos.EditCaddieRequest;
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
    private final SetupCodes setupCodes;
    private final QueueService queueService;
    private final AuditLog audit;

    public CaddieService(UserRepository users, CaddieRepository caddies, SetupCodes setupCodes,
                         QueueService queueService, AuditLog audit) {
        this.users = users;
        this.caddies = caddies;
        this.setupCodes = setupCodes;
        this.queueService = queueService;
        this.audit = audit;
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
        User user = new User(request.name().trim(), phone, Role.CADDIE, null, true);
        String setupCode = setupCodes.issue(user);
        user = users.save(user);
        Caddie caddie = new Caddie(user, request.grade(), request.priority(), 0);
        placeInGroup(caddie, placement);
        caddie = caddies.save(caddie);
        return new CaddieWithSetupCode(CaddieResponse.from(caddie), setupCode);
    }

    /** Everyone, in club list order, then those who have left. */
    @Transactional(readOnly = true)
    public List<CaddieResponse> list() {
        return caddies.findAllWithUser().stream().map(CaddieResponse::from).toList();
    }

    /** Forgotten password: a new setup code. Their old password and every login token stop working. */
    @Transactional
    public CaddieWithSetupCode resetSetupCode(long caddieId) {
        Caddie caddie = find(caddieId);
        String setupCode = setupCodes.issue(caddie.getUser());
        return new CaddieWithSetupCode(CaddieResponse.from(caddie), setupCode);
    }

    /**
     * Admin moves a caddie on the club list, e.g. from 70 to 50 after a mistake. They can only
     * move within their own group (priority / other adults / juniors). Holds until the next
     * daily update re-sorts by bags (docs/RULES.md).
     */
    @Transactional
    public CaddieResponse moveInClubList(long adminId, long caddieId, int newPosition) {
        List<Caddie> list = new ArrayList<>(caddies.findClubListForUpdate());
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
        renumber(list);
        audit.record(adminId, "MOVE_CLUB_LIST", state("caddie", caddie.getUser().getName(), "position", oldPosition),
            state("position", newPosition));
        return CaddieResponse.from(caddie);
    }

    /**
     * Correct a name or phone number, promote / demote (priority), or change grade (e.g. a junior
     * becoming an adult). Someone who changes group goes to the top of their new group, like a
     * new caddie, until the next 4pm update sorts them by bags (docs/RULES.md).
     */
    @Transactional
    public CaddieResponse edit(long adminId, long caddieId, EditCaddieRequest request) {
        Caddie caddie = find(caddieId);
        User user = caddie.getUser();
        String phone = PhoneNumbers.normalise(request.phone());
        if (!phone.equals(user.getPhone()) && users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }
        if (request.priority() && request.grade() == Grade.JUNIOR) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Juniors can't be priority caddies");
        }

        Map<String, Object> before = details(caddie);
        String oldGroup = groupName(caddie);
        user.changeDetails(request.name().trim(), phone);
        caddie.changeGrade(request.grade(), request.priority());
        if (user.isActive() && !groupName(caddie).equals(oldGroup)) {
            placeInGroup(caddie, Placement.TOP_OF_GROUP);
        }
        audit.record(adminId, "EDIT_CADDIE", before, details(caddie));
        return CaddieResponse.from(caddie);
    }

    /** Left the club: off the club list and can't log in. Their bags and history are kept. */
    @Transactional
    public CaddieResponse markLeft(long adminId, long caddieId) {
        Caddie caddie = find(caddieId);
        if (!caddie.getUser().isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, caddie.getUser().getName() + " has already left");
        }
        List<Caddie> list = new ArrayList<>(caddies.findClubListForUpdate());
        list.remove(caddie);
        renumber(list); // everyone below them moves up one
        Map<String, Object> before = details(caddie);
        caddie.setListPosition(0);
        caddie.getUser().markLeft();
        audit.record(adminId, "CADDIE_LEFT", before, state("active", false));
        return CaddieResponse.from(caddie);
    }

    /** Back at the club: top of their group, like a new caddie. Their old password works again. */
    @Transactional
    public CaddieResponse bringBack(long adminId, long caddieId) {
        Caddie caddie = find(caddieId);
        if (caddie.getUser().isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, caddie.getUser().getName() + " is already on the list");
        }
        caddie.getUser().markReturned();
        placeInGroup(caddie, Placement.TOP_OF_GROUP);
        audit.record(adminId, "CADDIE_BACK", state("active", false), details(caddie));
        return CaddieResponse.from(caddie);
    }

    private Caddie find(long caddieId) {
        return caddies.findById(caddieId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caddie not found"));
    }

    /**
     * Puts the caddie at the top or bottom of their group (docs/RULES.md) and renumbers everyone:
     * a new caddie, someone brought back, or someone whose grade / priority changed. The only
     * place that does this, so the group rules live in one spot (QueueService.newCaddiePosition).
     */
    private void placeInGroup(Caddie caddie, Placement placement) {
        List<Caddie> list = new ArrayList<>(caddies.findClubListForUpdate());
        list.remove(caddie); // already on the list when they're changing group
        long priorityCount = list.stream().filter(Caddie::isPriority).count();
        long adultCount = list.stream().filter(c -> c.getGrade() != Grade.JUNIOR).count();
        int position = queueService.newCaddiePosition(caddie.isPriority(), caddie.getGrade() == Grade.JUNIOR,
            placement, priorityCount, adultCount, list.size());
        list.add(position - 1, caddie);
        renumber(list);
    }

    private static void renumber(List<Caddie> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setListPosition(i + 1); // only changed rows are written
        }
    }

    private static Map<String, Object> details(Caddie caddie) {
        return state("caddie", caddie.getUser().getName(), "phone", caddie.getUser().getPhone(),
            "grade", caddie.getGrade(), "priority", caddie.isPriority(), "position", caddie.getListPosition());
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

}
