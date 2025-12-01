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
    private String deptName;
    private LocalDate date;
    private String shift;
    private Integer maxQuota;      // Maximum appointments allowed
    private Integer currentCount;  // Current booked count
    private Integer quotaLeft;     // Remaining slots
    private BigDecimal fee;
}
