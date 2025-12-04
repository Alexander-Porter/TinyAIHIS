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
    
    // GPS 已移除，签到由时间规则控制（就诊前30分钟内签到）
}
