package com.caddieapp.caddie;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.caddieapp.common.Enums.Role;

// Spring Data writes the SQL for these from the method names.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByRole(Role role);
}
