package com.caddieapp.auth;

import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response bodies for /api/auth. Records are Java's immutable data classes. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
        @NotBlank String phone,
        @NotBlank String password) {
    }

    public record SetPasswordRequest(
        // BCrypt only uses the first 72 bytes, so longer passwords give a false sense of security
        @NotBlank @Size(min = 6, max = 72, message = "must be 6 to 72 characters") String newPassword) {
    }

    /** grade and priority are null for admins. */
    public record UserResponse(
        long id,
        String name,
        String phone,
        Role role,
        boolean mustChangePassword,
        Grade grade,
        Boolean priority) {
    }

    public record LoginResponse(String token, UserResponse user) {
    }
}
