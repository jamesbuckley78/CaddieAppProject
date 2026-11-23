package com.caddieapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.caddieapp.availability.AttendanceReport;
import com.caddieapp.caddie.Caddie;
import com.caddieapp.caddie.CaddieRepository;
import com.caddieapp.common.Enums.BagType;
import com.caddieapp.config.TimeConfig;
import com.caddieapp.issue.IssueReports;
import com.caddieapp.issue.IssueReports.CaddieCount;
import com.caddieapp.loop.BagCounts;
import com.caddieapp.loop.Loop;

/**
 * The plain-SQL parts, run on the real local MySQL from docker-compose.yml. Unit tests use fake
 * repositories, so this is the only test that catches a mistake in the SQL itself.
 *
 * Safe for your data: @DataJpaTest undoes (rolls back) everything at the end of each test, and the
 * test only uses dates in 2001. MySQL picks the ids (its id counter is never rolled back, so fixed
 * ids like 900001 would make every later real id jump). Skipped when the local database isn't
 * running, and never runs against anything but localhost.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // the real MySQL, not an in-memory DB
@ImportAutoConfiguration(JdbcTemplateAutoConfiguration.class)
@Import({BagCounts.class, IssueReports.class, AttendanceReport.class, TimeConfig.class})
@EnabledIf("localMysqlIsUp")
class DatabaseQueriesTest {

    // A Monday-Sunday week long ago, so nothing real falls in it
    static final LocalDate MON = LocalDate.of(2001, 1, 1);
    static final LocalDate WED = MON.plusDays(2);
    static final LocalDate SUN_BEFORE = MON.minusDays(1);
    static final LocalDate MON_AFTER = MON.plusWeeks(1);

    // ids MySQL gives the test's admin and caddies (set in twoCaddiesAndAnAdmin)
    long admin;
    long aoife;
    long brian;

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    CaddieRepository caddies;
    @Autowired
    BagCounts bagCounts;
    @Autowired
    IssueReports issues;
    @Autowired
    AttendanceReport attendance;

    /** JUnit asks this before starting: is docker-compose's MySQL running here, and is the test pointed at it? */
    static boolean localMysqlIsUp() {
        String url = System.getenv("DB_URL");
        if (url != null && !url.contains("localhost") && !url.contains("127.0.0.1")) {
            return false; // never touch a non-local database
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 3306), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeEach
    void twoCaddiesAndAnAdmin() {
        jdbc.update("INSERT INTO users (name, phone, role) VALUES ('Test Admin', '0800900001', 'ADMIN')");
        admin = lastId();
        aoife = caddie("Aoife", "0800900010", 9001);
        brian = caddie("Brian", "0800900011", 9002);
    }

    @Test
    void lockedClubListLoadsOnMySqlInListOrder() {
        // The locking query every club list change uses (SELECT ... FOR UPDATE with the users joined)
        List<Caddie> list = caddies.findClubListForUpdate();

        assertThat(list).filteredOn(c -> Set.of(aoife, brian).contains(c.getId()))
            .extracting(c -> c.getUser().getName())
            .containsExactly("Aoife", "Brian");
    }

    @Test
    void bagCountsUseTheSameRuleAsTheJavaCodeIncludingDoubleLoops() {
        // Aoife: a single, a double, a single double loop and a double double loop; Brian: one single
        loop(aoife, MON, BagType.SINGLE, false);
        loop(aoife, WED, BagType.DOUBLE, false);
        loop(aoife, WED, BagType.SINGLE, true);
        loop(aoife, WED, BagType.DOUBLE, true);
        loop(brian, MON, BagType.SINGLE, false);
        loop(brian, SUN_BEFORE, BagType.DOUBLE, false); // the week before: not counted

        // The bag rule is written twice: SQL (BagCounts, the list order) and Java (Loop.bags(), History)
        int javaRule = javaBags(BagType.SINGLE, false) + javaBags(BagType.DOUBLE, false)
            + javaBags(BagType.SINGLE, true) + javaBags(BagType.DOUBLE, true);
        assertThat(javaRule).isEqualTo(1 + 2 + 2 + 4);

        assertThat(bagCounts.perCaddie(MON, MON.plusDays(6)))
            .containsExactlyInAnyOrderEntriesOf(Map.of(aoife, javaRule, brian, 1));
        assertThat(bagCounts.total(WED)).isEqualTo(2 + 2 + 4);
        assertThat(bagCounts.perCaddiePerDay(MON, WED, BagCounts.ALL_LOOPS).get(aoife))
            .containsExactlyInAnyOrderEntriesOf(Map.of(MON, 1, WED, 8));
    }

    @Test
    void issueSummaryCountsOnlyItsWeekAndRanksWhoReportsMost() {
        int allTimeBefore = issues.summary(null).total();
        issue(brian, SUN_BEFORE.atTime(23, 59)); // previous week
        issue(aoife, MON.atStartOfDay()); // first moment of the week
        issue(aoife, WED.atTime(12, 0));
        issue(brian, WED.atTime(13, 0));
        issue(aoife, MON_AFTER.atStartOfDay()); // next week

        IssueReports.Summary week = issues.summary(WED);
        assertThat(week.weekStart()).isEqualTo(MON);
        assertThat(week.total()).isEqualTo(3);
        assertThat(week.byCaddie()).extracting(CaddieCount::name, CaddieCount::reports)
            .containsExactly(tuple("Aoife", 2), tuple("Brian", 1));
        assertThat(week.issues()).extracting(IssueReports.Issue::caddieName).containsExactly("Brian", "Aoife", "Aoife");

        assertThat(issues.summary(null).total()).isEqualTo(allTimeBefore + 5); // all time
    }

    @Test
    void attendanceWeekShowsWhoCameInEachDay() {
        checkIn(aoife, MON.atTime(8, 0));
        checkIn(aoife, WED.atTime(9, 0));
        checkIn(brian, WED.atTime(7, 30));
        checkIn(brian, SUN_BEFORE.atTime(7, 0)); // previous week

        AttendanceReport.Week week = attendance.week(WED);

        assertThat(week.dayTotals()).containsExactly(1, 0, 2, 0, 0, 0, 0);
        assertThat(week.caddiesIn()).isEqualTo(2);
        assertThat(week.rows()).filteredOn(r -> Set.of(aoife, brian).contains(r.caddieId()))
            .extracting(AttendanceReport.Row::name, AttendanceReport.Row::daysWorked)
            .containsExactly(tuple("Aoife", 2), tuple("Brian", 1));
    }

    /** Adds a caddie (and their login) and returns the caddie id MySQL gave it. */
    private long caddie(String name, String phone, int position) {
        jdbc.update("INSERT INTO users (name, phone, role) VALUES (?, ?, 'CADDIE')", name, phone);
        long userId = lastId();
        jdbc.update("INSERT INTO caddies (user_id, grade, is_priority, list_position) VALUES (?, 'SINGLE', FALSE, ?)",
            userId, position);
        return lastId();
    }

    /** The id MySQL gave the last row this connection inserted. */
    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void loop(long caddieId, LocalDate date, BagType bag, boolean doubleLoop) {
        jdbc.update("INSERT INTO loops (caddie_id, date, bag_type, double_loop, assigned_by) VALUES (?, ?, ?, ?, ?)",
            caddieId, date, bag.name(), doubleLoop, admin);
    }

    private void issue(long caddieId, LocalDateTime at) {
        jdbc.update("INSERT INTO issue_reports (caddie_id, type, created_at) VALUES (?, 'RANGER', ?)", caddieId, at);
    }

    private void checkIn(long caddieId, LocalDateTime at) {
        jdbc.update("INSERT INTO attendance (caddie_id, date, checked_in_at) VALUES (?, ?, ?)", caddieId,
            at.toLocalDate(), at);
    }

    /** Bags for one loop by the Java rule (Loop.bags(), used by History). */
    private static int javaBags(BagType bag, boolean doubleLoop) {
        Loop loop = new Loop(null, MON, bag, null, null, null);
        loop.markDoubleLoop(doubleLoop);
        return loop.bags();
    }
}
