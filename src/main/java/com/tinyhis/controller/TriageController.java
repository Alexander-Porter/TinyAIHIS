package com.tinyhis.controller;

import com.tinyhis.ai.MedicalDocument;
import com.tinyhis.dto.KnowledgeSearchRequest;
import com.tinyhis.dto.Result;
import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;
import com.tinyhis.service.TriageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

/**
 * AI Triage Controller
 */
@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    @Value("${triage.sse.timeout-ms:180000}")
    private long sseTimeoutMs;

    /**
     * AI-based triage recommendation
     */
    @PostMapping("/recommend")
    public Result<TriageResult> triage(@RequestBody TriageRequest request) {
        TriageResult result = triageService.triage(request);
        return Result.success(result);
    }

    /**
     * Stream AI-based triage recommendation
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTriage(@RequestBody TriageRequest request) {
        SseEmitter emitter = new SseEmitter(sseTimeoutMs); // sse timeout
        triageService.streamTriage(request, emitter);
        return emitter;
    }

    /**
     * Stream Doctor Assistant
     */
    @PostMapping(value = "/doctor-assist", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDoctorAssist(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(sseTimeoutMs); // sse timeout
        
        String content = (String) request.get("content");
        String conversationId = (String) request.get("conversationId");
        
        Object patientIdObj = request.get("patientId");
        Long patientId = null;
        if (patientIdObj != null) {
            if (patientIdObj instanceof Integer) {
                patientId = ((Integer) patientIdObj).longValue();
            } else if (patientIdObj instanceof Long) {
                patientId = (Long) patientIdObj;
            } else if (patientIdObj instanceof String) {
                try {
                    patientId = Long.parseLong((String) patientIdObj);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        
        triageService.streamDoctorAssist(patientId, content, conversationId, emitter);
        return emitter;
    }
    
    /**
     * Global knowledge search endpoint for doctor workstation
     */
    @PostMapping("/search-knowledge")
    public Result<List<MedicalDocument>> searchKnowledge(@RequestBody @Valid KnowledgeSearchRequest request) {
        List<MedicalDocument> results = triageService.searchKnowledge(request.getQuery().trim(), request.getLimit());
        return Result.success(results);
    }
}
