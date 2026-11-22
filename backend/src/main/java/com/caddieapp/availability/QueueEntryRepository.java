package com.caddieapp.availability;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByDateAndCaddieId(LocalDate date, Long caddieId);

    /** Everyone who checked in on a day, with caddie and user loaded in one query. */
    @Query("select q from QueueEntry q join fetch q.caddie c join fetch c.user where q.date = :date")
    List<QueueEntry> findDay(@Param("date") LocalDate date);
}
