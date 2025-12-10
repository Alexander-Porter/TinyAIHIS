package com.tinyhis.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tinyhis.dto.DrugExcelDTO;
import com.tinyhis.dto.Result;
import com.tinyhis.entity.ConsultingRoom;
import com.tinyhis.entity.Department;
import com.tinyhis.entity.Schedule;
import com.tinyhis.entity.ScheduleTemplate;
import com.tinyhis.entity.SysUser;
import com.tinyhis.mapper.ConsultingRoomMapper;
import com.tinyhis.mapper.DepartmentMapper;
import com.tinyhis.mapper.ScheduleMapper;
import com.tinyhis.mapper.ScheduleTemplateMapper;
import com.tinyhis.mapper.SysUserMapper;
import com.tinyhis.service.DataQueryService;
import com.tinyhis.service.ExcelService;
import com.tinyhis.service.ExcelService;
import com.tinyhis.service.ScheduleTemplateService;
import com.tinyhis.service.DashboardService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Controller for management operations
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ExcelService excelService;
    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final ConsultingRoomMapper consultingRoomMapper;
    private final PasswordEncoder passwordEncoder;
    private final ScheduleTemplateMapper scheduleTemplateMapper;
    private final ScheduleTemplateService scheduleTemplateService;
    private final ScheduleMapper scheduleMapper;
    private final DataQueryService dataQueryService;
    private final DashboardService dashboardService;

    // ==================== User Management ====================

    /**
     * Get users with pagination and filters
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> pageResult = sysUserMapper.selectPage(new Page<>(page, size), wrapper);

        // Join department names
        List<SysUser> users = pageResult.getRecords();
        List<Department> depts = departmentMapper.selectList(null);
        Map<Long, String> deptMap = new HashMap<>();
        depts.forEach(d -> deptMap.put(d.getDeptId(), d.getDeptName()));

        List<Map<String, Object>> userList = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", u.getUserId());
            map.put("username", u.getUsername());
            map.put("realName", u.getRealName());
            map.put("role", u.getRole());
            map.put("deptId", u.getDeptId());
            map.put("deptName", u.getDeptId() != null ? deptMap.get(u.getDeptId()) : null);
            map.put("phone", u.getPhone());
            map.put("status", u.getStatus());
            map.put("createTime", u.getCreateTime());
            return map;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", userList);
        result.put("total", pageResult.getTotal());
        return Result.success(result);
    }

    /**
     * Save user (create or update)
     */
    @PostMapping("/user/save")
    public Result<SysUser> saveUser(@RequestBody SysUser user) {
        if (user.getUserId() == null) {
            // Create new user
            if (StringUtils.hasText(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            user.setStatus(1);
            sysUserMapper.insert(user);
        } else {
            // Update existing user
            SysUser existing = sysUserMapper.selectById(user.getUserId());
            if (existing != null) {
                existing.setRealName(user.getRealName());
                existing.setRole(user.getRole());
                existing.setDeptId(user.getDeptId());
                existing.setPhone(user.getPhone());
                if (StringUtils.hasText(user.getPassword())) {
                    existing.setPassword(passwordEncoder.encode(user.getPassword()));
                }
                sysUserMapper.updateById(existing);
                user = existing;
            }
        }
        return Result.success(user);
    }

    /**
     * Update user status
     */
    @PostMapping("/user/{userId}/status")
    public Result<Boolean> updateUserStatus(@PathVariable Long userId, @RequestParam int status) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            user.setStatus(status);
            sysUserMapper.updateById(user);
        }
        return Result.success(true);
    }

    /**
     * Delete user
     */
    @DeleteMapping("/user/{userId}")
    public Result<Boolean> deleteUser(@PathVariable Long userId) {
        sysUserMapper.deleteById(userId);
        return Result.success(true);
    }

    // ==================== Department Management ====================

    /**
     * Save department (create or update)
     */
    @PostMapping("/department/save")
    public Result<Department> saveDepartment(@RequestBody Department dept) {
        if (dept.getDeptId() == null) {
            dept.setStatus(1);
            departmentMapper.insert(dept);
        } else {
            departmentMapper.updateById(dept);
        }
        return Result.success(dept);
    }

    /**
     * Update department status
     */
    @PostMapping("/department/{deptId}/status")
    public Result<Boolean> updateDepartmentStatus(@PathVariable Long deptId, @RequestParam int status) {
        Department dept = departmentMapper.selectById(deptId);
        if (dept != null) {
            dept.setStatus(status);
            departmentMapper.updateById(dept);
        }
        return Result.success(true);
    }

    /**
     * Delete department
     */
    @DeleteMapping("/department/{deptId}")
    public Result<Boolean> deleteDepartment(@PathVariable Long deptId) {
        departmentMapper.deleteById(deptId);
        return Result.success(true);
    }

    // ==================== Statistics ====================

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        // TODO: 实现真实的统计查询
        stats.put("patients", 0);
        stats.put("doctors", sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getRole, "DOCTOR", "CHIEF")));
        stats.put("prescriptions", 0);
        stats.put("labOrders", 0);
        return Result.success(stats);
    }

    // ==================== Drug Import/Export ====================

    /**
     * Import drugs from Excel file
     */
    @PostMapping("/drugs/import")
    public Result<List<DrugExcelDTO>> importDrugs(@RequestParam("file") MultipartFile file) {
        List<DrugExcelDTO> imported = excelService.importDrugs(file);
        return Result.success(imported);
    }

    /**
     * Export drugs to Excel file
     */
    @GetMapping("/drugs/export")
    public void exportDrugs(HttpServletResponse response) {
        excelService.exportDrugs(response);
    }

    // ==================== Schedule Template Management ====================

    /**
     * Get schedule templates for a department
     */
    @GetMapping("/schedule-templates")
    public Result<List<Map<String, Object>>> getScheduleTemplates(@RequestParam Long deptId) {
        List<ScheduleTemplate> templates = scheduleTemplateService.getTemplatesByDept(deptId);

        // Get doctor names
        List<Long> doctorIds = templates.stream()
                .map(ScheduleTemplate::getDoctorId)
                .distinct()
                .toList();
        Map<Long, String> doctorNames = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(SysUser::getUserId, doctorIds);
            sysUserMapper.selectList(wrapper).forEach(u -> doctorNames.put(u.getUserId(), u.getRealName()));
        }

        // Get room names
        List<Long> roomIds = templates.stream()
                .map(ScheduleTemplate::getRoomId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> roomNames = new HashMap<>();
        if (!roomIds.isEmpty()) {
            LambdaQueryWrapper<ConsultingRoom> roomWrapper = new LambdaQueryWrapper<>();
            roomWrapper.in(ConsultingRoom::getRoomId, roomIds);
            consultingRoomMapper.selectList(roomWrapper).forEach(r -> roomNames.put(r.getRoomId(), r.getRoomName()));
        }

        List<Map<String, Object>> result = templates.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("templateId", t.getTemplateId());
            map.put("deptId", t.getDeptId());
            map.put("doctorId", t.getDoctorId());
            map.put("doctorName", doctorNames.get(t.getDoctorId()));
            map.put("roomId", t.getRoomId());
            map.put("roomName", t.getRoomId() != null ? roomNames.get(t.getRoomId()) : null);
            map.put("dayOfWeek", t.getDayOfWeek());
            map.put("shiftType", t.getShiftType());
            map.put("maxQuota", t.getMaxQuota());
            map.put("status", t.getStatus());
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * Save schedule template (create or update)
     * After saving, automatically generate schedule records for the next 14 days
     */
    @PostMapping("/schedule-template/save")
    public Result<ScheduleTemplate> saveScheduleTemplate(@RequestBody ScheduleTemplate template) {
        try {
            scheduleTemplateService.saveScheduleTemplate(template);
            return Result.success(template);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * Delete schedule template
     */
    @DeleteMapping("/schedule-template/{templateId}")
    public Result<Boolean> deleteScheduleTemplate(@PathVariable Long templateId) {
        scheduleTemplateMapper.deleteById(templateId);
        return Result.success(true);
    }

    /**
     * Generate schedules for a week based on templates
     */
    @PostMapping("/schedule/generate-week")
    public Result<Map<String, Object>> generateWeekSchedules(@RequestBody Map<String, String> params) {
        String startDateStr = params.get("startDate");
        String endDateStr = params.get("endDate");

        LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE);
        LocalDate endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ISO_DATE);

        int count = scheduleTemplateService.generateSchedulesFromTemplates(startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("generated", count);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        return Result.success(result);
    }

    /**
     * Get actual schedules for a department and date range
     */
    @GetMapping("/schedules")
    public Result<List<Map<String, Object>>> getSchedules(
            @RequestParam Long deptId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getDeptId, deptId)
                .ge(Schedule::getScheduleDate, start)
                .le(Schedule::getScheduleDate, end)
                .orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getShiftType);

        List<Schedule> schedules = scheduleMapper.selectList(wrapper);

        // Get doctor names
        List<Long> doctorIds = schedules.stream()
                .map(Schedule::getDoctorId)
                .distinct()
                .toList();
        Map<Long, String> doctorNames = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.in(SysUser::getUserId, doctorIds);
            sysUserMapper.selectList(userWrapper).forEach(u -> doctorNames.put(u.getUserId(), u.getRealName()));
        }

        List<Map<String, Object>> result = schedules.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("scheduleId", s.getScheduleId());
            map.put("deptId", s.getDeptId());
            map.put("doctorId", s.getDoctorId());
            map.put("doctorName", doctorNames.get(s.getDoctorId()));
            map.put("scheduleDate", s.getScheduleDate());
            map.put("shiftType", s.getShiftType());
            map.put("maxPatients", s.getMaxPatients());
            map.put("currentPatients", s.getCurrentPatients());
            map.put("status", s.getStatus());
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * Delete a specific schedule
     */
    @DeleteMapping("/schedule/{scheduleId}")
    public Result<Boolean> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleMapper.deleteById(scheduleId);
        return Result.success(true);
    }

    // ==================== Data Query & Export ====================

    /**
     * Get dashboard statistics
     */
    @GetMapping("/dashboard-stats")
    public Result<Map<String, Object>> getDashboardStats() {
        return Result.success(dashboardService.getDashboardStats());
    }

    /**
     * Flexible data query with filters
     */
    @GetMapping("/query")
    public Result<Map<String, Object>> queryData(
            @RequestParam String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean excludeChief) {

        Map<String, Object> result = dataQueryService.queryData(
                type, page, size, startDate, endDate, deptId, doctorId, role, drugId, status, keyword, excludeChief);
        return Result.success(result);
    }

    /**
     * Export data to Excel
     */
    @GetMapping("/export")
    public void exportData(
            @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long drugId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean excludeChief,
            HttpServletResponse response) {

        dataQueryService.exportData(type, startDate, endDate, deptId, doctorId, role, drugId, status, keyword,
                excludeChief, response);
    }

    // ==================== Consulting Room Management ====================

    /**
     * Get all consulting rooms
     */
    @GetMapping("/rooms")
    public Result<List<ConsultingRoom>> getRooms() {
        LambdaQueryWrapper<ConsultingRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ConsultingRoom::getRoomId);
        List<ConsultingRoom> rooms = consultingRoomMapper.selectList(wrapper);
        return Result.success(rooms);
    }

    /**
     * Get consulting room by ID
     */
    @GetMapping("/rooms/{roomId}")
    public Result<ConsultingRoom> getRoom(@PathVariable Long roomId) {
        ConsultingRoom room = consultingRoomMapper.selectById(roomId);
        return Result.success(room);
    }

    /**
     * Create or update consulting room
     */
    @PostMapping("/rooms")
    public Result<ConsultingRoom> saveRoom(@RequestBody ConsultingRoom room) {
        if (room.getRoomId() == null) {
            consultingRoomMapper.insert(room);
        } else {
            consultingRoomMapper.updateById(room);
        }
        return Result.success(room);
    }

    /**
     * Delete consulting room
     */
    @DeleteMapping("/rooms/{roomId}")
    public Result<Void> deleteRoom(@PathVariable Long roomId) {
        consultingRoomMapper.deleteById(roomId);
        return Result.success();
    }
}
