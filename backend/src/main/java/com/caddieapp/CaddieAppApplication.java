package com.caddieapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // for the end-of-day reset and no-reply escalation jobs
public class CaddieAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaddieAppApplication.class, args);
    }
}
