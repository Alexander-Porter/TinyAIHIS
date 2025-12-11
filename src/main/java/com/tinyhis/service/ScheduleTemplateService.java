package com.tinyhis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tinyhis.entity.ScheduleTemplate;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleTemplateService extends IService<ScheduleTemplate> {
    
    /**
     * 获取科室的所有排班模板
     */
    List<ScheduleTemplate> getTemplatesByDept(Long deptId);
    
    /**
     * 根据模板生成指定日期范围的排班
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 生成的排班数量
     */
    int generateSchedulesFromTemplates(LocalDate startDate, LocalDate endDate);
    
    /**
     * 保存排班模板（带验证）
     */
    void saveScheduleTemplate(ScheduleTemplate template);

    /**
     * 根据模板生成下周的排班
     */
    int generateNextWeekSchedules();
}
