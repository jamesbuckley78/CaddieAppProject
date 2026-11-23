package com.caddieapp.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.auth.AdminAccounts.AdminResponse;
import com.caddieapp.auth.AdminAccounts.AdminWithSetupCode;
import com.caddieapp.common.PhoneNumbers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin-only (SecurityConfig: /api/admin/**). */
@RestController
@RequestMapping("/api/admin/admins")
public class AdminAccountController {

    public record NewAdminRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = PhoneNumbers.PATTERN, message = "is not a valid phone number") String phone) {
    }

    private final AdminAccounts admins;

    public AdminAccountController(AdminAccounts admins) {
        this.admins = admins;
    }

    @GetMapping
    public List<AdminResponse> list() {
        return admins.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminWithSetupCode create(@AuthenticationPrincipal AuthUser admin, @Valid @RequestBody NewAdminRequest request) {
        return admins.create(admin.id(), request.name(), request.phone());
    }

    @PostMapping("/{id}/reset-code")
    public AdminWithSetupCode resetSetupCode(@AuthenticationPrincipal AuthUser admin, @PathVariable long id) {
        return admins.resetSetupCode(admin.id(), id);
    }

    @PostMapping("/{id}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthUser admin, @PathVariable long id) {
        admins.remove(admin.id(), id);
    }
}
