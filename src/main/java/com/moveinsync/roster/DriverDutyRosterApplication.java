package com.moveinsync.roster;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DriverDutyRosterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverDutyRosterApplication.class, args);
    }
}
