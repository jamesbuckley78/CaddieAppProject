package com.caddieapp.loop;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoopRepository extends JpaRepository<Loop, Long> {

    /** The loop a caddie is out on right now (sent out today, not back yet). */
    Optional<Loop> findFirstByCaddieIdAndDateAndSentOutAtIsNotNullAndReturnedAtIsNullOrderByIdDesc(Long caddieId,
                                                                                                 LocalDate date);

    /** Everyone out on course on a day (sent out, not back yet). */
    List<Loop> findByDateAndSentOutAtIsNotNullAndReturnedAtIsNull(LocalDate date);

    /** A caddie's loops between two dates, newest first - for their history. */
    @Query("select l from Loop l where l.caddie.id = :caddieId and l.date between :from and :to "
        + "order by l.date desc, l.id desc")
    List<Loop> findHistory(@Param("caddieId") Long caddieId, @Param("from") LocalDate from,
                           @Param("to") LocalDate to);
}
