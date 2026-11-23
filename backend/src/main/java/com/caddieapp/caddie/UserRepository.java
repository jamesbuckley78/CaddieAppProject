package com.caddieapp.caddie;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.caddieapp.common.Enums.Role;

// Spring Data writes the SQL for these from the method names.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByRole(Role role);

    /** The current admins (Admins tab). */
    List<User> findByRoleAndActiveTrueOrderByName(Role role);

    /** A phone's push token belongs to whoever last registered it - e.g. a phone passed to another caddie. */
    @Modifying
    @Query("update User u set u.pushToken = null, u.pushProvider = null where u.pushToken = :token and u.id <> :userId")
    void clearPushTokenFromOthers(@Param("token") String token, @Param("userId") long userId);

    /** Checked on every request (JwtAuthFilter) - just one number, not the whole user. */
    @Query("select u.tokenVersion from User u where u.id = :id")
    Optional<Integer> findTokenVersion(@Param("id") long id);
}
