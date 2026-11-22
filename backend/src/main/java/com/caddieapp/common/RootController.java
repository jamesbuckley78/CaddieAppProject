package com.caddieapp.common;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET / - so opening http://localhost:8080 in a browser shows something useful
 * instead of a 403. The backend is an API only; the screens are in the mobile app.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
            "app", "Caddie List API",
            "health", "/api/health",
            "docs", "See docs/API.md for the endpoint list",
            "screens", "Run the mobile app: cd mobile && npx expo start --web (http://localhost:8081)");
    }
}
