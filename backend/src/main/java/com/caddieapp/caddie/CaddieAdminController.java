package com.caddieapp.caddie;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.caddie.CaddieDtos.CaddieResponse;
import com.caddieapp.caddie.CaddieDtos.CaddieWithSetupCode;
import com.caddieapp.caddie.CaddieDtos.CreateCaddieRequest;

import jakarta.validation.Valid;

/** Admin-only (SecurityConfig: /api/admin/** needs ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/caddies")
public class CaddieAdminController {

    private final CaddieService caddieService;

    public CaddieAdminController(CaddieService caddieService) {
        this.caddieService = caddieService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaddieWithSetupCode create(@Valid @RequestBody CreateCaddieRequest request) {
        return caddieService.create(request);
    }

    @GetMapping
    public List<CaddieResponse> list() {
        return caddieService.list();
    }

    @PostMapping("/{id}/reset-code")
    public CaddieWithSetupCode resetSetupCode(@PathVariable long id) {
        return caddieService.resetSetupCode(id);
    }
}
