package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Doctor Schedule Entity
 * Supports optimistic locking for preventing overselling in flash sale scenarios
 */
@Data
@TableName("schedule")
public class Schedule {

    @TableId(type = IdType.AUTO)
    private Long scheduleId;
    
    private Long doctorId;
    private LocalDate scheduleDate;
    private String shiftType; // AM or PM
    private Integer maxQuota;      // Maximum number of appointments allowed
    private Integer currentCount;  // Current booked count
    private Integer status;
    
    @Version
    private Integer version;       // Optimistic lock version for flash sale protection
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
