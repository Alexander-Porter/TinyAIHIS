package com.tinyhis.service;

import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
}
