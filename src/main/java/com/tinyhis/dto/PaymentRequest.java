package com.tinyhis.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Payment Request DTO
 */
@Data
public class PaymentRequest {
    /**
     * Payment type: REGISTRATION, PRESCRIPTION, LAB
     */
    private String paymentType;
    
    /**
     * Item IDs to pay for
     */
    private List<Long> itemIds;
}
