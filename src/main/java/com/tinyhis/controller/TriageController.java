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
 * AI分诊控制器
 */
@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    @Value("${triage.sse.timeout-ms:180000}")
    private long sseTimeoutMs;

    /**
     * 基于AI的分诊推荐
     */
    @PostMapping("/recommend")
    public Result<TriageResult> triage(@RequestBody TriageRequest request) {
        TriageResult result = triageService.triage(request);
        return Result.success(result);
    }

    /**
     * 流式AI分诊推荐
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTriage(@RequestBody TriageRequest request) {
SseEmitter emitter = new SseEmitter(sseTimeoutMs); // SSE超时时间
        triageService.streamTriage(request, emitter);
        return emitter;
    }

    /**
     * 流式医生助手
     */
    @PostMapping(value = "/doctor-assist", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDoctorAssist(@RequestBody Map<String, Object> request) {
SseEmitter emitter = new SseEmitter(sseTimeoutMs); // SSE超时时间
        
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
                    // 忽略格式错误
                }
            }
        }
        
        triageService.streamDoctorAssist(patientId, content, conversationId, emitter);
        return emitter;
    }
    
    /**
     * 医生工作站的全局知识库搜索接口
     */
    @PostMapping("/search-knowledge")
    public Result<List<MedicalDocument>> searchKnowledge(@RequestBody @Valid KnowledgeSearchRequest request) {
        List<MedicalDocument> results = triageService.searchKnowledge(request.getQuery().trim(), request.getLimit());
        return Result.success(results);
    }
}
