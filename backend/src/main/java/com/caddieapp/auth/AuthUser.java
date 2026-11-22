package com.caddieapp.auth;

import com.caddieapp.common.Enums.Role;

/**
 * Who is making the current request, read from their JWT. Controllers get it with
 * {@code @AuthenticationPrincipal AuthUser user}.
 *
 * @param tokenVersion must match users.token_version, otherwise the token has been cancelled
 */
public record AuthUser(long id, Role role, boolean mustChangePassword, int tokenVersion) {
}
