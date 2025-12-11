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
    private final com.tinyhis.mapper.ConsultingRoomMapper consultingRoomMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    @Transactional
    public void saveScheduleTemplate(ScheduleTemplate template) {
        // 验证诊室
        if (template.getRoomId() != null) {
            com.tinyhis.entity.ConsultingRoom room = consultingRoomMapper.selectById(template.getRoomId());
            if (room == null) {
                throw new IllegalArgumentException("指定的诊室不存在");
            }
            if (org.springframework.util.StringUtils.hasText(room.getDeptIds())) {
                try {
                    java.util.List<Long> allowedDeptIds = objectMapper.readValue(room.getDeptIds(), 
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Long>>(){});
                    if (!allowedDeptIds.contains(template.getDeptId())) {
                        throw new IllegalArgumentException("该诊室不属于当前科室，无法分配");
                    }
                } catch (Exception e) {
                    log.warn("解析诊室科室ID失败: {}", room.getDeptIds());
                }
            }
        }

        template.setUpdateTime(LocalDateTime.now());
        if (template.getTemplateId() == null) {
            template.setStatus(1);
            template.setCreateTime(LocalDateTime.now());
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
        }

        // 为未来14天生成排班
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(14);
        generateScheduleForTemplateInternal(template, today, endDate);
    }
    
    private void generateScheduleForTemplateInternal(ScheduleTemplate template, LocalDate startDate, LocalDate endDate) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // 星期几：0=周一，1=周二，...，6=周日
            int dayIndex = current.getDayOfWeek().getValue() - 1;
            
            if (template.getDayOfWeek() == dayIndex) {
                final LocalDate scheduleDate = current;
                // 检查排班是否已存在
                LambdaQueryWrapper<Schedule> existsWrapper = new LambdaQueryWrapper<>();
                existsWrapper.eq(Schedule::getDoctorId, template.getDoctorId())
                            .eq(Schedule::getScheduleDate, scheduleDate)
                            .eq(Schedule::getShiftType, template.getShiftType());
                
                if (scheduleMapper.selectCount(existsWrapper) == 0) {
                    // 创建新排班
                    Schedule schedule = new Schedule();
                    schedule.setDeptId(template.getDeptId());
                    schedule.setDoctorId(template.getDoctorId());
                    schedule.setScheduleDate(scheduleDate);
                    schedule.setShiftType(template.getShiftType());
                    schedule.setMaxQuota(template.getMaxQuota());
                    schedule.setCurrentCount(0);
                    schedule.setStatus(1);
                    schedule.setVersion(0);
                    schedule.setRoomId(template.getRoomId()); // 从模板设置诊室
                    schedule.setCreateTime(LocalDateTime.now());
                    schedule.setUpdateTime(LocalDateTime.now());
                    
                    scheduleMapper.insert(schedule);
                }
            }
            current = current.plusDays(1);
        }
    }

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
        // 获取所有有效模板
        LambdaQueryWrapper<ScheduleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleTemplate::getStatus, 1);
        List<ScheduleTemplate> templates = templateMapper.selectList(wrapper);
        
        int count = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            // 星期几：0=周一，1=周二，...，6=周日
            int dayIndex = current.getDayOfWeek().getValue() - 1; // 将星期几转换为0-6（周一=0）
            
            final LocalDate scheduleDate = current;
            for (ScheduleTemplate template : templates) {
                if (template.getDayOfWeek() == dayIndex) {
                    // 检查排班是否已存在
                    LambdaQueryWrapper<Schedule> existsWrapper = new LambdaQueryWrapper<>();
                    existsWrapper.eq(Schedule::getDoctorId, template.getDoctorId())
                                .eq(Schedule::getScheduleDate, scheduleDate)
                                .eq(Schedule::getShiftType, template.getShiftType());
                    
                    if (scheduleMapper.selectCount(existsWrapper) == 0) {
                        // 创建新排班
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
                        log.debug("为医生 {} 在 {} {} 生成排班", 
                                template.getDoctorId(), scheduleDate, template.getShiftType());
                    }
                }
            }
            current = current.plusDays(1);
        }
        
        log.info("根据模板为 {} 至 {} 生成了 {} 条排班记录", startDate, endDate, count);
        return count;
    }

    @Override
    @Transactional
    public int generateNextWeekSchedules() {
        LocalDate today = LocalDate.now();
        // 获取下周一
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        // 获取下周日
        LocalDate nextSunday = nextMonday.plusDays(6);
        
        return generateSchedulesFromTemplates(nextMonday, nextSunday);
    }
}
