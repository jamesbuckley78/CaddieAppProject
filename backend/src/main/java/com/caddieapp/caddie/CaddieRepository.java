package com.caddieapp.caddie;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.caddieapp.common.Enums.Grade;

public interface CaddieRepository extends JpaRepository<Caddie, Long> {

    /** The club list: all caddies with their user loaded in the same query, top first. */
    @Query("select c from Caddie c join fetch c.user order by c.listPosition, c.id")
    List<Caddie> findClubList();

    Optional<Caddie> findByUserId(Long userId);

    long countByPriorityTrue();

    /** e.g. countByGradeNot(JUNIOR) = how many adults. */
    long countByGradeNot(Grade grade);

    @Query("select coalesce(max(c.listPosition), 0) from Caddie c")
    int findMaxListPosition();

    /** Makes room at `position` by moving everyone from there down one place. */
    @Modifying(flushAutomatically = true)
    @Query("update Caddie c set c.listPosition = c.listPosition + 1 where c.listPosition >= :position")
    void shiftDownFrom(@Param("position") int position);
}
