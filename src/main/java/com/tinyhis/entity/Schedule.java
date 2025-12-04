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
    private Long deptId;           // Department ID
    private Long roomId;           // Consulting Room ID
    private LocalDate scheduleDate;
    private String shiftType; // AM, PM or ER (emergency)
    private Integer maxQuota;      // Maximum number of appointments allowed
    private Integer currentCount;  // Current booked count
    private Integer status;
    
    @Version
    private Integer version;       // Optimistic lock version for flash sale protection
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // Alias methods for compatibility with different naming conventions
    public Integer getMaxPatients() {
        return maxQuota;
    }
    
    public void setMaxPatients(Integer maxPatients) {
        this.maxQuota = maxPatients;
    }
    
    public Integer getCurrentPatients() {
        return currentCount;
    }
    
    public void setCurrentPatients(Integer currentPatients) {
        this.currentCount = currentPatients;
    }
}
