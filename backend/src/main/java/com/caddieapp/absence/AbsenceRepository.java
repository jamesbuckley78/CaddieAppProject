package com.caddieapp.absence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    boolean existsByCaddieIdAndDate(Long caddieId, LocalDate date);

    /** A caddie's own requests from a date on, soonest first. */
    List<Absence> findByCaddieIdAndDateGreaterThanEqualOrderByDateAsc(Long caddieId, LocalDate from);

    /** Waiting for the admin, soonest first. */
    @Query("select a from Absence a join fetch a.caddie c join fetch c.user "
        + "where a.approved is null and a.date >= :from order by a.date, a.submittedAt")
    List<Absence> findPending(@Param("from") LocalDate from);

    /** Approved or rejected recently, newest decision first. */
    @Query("select a from Absence a join fetch a.caddie c join fetch c.user "
        + "where a.approved is not null and a.decidedAt >= :since order by a.decidedAt desc")
    List<Absence> findDecidedSince(@Param("since") java.time.LocalDateTime since);

    /** Names of caddies with an approved absence on a day - shown on the admin board. */
    @Query("select u.name from Absence a join a.caddie c join c.user u "
        + "where a.date = :date and a.approved = true order by u.name")
    List<String> approvedNamesOn(@Param("date") LocalDate date);
}
