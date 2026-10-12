package com.moveinsync.roster.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Value("${app.timezone:Asia/Kolkata}")
    private String timezone;

    @Value("${app.roster.rest-hours:8}")
    private int restHours;

    @Value("${app.roster.regular-work-hours-per-day:8}")
    private int regularWorkHoursPerDay;

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(timezone));
    }

    @Bean
    public ZoneId applicationZoneId() {
        return ZoneId.of(timezone);
    }

    public int getRestHours() {
        return restHours;
    }

    public int getRegularWorkHoursPerDay() {
        return regularWorkHoursPerDay;
    }
}
