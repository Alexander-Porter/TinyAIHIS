package com.tinyhis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tinyhis.entity.ScheduleTemplate;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleTemplateService extends IService<ScheduleTemplate> {
    
    /**
     * Get all templates for a department
     */
    List<ScheduleTemplate> getTemplatesByDept(Long deptId);
    
    /**
     * Generate schedules for a specific date range based on templates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Number of schedules generated
     */
    int generateSchedulesFromTemplates(LocalDate startDate, LocalDate endDate);
    
    /**
     * Save schedule template with validation
     */
    void saveScheduleTemplate(ScheduleTemplate template);

    /**
     * Generate schedules for next week based on templates
     */
    int generateNextWeekSchedules();
}
