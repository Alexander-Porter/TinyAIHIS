package com.tinyhis.service;

import com.tinyhis.ai.MedicalDocument;
import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI分诊服务接口
 */
public interface TriageService {

    /**
     * 基于身体部位和症状执行AI分诊
     */
    TriageResult triage(TriageRequest request);

    /**
     * 流式AI分诊
     */
    void streamTriage(TriageRequest request, SseEmitter emitter);

    /**
     * 流式医生助手
     */
    void streamDoctorAssist(Long patientId, String userQuery, String conversationId, SseEmitter emitter);
    
    /**
     * 搜索知识库
     */
    List<MedicalDocument> searchKnowledge(String query, int limit);
}
