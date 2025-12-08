package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tinyhis.entity.Schedule;
import com.tinyhis.entity.ScheduleTemplate;
import com.tinyhis.mapper.ScheduleMapper;
import com.tinyhis.mapper.ScheduleTemplateMapper;
import com.tinyhis.service.ScheduleTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleTemplateServiceImpl extends ServiceImpl<ScheduleTemplateMapper, ScheduleTemplate> 
        implements ScheduleTemplateService {

    private final ScheduleTemplateMapper templateMapper;
    private final ScheduleMapper scheduleMapper;

    @Override
    public List<ScheduleTemplate> getTemplatesByDept(Long deptId) {
        LambdaQueryWrapper<ScheduleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleTemplate::getDeptId, deptId)
               .eq(ScheduleTemplate::getStatus, 1)
               .orderByAsc(ScheduleTemplate::getDayOfWeek)
               .orderByAsc(ScheduleTemplate::getShiftType);
        return templateMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public int generateSchedulesFromTemplates(LocalDate startDate, LocalDate endDate) {
        // Get all active templates
        LambdaQueryWrapper<ScheduleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleTemplate::getStatus, 1);
        List<ScheduleTemplate> templates = templateMapper.selectList(wrapper);
        
        int count = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            // dayOfWeek: 0=Monday, 1=Tuesday, ..., 6=Sunday
            int dayIndex = current.getDayOfWeek().getValue() - 1; // Convert Monday=1 to 0
            
            final LocalDate scheduleDate = current;
            for (ScheduleTemplate template : templates) {
                if (template.getDayOfWeek() == dayIndex) {
                    // Check if schedule already exists
                    LambdaQueryWrapper<Schedule> existsWrapper = new LambdaQueryWrapper<>();
                    existsWrapper.eq(Schedule::getDoctorId, template.getDoctorId())
                                .eq(Schedule::getScheduleDate, scheduleDate)
                                .eq(Schedule::getShiftType, template.getShiftType());
                    
                    if (scheduleMapper.selectCount(existsWrapper) == 0) {
                        // Create new schedule
                        Schedule schedule = new Schedule();
                        schedule.setDeptId(template.getDeptId());
                        schedule.setDoctorId(template.getDoctorId());
                        schedule.setRoomId(template.getRoomId());
                        schedule.setScheduleDate(scheduleDate);
                        schedule.setShiftType(template.getShiftType());
                        schedule.setMaxPatients(template.getMaxQuota());
                        schedule.setCurrentPatients(0);
                        schedule.setStatus(1);
                        schedule.setCreateTime(LocalDateTime.now());
                        schedule.setUpdateTime(LocalDateTime.now());
                        
                        scheduleMapper.insert(schedule);
                        count++;
                        log.debug("Generated schedule for doctor {} on {} {}", 
                                template.getDoctorId(), scheduleDate, template.getShiftType());
                    }
                }
            }
            current = current.plusDays(1);
        }
        
        log.info("Generated {} schedules from templates for {} to {}", count, startDate, endDate);
        return count;
    }

    @Override
    @Transactional
    public int generateNextWeekSchedules() {
        LocalDate today = LocalDate.now();
        // Get next Monday
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        // Get next Sunday
        LocalDate nextSunday = nextMonday.plusDays(6);
        
        return generateSchedulesFromTemplates(nextMonday, nextSunday);
    }
}
