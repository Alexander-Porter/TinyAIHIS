package com.tinyhis.service;

import com.tinyhis.dto.ScheduleDTO;
import com.tinyhis.entity.Department;
import com.tinyhis.entity.Schedule;
import com.tinyhis.entity.SysUser;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班服务接口
 */
public interface ScheduleService {

    /**
     * 获取所有科室
     */
    List<Department> getAllDepartments();

    /**
     * 根据ID获取科室
     */
    Department getDepartmentById(Long deptId);

    /**
     * 获取科室下的医生列表
     */
    List<SysUser> getDoctorsByDept(Long deptId);

    /**
     * 获取科室在指定日期范围内的排班
     */
    List<ScheduleDTO> getScheduleList(Long deptId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据ID获取排班信息
     */
    Schedule getScheduleById(Long scheduleId);

    /**
     * 创建或更新排班
     */
    Schedule saveSchedule(Schedule schedule);

    /**
     * 增加排班的当前挂号数（带秒杀保护）
     * 使用乐观锁防止超卖
     */
    boolean incrementCount(Long scheduleId);
    
    /**
     * 减少排班的当前挂号数（用于取消预约）
     */
    boolean decrementCount(Long scheduleId);
}
