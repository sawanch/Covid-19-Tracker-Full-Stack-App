package com.covidtracker.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OutbreakTrackerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutbreakTrackerApiApplication.class, args);
    }
}
