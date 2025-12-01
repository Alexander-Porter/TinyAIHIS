package com.tinyhis.controller;

import com.tinyhis.dto.Result;
import com.tinyhis.dto.ScheduleDTO;
import com.tinyhis.entity.Department;
import com.tinyhis.entity.Schedule;
import com.tinyhis.entity.SysUser;
import com.tinyhis.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Schedule Controller
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Get all departments
     */
    @GetMapping("/departments")
    public Result<List<Department>> getDepartments() {
        List<Department> departments = scheduleService.getAllDepartments();
        return Result.success(departments);
    }

    /**
     * Get department by ID
     */
    @GetMapping("/department/{deptId}")
    public Result<Department> getDepartment(@PathVariable Long deptId) {
        Department department = scheduleService.getDepartmentById(deptId);
        return Result.success(department);
    }

    /**
     * Get doctors by department
     */
    @GetMapping("/doctors")
    public Result<List<SysUser>> getDoctors(@RequestParam Long deptId) {
        List<SysUser> doctors = scheduleService.getDoctorsByDept(deptId);
        return Result.success(doctors);
    }

    /**
     * Get schedule list
     */
    @GetMapping("/list")
    public Result<List<ScheduleDTO>> getScheduleList(
            @RequestParam Long deptId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ScheduleDTO> schedules = scheduleService.getScheduleList(deptId, startDate, endDate);
        return Result.success(schedules);
    }

    /**
     * Get schedule by ID
     */
    @GetMapping("/{scheduleId}")
    public Result<Schedule> getSchedule(@PathVariable Long scheduleId) {
        Schedule schedule = scheduleService.getScheduleById(scheduleId);
        return Result.success(schedule);
    }

    /**
     * Create or update schedule (Admin only)
     */
    @PostMapping("/save")
    public Result<Schedule> saveSchedule(@RequestBody Schedule schedule) {
        Schedule saved = scheduleService.saveSchedule(schedule);
        return Result.success(saved);
    }
}
