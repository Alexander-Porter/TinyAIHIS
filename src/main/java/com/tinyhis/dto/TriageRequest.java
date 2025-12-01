package com.tinyhis.dto;

import lombok.Data;

/**
 * AI Triage Request DTO
 */
@Data
public class TriageRequest {
    private String bodyPart;
    private String description;
}
