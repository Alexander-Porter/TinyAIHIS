package com.tinyhis.service;

import com.tinyhis.ai.MedicalKnowledgeBase;
import com.tinyhis.ai.RagTriageService;
import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;
import com.tinyhis.service.impl.TriageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Triage Service Unit Tests
 */
class TriageServiceTest {

    private TriageService triageService;

    @BeforeEach
    void setUp() {
        MedicalKnowledgeBase knowledgeBase = Mockito.mock(MedicalKnowledgeBase.class);
        RagTriageService ragTriageService = new RagTriageService(knowledgeBase);
        triageService = new TriageServiceImpl(ragTriageService);
    }

    @Test
    void testTriage_HeadPain() {
        TriageRequest request = new TriageRequest();
        request.setBodyPart("头部");
        request.setDescription("头痛");

        TriageResult result = triageService.triage(request);

        assertNotNull(result);
        // Fallback mode will return 内科 for head
        assertNotNull(result.getDeptName());
    }

    @Test
    void testTriage_StomachPain() {
        TriageRequest request = new TriageRequest();
        request.setBodyPart("腹部");
        request.setDescription("腹痛三天");

        TriageResult result = triageService.triage(request);

        assertNotNull(result);
        assertEquals("消化内科", result.getDeptName());
    }

    @Test
    void testTriage_EyeProblem() {
        TriageRequest request = new TriageRequest();
        request.setBodyPart("眼睛");
        request.setDescription("视力模糊");

        TriageResult result = triageService.triage(request);

        assertNotNull(result);
        assertEquals("眼科", result.getDeptName());
    }

    @Test
    void testTriage_ChildPatient() {
        TriageRequest request = new TriageRequest();
        request.setBodyPart(null);
        request.setDescription("儿童发热");

        TriageResult result = triageService.triage(request);

        assertNotNull(result);
        assertEquals("儿科", result.getDeptName());
    }

    @Test
    void testTriage_Default() {
        TriageRequest request = new TriageRequest();
        request.setBodyPart("未知");
        request.setDescription("不明症状");

        TriageResult result = triageService.triage(request);

        assertNotNull(result);
        assertEquals("内科", result.getDeptName()); // Default
    }
}
