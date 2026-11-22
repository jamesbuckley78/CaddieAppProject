package com.caddieapp.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Today" and "now" always mean the club's time zone, whatever the server is set to.
 * Services take a Clock instead of calling LocalDate.now() directly, so tests can fix the time.
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock clock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
