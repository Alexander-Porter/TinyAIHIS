package com.tinyhis.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lab order view for lab workstation including patient info.
 */
@Data
public class LabOrderView {
    private Long orderId;
    private Long recordId;
    private String itemName;
    private BigDecimal price;
    private Integer status;
    private String resultText;
    private String resultImages;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // Patient snapshot
    private String patientName;
    private Integer gender;
    private Integer age;
}
