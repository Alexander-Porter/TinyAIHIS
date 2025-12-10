package com.tinyhis.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagTriageServiceTest {

    @InjectMocks
    private RagTriageService ragTriageService;

    @Mock
    private MedicalKnowledgeBase knowledgeBase;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ragTriageService, "ragEnabled", false);
        ReflectionTestUtils.setField(ragTriageService, "objectMapper", objectMapper);
    }

    @Test
    void testFallbackTriage() {
        SseEmitter emitter = mock(SseEmitter.class);

        // Test fallback (ragEnabled = false)
        ragTriageService.streamTriage("咳嗽", null, emitter);

        // Verify that we complete async
        // Since it's CompletableFuture.runAsync, we need to wait a tiny bit or verify
        // strictly if we could control executor.
        // For unit test, simple sleep is easiest or refactor service to return Future.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify emitter sent 'message' and 'result'
        try {
            verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
            // In fallback, we set department based on keyword.
            // "咳嗽" -> 呼吸内科
        } catch (Exception e) {
            fail("SSE send failed");
        }
    }
}
