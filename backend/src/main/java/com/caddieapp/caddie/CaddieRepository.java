package com.caddieapp.caddie;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CaddieRepository extends JpaRepository<Caddie, Long> {

    /** All caddies with their user loaded in the same query (avoids one query per caddie). */
    @Query("select c from Caddie c join fetch c.user u order by u.name")
    List<Caddie> findAllWithUser();

    Optional<Caddie> findByUserId(Long userId);
}
