package com.tinyhis.service.impl;

import com.tinyhis.ai.RagTriageService;
import com.tinyhis.ai.TriageRecommendation;
import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;
import com.tinyhis.service.TriageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI Triage Service Implementation
 * Uses RAG (Retrieval Augmented Generation) with Spring AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriageServiceImpl implements TriageService {

    private final RagTriageService ragTriageService;

    @Override
    public TriageResult triage(TriageRequest request) {
        String bodyPart = request.getBodyPart();
        String description = request.getDescription();

        try {
            // Use RAG-based triage
            TriageRecommendation recommendation = ragTriageService.triage(description, bodyPart);
            
            TriageResult result = new TriageResult();
            result.setDeptId(recommendation.getDeptId());
            result.setDeptName(recommendation.getDepartment());
            result.setReason(recommendation.getReason());
            
            return result;
        } catch (Exception e) {
            log.error("Triage failed", e);
            // Fallback to default
            return new TriageResult(1L, "内科", "建议先到内科进行综合评估");
        }
    }
}
