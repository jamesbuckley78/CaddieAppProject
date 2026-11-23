package com.caddieapp.availability;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByDateAndCaddieId(LocalDate date, Long caddieId);

    /**
     * Same as above but locks the row (SELECT ... FOR UPDATE) until the transaction ends. If two
     * admins tap "Call" on the same caddie at once, the second waits, then sees they're already
     * called (CLAUDE.md: assigning must use a row lock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QueueEntry q where q.date = :date and q.caddie.id = :caddieId")
    Optional<QueueEntry> findForUpdate(@Param("date") LocalDate date, @Param("caddieId") Long caddieId);

    /** Everyone who checked in on a day, with caddie and user loaded in one query. */
    @Query("select q from QueueEntry q join fetch q.caddie c join fetch c.user u where q.date = :date and u.active = true")
    List<QueueEntry> findDay(@Param("date") LocalDate date);
}
