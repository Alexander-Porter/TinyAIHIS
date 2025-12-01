package com.tinyhis.ai;

import lombok.Data;
import java.util.List;

/**
 * Triage Recommendation from RAG
 */
@Data
public class TriageRecommendation {
    private Long deptId;
    private String department;
    private String reason;
    private String possibleDiseases;
    private List<String> referenceDocs;
}
