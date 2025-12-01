package com.tinyhis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Triage Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriageResult {
    private Long deptId;
    private String deptName;
    private String reason;
}
