package com.tinyhis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Registration (Appointment) Request DTO
 */
@Data
public class RegistrationRequest {
    
    @NotNull(message = "患者ID不能为空")
    private Long patientId;
    
    @NotNull(message = "排班ID不能为空")
    private Long scheduleId;
}
