package com.caddieapp.caddie;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface CaddieRepository extends JpaRepository<Caddie, Long> {

    /** The club list: every caddie who hasn't left, with their user loaded in the same query, top first. */
    @Query("select c from Caddie c join fetch c.user u where u.active = true order by c.listPosition, c.id")
    List<Caddie> findClubList();

    /**
     * The same list, locked (SELECT ... FOR UPDATE) until the transaction ends. Everything that
     * renumbers the club list (moves, edits, leavers, new caddies, the 4pm update) loads it this
     * way, so if two admins act at once the second waits instead of mixing the two orders.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caddie c join fetch c.user u where u.active = true order by c.listPosition, c.id")
    List<Caddie> findClubListForUpdate();

    /** Everyone including those who left (admin's Caddies tab), club list order, leavers last. */
    @Query("select c from Caddie c join fetch c.user u order by u.active desc, c.listPosition, u.name")
    List<Caddie> findAllWithUser();

    Optional<Caddie> findByUserId(Long userId);
}
