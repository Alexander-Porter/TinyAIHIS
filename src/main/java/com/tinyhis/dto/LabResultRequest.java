package com.tinyhis.dto;

import lombok.Data;

/**
 * Lab Result Request DTO
 */
@Data
public class LabResultRequest {
    private Long orderId;
    private String resultText;
    private String resultImages; // JSON array of URLs
}
