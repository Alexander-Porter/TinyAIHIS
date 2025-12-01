package com.tinyhis.service;

import com.tinyhis.dto.ScheduleDTO;
import com.tinyhis.entity.Department;
import com.tinyhis.entity.Schedule;
import com.tinyhis.entity.SysUser;

import java.time.LocalDate;
import java.util.List;

/**
 * Schedule Service Interface
 */
public interface ScheduleService {

    /**
     * Get all departments
     */
    List<Department> getAllDepartments();

    /**
     * Get department by ID
     */
    Department getDepartmentById(Long deptId);

    /**
     * Get doctors by department
     */
    List<SysUser> getDoctorsByDept(Long deptId);

    /**
     * Get schedules by department and date range
     */
    List<ScheduleDTO> getScheduleList(Long deptId, LocalDate startDate, LocalDate endDate);

    /**
     * Get schedule by ID
     */
    Schedule getScheduleById(Long scheduleId);

    /**
     * Create or update schedule
     */
    Schedule saveSchedule(Schedule schedule);

    /**
     * Increment current count for schedule (with flash sale protection)
     * Uses optimistic locking to prevent overselling
     */
    boolean incrementCount(Long scheduleId);
    
    /**
     * Decrement current count for schedule (for cancellation)
     */
    boolean decrementCount(Long scheduleId);
}
