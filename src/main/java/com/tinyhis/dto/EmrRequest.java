package com.tinyhis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * EMR (Electronic Medical Record) Request DTO
 */
@Data
public class EmrRequest {
    
    @NotNull(message = "挂号ID不能为空")
    private Long regId;
    
    private String symptom; // Chief complaint
    
    private String diagnosis;
    
    private String content; // Medical history details
    
    private List<PrescriptionItem> prescriptions;
    
    private List<LabOrderItem> labOrders;
    
    @Data
    public static class PrescriptionItem {
        private Long drugId;
        private Integer quantity;
        private String usageInstruction;
    }
    
    @Data
    public static class LabOrderItem {
        private String itemName;
        private java.math.BigDecimal price;
    }
}
