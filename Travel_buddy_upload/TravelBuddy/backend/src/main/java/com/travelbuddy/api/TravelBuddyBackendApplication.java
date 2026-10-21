package com.travelbuddy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravelBuddyBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelBuddyBackendApplication.class, args);
    }
}
