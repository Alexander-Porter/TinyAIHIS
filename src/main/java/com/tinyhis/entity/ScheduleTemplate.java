package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Schedule Template Entity
 * Represents a weekly recurring schedule pattern (e.g., Doctor A works Monday AM every week)
 */
@Data
@TableName("schedule_template")
public class ScheduleTemplate {

    @TableId(type = IdType.AUTO)
    private Long templateId;
    
    private Long deptId;      // Department ID
    private Long doctorId;    // Doctor ID
    private Long roomId;      // 默认诊室 ID
    private Integer dayOfWeek; // 0=Monday, 1=Tuesday, ..., 6=Sunday
    private String shiftType; // AM, PM or ER (emergency)
    private Integer maxQuota; // Maximum appointments per shift
    private Integer status;   // 1=active, 0=inactive
    
    // Transient fields for display (not stored in DB)
    private transient String doctorName;
    private transient String roomName;
    private transient Integer currentCount;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
