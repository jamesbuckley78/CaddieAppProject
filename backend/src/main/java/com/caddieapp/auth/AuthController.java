package com.caddieapp.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.auth.AuthDtos.LoginRequest;
import com.caddieapp.auth.AuthDtos.LoginResponse;
import com.caddieapp.auth.AuthDtos.SetPasswordRequest;
import com.caddieapp.auth.AuthDtos.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Open to everyone. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.phone(), request.password());
    }

    /** Needs a token - including one that still has to change its password. */
    @PostMapping("/set-password")
    public LoginResponse setPassword(@AuthenticationPrincipal AuthUser user,
                                     @Valid @RequestBody SetPasswordRequest request) {
        return authService.setPassword(user.id(), request.newPassword());
    }

    /** The app calls this on start-up to check a saved token is still good. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return authService.me(user.id());
    }
}
