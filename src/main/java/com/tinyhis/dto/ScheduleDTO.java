package com.tinyhis.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Schedule Response DTO
 */
@Data
public class ScheduleDTO {
    private Long scheduleId;
    private Long doctorId;
    private String doctorName;
    private Long deptId;
    private String deptName;
    private Long roomId;
    private String roomName;      // Consulting room name
    private String roomLocation;  // Consulting room location
    private LocalDate date;
    private String shift;
    private Integer maxQuota;      // Maximum appointments allowed
    private Integer currentCount;  // Current booked count
    private Integer quotaLeft;     // Remaining slots
    private BigDecimal fee;
    private Boolean expired;       // 号源是否已过期（当天上午号在下午显示为过期）
}
