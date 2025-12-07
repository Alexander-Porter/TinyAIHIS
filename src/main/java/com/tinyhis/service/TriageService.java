package com.tinyhis.service;

import com.tinyhis.ai.MedicalDocument;
import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI Triage Service Interface
 */
public interface TriageService {

    /**
     * Perform AI-based triage based on body part and symptoms
     */
    TriageResult triage(TriageRequest request);

    /**
     * Stream AI-based triage
     */
    void streamTriage(TriageRequest request, SseEmitter emitter);

    /**
     * Stream Doctor Assistant
     */
    void streamDoctorAssist(Long patientId, String userQuery, String conversationId, SseEmitter emitter);
    
    /**
     * Search knowledge base
     */
    List<MedicalDocument> searchKnowledge(String query, int limit);
}
