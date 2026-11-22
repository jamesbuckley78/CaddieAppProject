package com.caddieapp.notification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.caddieapp.caddie.User;
import com.caddieapp.common.Enums.BagType;

/**
 * Tells caddies they've been called. Sends an Expo push notification to their phone if the
 * app registered a push token; otherwise the caddie still sees it on their Today screen, which
 * refreshes every few seconds. SMS fallback (Twilio) is planned.
 *
 * Push providers are stored per user (users.push_provider) so a future Flutter app can send
 * Firebase tokens without changing this API.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestClient http;
    private final boolean enabled;

    public NotificationService(RestClient.Builder builder, @Value("${app.push.enabled}") boolean enabled) {
        this.http = builder.build();
        this.enabled = enabled;
    }

    public void caddieCalled(User caddie, BagType bagType) {
        String bag = bagType == BagType.DOUBLE ? "a double" : "a single";
        send(caddie, "You're up!", "You've been called for " + bag + ". Go to the caddie master.");
    }

    /**
     * Fire and forget on another thread: a slow or failed push must never hold up the admin's
     * tap or roll back the call.
     */
    void send(User user, String title, String body) {
        if (!enabled || user.getPushToken() == null || !"EXPO".equals(user.getPushProvider())) {
            return;
        }
        String token = user.getPushToken();
        CompletableFuture.runAsync(() -> {
            try {
                http.post().uri(EXPO_PUSH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(Map.of("to", token, "title", title, "body", body,
                        "sound", "default", "priority", "high")))
                    .retrieve()
                    .toBodilessEntity();
            } catch (RuntimeException e) {
                log.warn("Push notification failed for user {}: {}", user.getId(), e.getMessage());
            }
        });
    }
}
