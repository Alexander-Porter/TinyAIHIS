package com.tinyhis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Check-in Request DTO
 */
@Data
public class CheckInRequest {
    
    @NotNull(message = "挂号ID不能为空")
    private Long regId;
    
    @NotNull(message = "纬度不能为空")
    private Double latitude;
    
    @NotNull(message = "经度不能为空")
    private Double longitude;
}
