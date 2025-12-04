package com.tinyhis.config;

import com.tinyhis.service.ScheduleTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Automatically generates schedules from templates on application startup.
 * Generates schedules for the next 14 days based on weekly templates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleInitializer implements ApplicationRunner {

    private final ScheduleTemplateService scheduleTemplateService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing schedules from templates...");
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(14); // Generate for next 2 weeks
        
        int generated = scheduleTemplateService.generateSchedulesFromTemplates(today, endDate);
        
        log.info("Schedule initialization complete. Generated {} schedules for {} to {}", 
                generated, today, endDate);
    }
}
