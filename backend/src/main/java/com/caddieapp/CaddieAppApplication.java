package com.caddieapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// The exclude turns off Spring's built-in test user ("Using generated security password"):
// logins go through our own users table and JWTs instead.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan // picks up @ConfigurationProperties records like CheckInProperties
@EnableScheduling // for the end-of-day reset and no-reply escalation jobs
public class CaddieAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaddieAppApplication.class, args);
    }
}
