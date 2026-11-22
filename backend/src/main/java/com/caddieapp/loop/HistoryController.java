package com.caddieapp.loop;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.caddieapp.auth.AuthUser;
import com.caddieapp.loop.HistoryService.HistoryResponse;

@RestController
public class HistoryController {

    private final HistoryService history;

    public HistoryController(HistoryService history) {
        this.history = history;
    }

    /** Caddie: my loops and days worked, the last `weeks` weeks (default 8, max 52). */
    @GetMapping("/api/history/me")
    public HistoryResponse mine(@AuthenticationPrincipal AuthUser user,
                                @RequestParam(defaultValue = "8") int weeks) {
        return history.forUser(user.id(), Math.max(1, Math.min(weeks, 52)));
    }
}
