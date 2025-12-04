package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.ScheduleDTO;
import com.tinyhis.entity.ConsultingRoom;
import com.tinyhis.entity.Department;
import com.tinyhis.entity.Schedule;
import com.tinyhis.entity.SysUser;
import com.tinyhis.mapper.ConsultingRoomMapper;
import com.tinyhis.mapper.DepartmentMapper;
import com.tinyhis.mapper.ScheduleMapper;
import com.tinyhis.mapper.SysUserMapper;
import com.tinyhis.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Schedule Service Implementation
 * 
 * Implements flash sale protection using MyBatis-Plus optimistic locking.
 * When multiple users attempt to book the same appointment slot simultaneously,
 * only one will succeed while others receive "号源不足" error.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final DepartmentMapper departmentMapper;
    private final ScheduleMapper scheduleMapper;
    private final SysUserMapper sysUserMapper;
    private final ConsultingRoomMapper consultingRoomMapper;

    private static final BigDecimal DEFAULT_FEE = new BigDecimal("50.00");
    private static final int MAX_RETRY_TIMES = 3;

    @Override
    public List<Department> getAllDepartments() {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getStatus, 1);
        return departmentMapper.selectList(wrapper);
    }

    @Override
    public Department getDepartmentById(Long deptId) {
        return departmentMapper.selectById(deptId);
    }

    @Override
    public List<SysUser> getDoctorsByDept(Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeptId, deptId)
               .in(SysUser::getRole, "DOCTOR", "CHIEF")
               .eq(SysUser::getStatus, 1);
        List<SysUser> doctors = sysUserMapper.selectList(wrapper);
        doctors.forEach(d -> d.setPassword(null));
        return doctors;
    }

    @Override
    public List<ScheduleDTO> getScheduleList(Long deptId, LocalDate startDate, LocalDate endDate) {
        // Get all doctors in department
        List<SysUser> doctors = getDoctorsByDept(deptId);
        if (doctors.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> doctorIds = doctors.stream().map(SysUser::getUserId).toList();
        
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Schedule::getDoctorId, doctorIds)
               .ge(Schedule::getScheduleDate, startDate)
               .le(Schedule::getScheduleDate, endDate)
               .eq(Schedule::getStatus, 1)
               .orderByAsc(Schedule::getScheduleDate)
               .orderByAsc(Schedule::getShiftType);

        List<Schedule> schedules = scheduleMapper.selectList(wrapper);
        Department dept = getDepartmentById(deptId);
        
        // Load all consulting rooms for mapping
        Map<Long, ConsultingRoom> roomMap = consultingRoomMapper.selectList(null)
                .stream()
                .collect(Collectors.toMap(ConsultingRoom::getRoomId, r -> r));

        // 计算号源过期状态
        LocalDate today = LocalDate.now();
        int currentHour = java.time.LocalTime.now().getHour();

        List<ScheduleDTO> result = new ArrayList<>();
        for (Schedule s : schedules) {
            ScheduleDTO dto = new ScheduleDTO();
            dto.setScheduleId(s.getScheduleId());
            dto.setDoctorId(s.getDoctorId());
            dto.setDate(s.getScheduleDate());
            dto.setShift(s.getShiftType());
            dto.setMaxQuota(s.getMaxQuota());
            dto.setCurrentCount(s.getCurrentCount());
            dto.setQuotaLeft(s.getMaxQuota() - s.getCurrentCount());
            dto.setFee(DEFAULT_FEE);
            dto.setDeptId(deptId);
            dto.setDeptName(dept != null ? dept.getDeptName() : null);
            
            // Set room info
            dto.setRoomId(s.getRoomId());
            if (s.getRoomId() != null && roomMap.containsKey(s.getRoomId())) {
                ConsultingRoom room = roomMap.get(s.getRoomId());
                dto.setRoomName(room.getRoomName());
                dto.setRoomLocation(room.getLocation());
            }
            
            // 计算是否过期
            boolean expired = false;
            LocalDate scheduleDate = s.getScheduleDate();
            String shift = s.getShiftType();
            
            if (scheduleDate.isBefore(today)) {
                // 过去的日期一律过期
                expired = true;
            } else if (scheduleDate.equals(today) && !"ER".equalsIgnoreCase(shift)) {
                // 当天的号，根据时段判断
                if ("AM".equalsIgnoreCase(shift) && currentHour >= 12) {
                    expired = true;  // 上午号在12点后过期
                } else if ("PM".equalsIgnoreCase(shift) && currentHour >= 18) {
                    expired = true;  // 下午号在18点后过期
                }
            }
            dto.setExpired(expired);
            
            // Find doctor name
            doctors.stream()
                   .filter(d -> d.getUserId().equals(s.getDoctorId()))
                   .findFirst()
                   .ifPresent(d -> dto.setDoctorName(d.getRealName()));
            
            result.add(dto);
        }

        return result;
    }

    @Override
    public Schedule getScheduleById(Long scheduleId) {
        return scheduleMapper.selectById(scheduleId);
    }

    @Override
    public Schedule saveSchedule(Schedule schedule) {
        if (schedule.getScheduleId() == null) {
            schedule.setVersion(0);  // Initialize version for new schedule
            scheduleMapper.insert(schedule);
        } else {
            scheduleMapper.updateById(schedule);
        }
        return schedule;
    }

    /**
     * Increment appointment count with optimistic locking for flash sale protection.
     * 
     * This method implements a retry mechanism:
     * 1. Read current schedule with version
     * 2. Check if quota is available
     * 3. Update with version check (CAS operation)
     * 4. If update fails (version mismatch), retry up to MAX_RETRY_TIMES
     * 
     * @param scheduleId the schedule ID to increment
     * @return true if successfully incremented, false if quota exhausted or failed
     */
    @Override
    @Transactional
    public boolean incrementCount(Long scheduleId) {
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            Schedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule == null) {
                log.warn("Schedule not found: {}", scheduleId);
                return false;
            }
            
            // Check quota availability
            if (schedule.getCurrentCount() >= schedule.getMaxQuota()) {
                log.info("Schedule {} quota exhausted: {}/{}", 
                    scheduleId, schedule.getCurrentCount(), schedule.getMaxQuota());
                return false;
            }
            
            // Optimistic lock update - MyBatis-Plus will automatically check version
            int newCount = schedule.getCurrentCount() + 1;
            schedule.setCurrentCount(newCount);
            int rows = scheduleMapper.updateById(schedule);
            
            if (rows > 0) {
                log.info("Successfully booked schedule {}: now {}/{}", 
                    scheduleId, newCount, schedule.getMaxQuota());
                return true;
            }
            
            // Version conflict, retry
            log.debug("Optimistic lock conflict for schedule {}, retry {}", scheduleId, i + 1);
        }
        
        log.warn("Failed to book schedule {} after {} retries", scheduleId, MAX_RETRY_TIMES);
        return false;
    }
    
    /**
     * Decrement appointment count (for cancellation).
     * Also uses optimistic locking to ensure consistency.
     */
    @Override
    @Transactional
    public boolean decrementCount(Long scheduleId) {
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            Schedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule == null || schedule.getCurrentCount() <= 0) {
                return false;
            }
            
            schedule.setCurrentCount(schedule.getCurrentCount() - 1);
            int rows = scheduleMapper.updateById(schedule);
            
            if (rows > 0) {
                return true;
            }
        }
        return false;
    }
}
