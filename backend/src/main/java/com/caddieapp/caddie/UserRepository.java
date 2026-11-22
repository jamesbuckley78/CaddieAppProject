package com.caddieapp.caddie;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.caddieapp.common.Enums.Role;

// Spring Data writes the SQL for these from the method names.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByRole(Role role);

    /** Checked on every request (JwtAuthFilter) - just one number, not the whole user. */
    @Query("select u.tokenVersion from User u where u.id = :id")
    Optional<Integer> findTokenVersion(@Param("id") long id);
}
