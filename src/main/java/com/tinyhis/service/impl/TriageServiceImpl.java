package com.tinyhis.service.impl;

import com.tinyhis.ai.MedicalDocument;
import com.tinyhis.ai.RagTriageService;
import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;
import com.tinyhis.service.TriageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 分诊服务实现
 * 使用基于 RAG（检索增强生成）技术，通过 HTTP 客户端直接连接 SiliconFlow/DeepSeek 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriageServiceImpl implements TriageService {

    private final RagTriageService ragTriageService;

    @Override
    public TriageResult triage(TriageRequest request) {
        throw new UnsupportedOperationException("Synchronous triage is deprecated. Use streamTriage.");
    }

    @Override
    public void streamTriage(TriageRequest request, SseEmitter emitter) {
        ragTriageService.streamTriage(request.getDescription(), request.getBodyPart(), emitter);
    }

    @Override
    public void streamDoctorAssist(Long patientId, String userQuery, String conversationId, SseEmitter emitter) {
        ragTriageService.streamDoctorAssist(patientId, userQuery, conversationId, emitter);
    }
    
    @Override
    public List<MedicalDocument> searchKnowledge(String query, int limit) {
        return ragTriageService.searchKnowledge(query, limit);
    }
}
