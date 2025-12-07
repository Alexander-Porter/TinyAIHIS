package com.tinyhis.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyhis.entity.*;
import com.tinyhis.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * RAG-based Triage Service using Spring AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagTriageService {

    private final MedicalKnowledgeBase knowledgeBase;
    
    private final PatientInfoMapper patientInfoMapper;
    private final RegistrationMapper registrationMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final LabOrderMapper labOrderMapper;
    private final DrugDictMapper drugDictMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
        private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    @Value("${triage.rag.enabled:true}")
    private boolean ragEnabled;
    
    @Value("${triage.rag.top-k:3}")
    private int topK;

    @Value("${siliconflow.api-key:}")
    private String siliconflowApiKey;

    @Value("${siliconflow.model:gpt-4o-mini}")
    private String siliconflowModel;

    /**
     * Stream triage recommendation with SSE
     */
    public void streamTriage(String symptoms, String bodyPart, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                if (!ragEnabled) {
                    TriageRecommendation rec = fallbackTriage(symptoms, bodyPart);
                    sendEvent(emitter, "message", rec.getReason());
                    sendEvent(emitter, "result", objectMapper.writeValueAsString(rec));
                    emitter.complete();
                    return;
                }

                sendEvent(emitter, "status", "正在分析病情...");
                
                String query = buildQuery(symptoms, bodyPart);
                List<MedicalDocument> relevantDocs = knowledgeBase.search(query, topK);
                
                if (!relevantDocs.isEmpty()) {
                    // Send structured tool call information
                    Map<String, Object> toolCall = new HashMap<>();
                    toolCall.put("name", "search_knowledge_base");
                    toolCall.put("query", query);
                    List<Map<String, String>> sources = relevantDocs.stream()
                        .map(doc -> {
                            Map<String, String> source = new HashMap<>();
                            source.put("disease", doc.getDiseaseName());
                            source.put("department", doc.getDepartment());
                            return source;
                        })
                        .toList();
                    toolCall.put("sources", sources);
                    sendEvent(emitter, "tool_call", objectMapper.writeValueAsString(toolCall));
                    
                    // Also send simple text for display
                    String docNames = relevantDocs.stream()
                        .map(doc -> String.format("%s (%s)", doc.getDiseaseName(), doc.getDepartment()))
                        .collect(Collectors.joining(", "));
                    sendEvent(emitter, "tool", "检索知识库: " + docNames);
                }
                
                String context = buildContext(relevantDocs);
                String prompt = buildPrompt(context, symptoms, bodyPart);
                
                callLLMStream(new Prompt(prompt), emitter, null);
                
            } catch (Exception e) {
                log.error("Streaming triage failed", e);
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * Stream Doctor Assistant with Patient Context and Chat History
     */
    public void streamDoctorAssist(Long patientId, String userQuery, String conversationId, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                // Generate conversationId if missing
                String chatId = (conversationId == null || conversationId.isEmpty()) 
                    ? UUID.randomUUID().toString() 
                    : conversationId;
                
                // Send chatId back to frontend
                sendEvent(emitter, "session", chatId);

                // 1. Build Patient Context
                String patientContext = buildPatientContext(patientId);
                
                // 2. Search Knowledge Base
                List<MedicalDocument> relevantDocs = knowledgeBase.search(userQuery, topK);
                
                // Send tool call information if knowledge found
                if (!relevantDocs.isEmpty()) {
                    Map<String, Object> toolCall = new HashMap<>();
                    toolCall.put("name", "search_knowledge_base");
                    toolCall.put("query", userQuery);
                    List<Map<String, String>> sources = relevantDocs.stream()
                        .map(doc -> {
                            Map<String, String> source = new HashMap<>();
                            source.put("disease", doc.getDiseaseName());
                            source.put("department", doc.getDepartment());
                            return source;
                        })
                        .toList();
                    toolCall.put("sources", sources);
                    sendEvent(emitter, "tool_call", objectMapper.writeValueAsString(toolCall));
                    
                    String docNames = relevantDocs.stream()
                        .map(doc -> String.format("%s (%s)", doc.getDiseaseName(), doc.getDepartment()))
                        .collect(Collectors.joining(", "));
                    sendEvent(emitter, "tool", "检索知识库: " + docNames);
                }
                
                String knowledgeContext = buildContext(relevantDocs);
                
                // 3. Build System Prompt
                String systemPrompt = buildDoctorSystemPrompt(patientContext, knowledgeContext);
                
                // 4. Load History
                List<Message> history = loadChatHistory(chatId);
                
                // 5. Construct Prompt
                List<Message> messages = new ArrayList<>();
                messages.add(new SystemMessage(systemPrompt));
                messages.addAll(history);
                messages.add(new UserMessage(userQuery));
                
                Prompt prompt = new Prompt(messages);
                
                // 6. Stream
                callLLMStreamWithHistory(prompt, emitter, chatId, userQuery);
                
            } catch (Exception e) {
                log.error("Doctor assist failed", e);
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * Stream SiliconFlow OpenAI-compatible SSE (includes reasoning_content if provided)
     */
    private void streamSiliconflow(Prompt prompt, SseEmitter emitter, String chatId, String userQuery, Runnable onComplete) {
        if (siliconflowApiKey == null || siliconflowApiKey.isEmpty()) {
            emitter.completeWithError(new IllegalStateException("SILICONFLOW_API_KEY is missing"));
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", siliconflowModel);
            body.put("stream", true);
            body.put("messages", toOpenAiMessages(prompt));
            // Ask backend to include reasoning (provider-dependent; siliconflow supports OpenAI compatible fields)
            body.put("reasoning", true);

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.siliconflow.cn/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + siliconflowApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2 || response.body() == null) {
                emitter.completeWithError(new IllegalStateException("SiliconFlow HTTP " + response.statusCode()));
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
            String line;
            StringBuilder fullResponse = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (!line.startsWith("data:")) continue;

                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;

                try {
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(data);
                    com.fasterxml.jackson.databind.JsonNode choices = node.path("choices");
                    if (!choices.isArray() || choices.isEmpty()) continue;
                    com.fasterxml.jackson.databind.JsonNode delta = choices.get(0).path("delta");

                    String reasoning = delta.path("reasoning_content").asText("");
                    if (!reasoning.isEmpty()) {
                        sendEvent(emitter, "thought", reasoning);
                    }

                    String content = delta.path("content").asText("");
                    if (!content.isEmpty()) {
                        fullResponse.append(content);
                        sendEvent(emitter, "message", content);
                    }
                } catch (Exception parseEx) {
                    log.error("Failed to parse SiliconFlow chunk", parseEx);
                }
            }

            if (chatId != null && userQuery != null) {
                saveChatHistory(chatId, userQuery, fullResponse.toString());
            }
            if (onComplete != null) onComplete.run();
            emitter.complete();

        } catch (Exception e) {
            log.error("SiliconFlow streaming failed", e);
            emitter.completeWithError(e);
        }
    }

    private List<Map<String, Object>> toOpenAiMessages(Prompt prompt) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Message m : prompt.getInstructions()) {
            if (m instanceof SystemMessage sm) {
                list.add(Map.of("role", "system", "content", sm.getText()));
            } else if (m instanceof UserMessage um) {
                list.add(Map.of("role", "user", "content", um.getText()));
            } else if (m instanceof AssistantMessage am) {
                list.add(Map.of("role", "assistant", "content", am.getText()));
            }
        }
        return list;
    }

    private String buildQuery(String symptoms, String bodyPart) {
        StringBuilder query = new StringBuilder();
        if (bodyPart != null && !bodyPart.isEmpty()) {
            query.append(bodyPart).append(" ");
        }
        if (symptoms != null && !symptoms.isEmpty()) {
            query.append(symptoms);
        }
        return query.toString().trim();
    }

    private String buildContext(List<MedicalDocument> docs) {
        if (docs.isEmpty()) {
            return "暂无相关医学知识参考。";
        }
        return docs.stream()
                .map(doc -> String.format("【%s】\n科室: %s\n%s", 
                        doc.getDiseaseName(), 
                        doc.getDepartment(),
                        truncateContent(doc.getContent(), 500)))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    private String buildPrompt(String context, String symptoms, String bodyPart) {
        return """
            你是一位专业的医院导诊助手。请根据以下医学知识参考和患者描述，推荐最合适的就诊科室。
            
            【医学知识参考】
            %s
            
            【患者描述】
            - 症状: %s
            - 部位: %s
            
            请先进行思考分析，然后给出推荐科室。格式如下：
            思考过程：[你的分析过程]
            科室：[科室名称]
            理由：[简短理由]
            可能疾病：[可能的疾病名称]
            """.formatted(
                context,
                symptoms != null ? symptoms : "未提供",
                bodyPart != null ? bodyPart : "未提供");
    }
    
    private String buildDoctorSystemPrompt(String patientContext, String knowledgeContext) {
        return """
            你是一名经验丰富的医生助手。请根据以下患者信息、病历记录和医学参考资料，为医生提供诊断建议和治疗方案。
            
            【患者信息】
            %s
            
            【医学参考资料】
            %s
            
            请保持专业、客观。如果信息不足，请提示医生补充。
            """.formatted(patientContext, knowledgeContext);
    }

    private String buildPatientContext(Long patientId) {
        if (patientId == null) return "未指定患者";
        
        PatientInfo patient = patientInfoMapper.selectById(patientId);
        if (patient == null) return "患者不存在";
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("姓名：%s，性别：%s，年龄：%d岁\n", 
            patient.getName(),
            patient.getGender() == 1 ? "男" : "女", 
            patient.getAge()));
            
        // Current consultation (status=3) - show separately, not mixed into history/recent meds
        Registration currentReg = registrationMapper.selectOne(
            new LambdaQueryWrapper<Registration>()
                .eq(Registration::getPatientId, patientId)
                .eq(Registration::getStatus, 3)
                .orderByDesc(Registration::getCreateTime)
                .last("LIMIT 1"));

        Long currentRecordId = null;
        if (currentReg != null) {
            sb.append("\n【本次就诊】\n");
            MedicalRecord currentRecord = medicalRecordMapper.selectOne(
                new LambdaQueryWrapper<MedicalRecord>()
                    .eq(MedicalRecord::getPatientId, patientId)
                    .eq(MedicalRecord::getRegId, currentReg.getRegId())
                    .orderByDesc(MedicalRecord::getCreateTime)
                    .last("LIMIT 1"));
            if (currentRecord != null) {
                currentRecordId = currentRecord.getRecordId();
                sb.append(String.format("- 主诉：%s\n- 诊断：%s\n- 现病史：%s\n", 
                    safe(currentRecord.getSymptom()),
                    safe(currentRecord.getDiagnosis()),
                    safe(currentRecord.getContent())));
            }

            // Current lab orders
            if (currentRecordId != null) {
                List<LabOrder> currentLabs = labOrderMapper.selectList(
                    new LambdaQueryWrapper<LabOrder>()
                        .eq(LabOrder::getRecordId, currentRecordId)
                        .orderByDesc(LabOrder::getCreateTime));
                if (!currentLabs.isEmpty()) {
                    sb.append("- 本次检查：\n");
                    currentLabs.forEach(l -> sb.append(String.format("  • %s [%s]%s\n",
                        safe(l.getItemName()),
                        labStatusText(l.getStatus()),
                        (l.getResultText() != null && !l.getResultText().isEmpty()) ? " 结果：" + l.getResultText() : "")));
                }

                // Current prescriptions
                List<Prescription> currentPres = prescriptionMapper.selectList(
                    new LambdaQueryWrapper<Prescription>()
                        .eq(Prescription::getRecordId, currentRecordId)
                        .orderByDesc(Prescription::getCreateTime));
                if (!currentPres.isEmpty()) {
                    sb.append("- 本次用药：\n");
                    currentPres.forEach(p -> {
                        DrugDict drug = drugDictMapper.selectById(p.getDrugId());
                        String drugName = (drug != null) ? drug.getName() : "未知药物";
                        sb.append(String.format("  • %s x%d (%s)\n", drugName, p.getQuantity(), p.getUsageInstruction()));
                    });
                }
            }
        }

        // Historical medical records (exclude current record)
        List<MedicalRecord> records = medicalRecordMapper.selectList(
            new QueryWrapper<MedicalRecord>().eq("patient_id", patientId)
                .orderByDesc("create_time").last("LIMIT 5"));
        if (currentRecordId != null) {
            final Long currentRecId = currentRecordId;
            records = records.stream()
                .filter(r -> !Objects.equals(r.getRecordId(), currentRecId))
                .toList();
        }
                
        if (!records.isEmpty()) {
            sb.append("\n【历史病历】\n");
            records.stream().limit(3).forEach(r ->
                sb.append(String.format("- %s 诊断：%s\n  主诉：%s\n", 
                    r.getCreateTime().toLocalDate(), r.getDiagnosis(), r.getSymptom()))
            );
        }
        
        // Recent Prescriptions from historical records only
        List<Long> recordIds = records.stream()
            .map(MedicalRecord::getRecordId)
            .filter(Objects::nonNull)
            .toList();
        if (!recordIds.isEmpty()) {
            List<Prescription> prescriptions = prescriptionMapper.selectList(
                new QueryWrapper<Prescription>()
                    .in("record_id", recordIds)
                    .orderByDesc("create_time")
                    .last("LIMIT 10"));
            
            if (!prescriptions.isEmpty()) {
                sb.append("\n【近期用药】\n");
                for (Prescription p : prescriptions) {
                    DrugDict drug = drugDictMapper.selectById(p.getDrugId());
                    String drugName = (drug != null) ? drug.getName() : "未知药物";
                    sb.append(String.format("- %s x%d (%s)\n", drugName, p.getQuantity(), p.getUsageInstruction()));
                }
            }
        }
        
        return sb.toString();
    }

    private void callLLMStream(Prompt prompt, SseEmitter emitter, Runnable onComplete) {
        streamSiliconflow(prompt, emitter, null, null, onComplete);
    }
    
    private void callLLMStreamWithHistory(Prompt prompt, SseEmitter emitter, String chatId, String userQuery) {
        streamSiliconflow(prompt, emitter, chatId, userQuery, null);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            String encodedData = Base64.getEncoder().encodeToString(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            emitter.send(SseEmitter.event().name(name).data(encodedData));
        } catch (IOException e) {
            log.error("Error sending SSE event", e);
        }
    }

    private String safe(String val) {
        return val == null ? "" : val;
    }

    private String labStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待缴费";
            case 1 -> "待检查";
            case 2 -> "已完成";
            default -> "未知";
        };
    }

    private TriageRecommendation fallbackTriage(String symptoms, String bodyPart) {
        TriageRecommendation rec = new TriageRecommendation();
        String query = buildQuery(symptoms, bodyPart);
        
        if (query.contains("咳嗽") || query.contains("感冒") || query.contains("发烧") || query.contains("呼吸")) {
            rec.setDepartment("呼吸内科");
            rec.setDeptId(1L);
        } else if (query.contains("胃") || query.contains("腹") || query.contains("消化") || query.contains("肠")) {
            rec.setDepartment("消化内科");
            rec.setDeptId(3L);
        } else if (query.contains("心") || query.contains("胸闷")) {
            rec.setDepartment("心内科");
            rec.setDeptId(4L);
        } else if (query.contains("骨") || query.contains("关节") || query.contains("腰") || query.contains("腿")) {
            rec.setDepartment("骨科");
            rec.setDeptId(5L);
        } else if (query.contains("儿童") || query.contains("小孩")) {
            rec.setDepartment("儿科");
            rec.setDeptId(6L);
        } else if (query.contains("妇") || query.contains("月经") || query.contains("怀孕")) {
            rec.setDepartment("妇产科");
            rec.setDeptId(7L);
        } else if (query.contains("眼")) {
            rec.setDepartment("眼科");
            rec.setDeptId(8L);
        } else {
            rec.setDepartment("内科");
            rec.setDeptId(1L);
        }
        
        rec.setReason("根据您的症状描述，建议您到" + rec.getDepartment() + "就诊");
        return rec;
    }
    
    private List<Message> loadChatHistory(String conversationId) {
        List<String> historyJson = redisTemplate.opsForList().range("chat:history:" + conversationId, 0, -1);
        if (historyJson == null || historyJson.isEmpty()) return new ArrayList<>();
        
        List<Message> messages = new ArrayList<>();
        for (String json : historyJson) {
            try {
                Map<String, String> map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
                String role = map.get("role");
                String content = map.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                }
            } catch (Exception e) {
                log.error("Failed to parse chat history", e);
            }
        }
        return messages;
    }

    private void saveChatHistory(String conversationId, String userQuery, String aiResponse) {
        try {
            Map<String, String> userMsg = Map.of("role", "user", "content", userQuery);
            Map<String, String> aiMsg = Map.of("role", "assistant", "content", aiResponse);
            
            redisTemplate.opsForList().rightPushAll("chat:history:" + conversationId, 
                objectMapper.writeValueAsString(userMsg),
                objectMapper.writeValueAsString(aiMsg));
            
            redisTemplate.expire("chat:history:" + conversationId, Duration.ofHours(24));
        } catch (Exception e) {
            log.error("Failed to save chat history", e);
        }
    }
    
        public TriageRecommendation triage(String symptoms, String bodyPart) {
            throw new UnsupportedOperationException("Synchronous triage is deprecated. Use streamTriage.");
        }
        
    /**
     * Search knowledge base and return structured results for global text search.
     * This method is intended for on-demand searches from the UI (e.g., text selection search)
     * rather than RAG context retrieval. It uses the default hybrid search with
     * keyword and vector matching.
     * 
     * @param query Search query text
     * @param limit Maximum number of results to return
     * @return List of matching documents with content
     */
    public List<MedicalDocument> searchKnowledge(String query, int limit) {
        if (limit <= 0) limit = 5;
        return knowledgeBase.search(query, limit);
    }
}
