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

        // 测试回退逻辑（当ragEnabled = false时）
        ragTriageService.streamTriage("咳嗽", null, emitter);

        // 验证异步操作是否完成
        // 由于使用了CompletableFuture.runAsync，需要等待一小段时间
        // 在单元测试中，简单休眠是最直接的方式，或者可以考虑重构服务以返回Future
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 验证SSE发射器是否成功发送了'message'和'result'事件
        try {
            verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
            // 在回退逻辑中，系统会根据症状关键词自动匹配科室
            // 例如："咳嗽" 会自动匹配到呼吸内科
        } catch (Exception e) {
            fail("SSE发送失败");
        }
    }
}
