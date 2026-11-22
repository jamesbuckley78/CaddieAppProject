package com.caddieapp;

import org.springframework.test.util.ReflectionTestUtils;

import com.caddieapp.caddie.User;
import com.caddieapp.common.Enums.Role;

/** Builds User objects for unit tests (the real id normally comes from the database). */
public final class TestUsers {

    private TestUsers() {
    }

    public static User user(long id, Role role, String passwordHash, boolean mustChangePassword) {
        User user = new User("Test " + role, "0871234567", role, passwordHash, mustChangePassword);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
