package com.tinyhis.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyhis.entity.*;
import com.tinyhis.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    @Value("${siliconflow.model:deepseek-ai/DeepSeek-V3.2}")
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
                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(systemMessage(buildTriageSystemPrompt()));
                messages.add(userMessage(buildUserDescription(symptoms, bodyPart)));

                chatWithTools(messages, emitter, null, null, 0, new StringBuilder());
                
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
                sendEvent(emitter, "status", "正在准备患者信息...");

                String patientContext = buildPatientContext(patientId);
                List<Map<String, Object>> history = loadChatHistory(chatId);

                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(systemMessage(buildDoctorSystemPrompt(patientContext)));
                messages.addAll(history);
                messages.add(userMessage(userQuery));

                chatWithTools(messages, emitter, chatId, userQuery, 0, new StringBuilder());
                
            } catch (Exception e) {
                log.error("Doctor assist failed", e);
                emitter.completeWithError(e);
            }
        });
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



    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }


    private String buildDoctorSystemPrompt(String patientContext) {
        return """
            你是一名经验丰富的医生助手。请根据以下患者信息、病历记录和医学参考资料，为医生提供诊断建议和治疗方案。

            你可以调用函数 search_knowledge_base 来检索相关疾病和科室信息，必要时请先调用该工具再回答。
            回答需要简洁、专业，并在信息不足时指出需要补充的检查或信息。

            【患者信息】
            %s
            """.formatted(patientContext);
    }

    private String buildTriageSystemPrompt() {
        return """
            你是一名医院导诊助手，目标是根据患者症状推荐合适的就诊科室。
            如果需要医学知识，请调用 search_knowledge_base 函数获取相关疾病和科室信息，然后基于检索结果给出推荐。
            输出包含：
            - 思考过程：简要分析
            - 科室：推荐科室
            - 理由：简短理由
            - 可能疾病：列出可能疾病
            """;
    }

    private String buildUserDescription(String symptoms, String bodyPart) {
        return """
            症状：%s
            部位：%s
            请根据需要调用 search_knowledge_base 获取知识库信息。
            """.formatted(
                symptoms != null ? symptoms : "未提供",
                bodyPart != null ? bodyPart : "未提供");
    }

    private Map<String, Object> systemMessage(String content) {
        return Map.of("role", "system", "content", content);
    }

    private Map<String, Object> userMessage(String content) {
        return Map.of("role", "user", "content", content);
    }

    private List<Map<String, Object>> buildTools() {
        Map<String, Object> searchFunction = Map.of(
            "name", "search_knowledge_base",
            "description", "检索医学知识库，返回相关疾病与科室信息。",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of(
                        "type", "string",
                        "description", "症状或问题描述，用于检索知识库"
                    ),
                    "top_k", Map.of(
                        "type", "integer",
                        "description", "返回的结果数量",
                        "default", topK
                    )
                ),
                "required", List.of("query")
            )
        );

        return List.of(Map.of(
            "type", "function",
            "function", searchFunction
        ));
    }

    private void chatWithTools(List<Map<String, Object>> messages, SseEmitter emitter, String chatId, String userQuery, int depth, StringBuilder finalResponse) {


        if (siliconflowApiKey == null || siliconflowApiKey.isEmpty()) {
            emitter.completeWithError(new IllegalStateException("SILICONFLOW_API_KEY is missing"));
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", siliconflowModel);
            body.put("stream", true);
            body.put("messages", messages);
            body.put("reasoning", true);
            body.put("tools", buildTools());
            System.out.println("SiliconFlow Chat Request: " + objectMapper.writeValueAsString(body));
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
            Map<String, ToolCallState> toolCalls = new LinkedHashMap<>();
            StringBuilder stageContent = new StringBuilder();
            String finishReason = null;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || !line.startsWith("data:")) continue;

                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;

                try {
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(data);
                    com.fasterxml.jackson.databind.JsonNode choices = node.path("choices");
                    if (!choices.isArray() || choices.isEmpty()) continue;
                    com.fasterxml.jackson.databind.JsonNode choice = choices.get(0);
                    com.fasterxml.jackson.databind.JsonNode delta = choice.path("delta");
                    log.info(choices.toString());
                    String reasoning = delta.path("reasoning_content").asText("");
                    if (!reasoning.isEmpty()) {
                        sendEvent(emitter, "thought", reasoning);
                    }

                    String content = delta.path("content").asText("");
                    if (!content.isEmpty()) {
                        stageContent.append(content);
                        sendEvent(emitter, "message", content);
                    }

                        com.fasterxml.jackson.databind.JsonNode toolCallNodes = delta.path("tool_calls");
                        if (toolCallNodes.isArray()) {
                            log.info("Tool call delta: {}", toolCallNodes.toString());
                            for (com.fasterxml.jackson.databind.JsonNode call : toolCallNodes) {
                                int index = call.path("index").asInt(-1);
                                String idField = call.path("id").asText("");

                                // Use index as primary accumulator key; id is captured but not used for grouping
                                String key;
                                if (index >= 0) {
                                    key = "idx-" + index;
                                } else if (!idField.isEmpty()) {
                                    key = idField;
                                } else {
                                    key = UUID.randomUUID().toString();
                                }

                                ToolCallState state = toolCalls.computeIfAbsent(key, k -> new ToolCallState(key, index));
                                if (state.modelId == null && !idField.isEmpty()) {
                                    state.modelId = idField;
                                }

                                String name = call.path("function").path("name").asText(null);
                                if (name != null && (state.name == null || state.name.isEmpty())) {
                                    state.name = name;
                                }
                                String arguments = call.path("function").path("arguments").asText("");
                                if (!arguments.isEmpty()) {
                                    state.arguments.append(arguments);
                                }
                                log.info("Accumulated tool_call key={}, modelId={}, name={}, idx={}, args_so_far='{}'", key, state.modelId, state.name, state.index, state.arguments);
                            }
                        }

                    String fr = choice.path("finish_reason").asText(null);
                    if (fr != null && !fr.isEmpty()) {
                        finishReason = fr;
                    }
                } catch (Exception parseEx) {
                    log.error("Failed to parse SiliconFlow chunk", parseEx);
                }
            }

            if (finishReason == null && !toolCalls.isEmpty()) {
                finishReason = "tool_calls";
            }

            if ("tool_calls".equals(finishReason) && !toolCalls.isEmpty()) {
                List<Map<String, Object>> nextMessages = new ArrayList<>(messages);
                log.info("Tool calls to execute ({}): {}", toolCalls.size(), toolCalls);
                nextMessages.add(assistantToolCallMessage(stageContent.toString(), toolCalls.values()));

                for (ToolCallState state : toolCalls.values()) {
                    log.info("Executing tool: {} with args: {} (key={}, idx={}, modelId={})", state.name, state.arguments, state.id, state.index, state.modelId);
                    Map<String, Object> toolMessage = executeSearchTool(state, emitter);
                    if (toolMessage != null) {
                        nextMessages.add(toolMessage);
                    }
                }

                chatWithTools(nextMessages, emitter, chatId, userQuery, depth + 1, finalResponse);
                return;
            }

            finalResponse.append(stageContent);
            if (chatId != null && userQuery != null && finalResponse.length() > 0) {
                saveChatHistory(chatId, userQuery, finalResponse.toString());
            }
            emitter.complete();

        } catch (Exception e) {
            log.error("SiliconFlow streaming failed", e);
            emitter.completeWithError(e);
        }
    }

    private Map<String, Object> assistantToolCallMessage(String content, Collection<ToolCallState> toolCalls) {
        List<Map<String, Object>> calls = toolCalls.stream()
            .map(state -> Map.of(
                "id", state.effectiveId(),
                "type", "function",
                "function", Map.of(
                    "name", state.name != null ? state.name : "search_knowledge_base",
                    "arguments", state.arguments.toString()
                )
            ))
            .toList();

        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("tool_calls", calls);
        if (!content.isEmpty()) {
            message.put("content", content);
        }
        return message;
    }

    private Map<String, Object> executeSearchTool(ToolCallState state, SseEmitter emitter) {
        String argsJson = state.arguments.toString();
        Map<String, Object> args = parseArgs(argsJson);
        String query = args.getOrDefault("query", "").toString();
        int limit = parseTopK(args.get("top_k"));
        if (limit <= 0) limit = topK;

        List<MedicalDocument> docs = query.isEmpty() ? List.of() : knowledgeBase.search(query, limit);
        if (!docs.isEmpty()) {
            Map<String, Object> toolCall = new HashMap<>();
            toolCall.put("name", state.name != null ? state.name : "search_knowledge_base");
            toolCall.put("query", query);
            List<Map<String, String>> sources = docs.stream()
                .map(doc -> {
                    Map<String, String> source = new HashMap<>();
                    source.put("disease", doc.getDiseaseName());
                    source.put("department", doc.getDepartment());
                    return source;
                })
                .toList();
            toolCall.put("sources", sources);
            sendEvent(emitter, "tool_call", safeJson(toolCall));



        } 

        Map<String, Object> toolMessage = new HashMap<>();
        toolMessage.put("role", "tool");
        toolMessage.put("tool_call_id", state.effectiveId());
        toolMessage.put("name", state.name != null ? state.name : "search_knowledge_base");
        toolMessage.put("content", buildToolResultContent(query, docs));
        return toolMessage;
    }

    private Map<String, Object> parseArgs(String argsJson) {
        if (argsJson == null) return new HashMap<>();

        String raw = argsJson.trim();
        if (raw.isEmpty()) return new HashMap<>();

        // Handle simple numeric payloads like "3" meaning only top_k was provided
        if (raw.matches("^-?\\d+$")) {
            return Map.of("top_k", Integer.parseInt(raw));
        }

        // Ensure arguments are a JSON object; if the model streamed partial braces, try to coerce
        if (!raw.startsWith("{") && !raw.startsWith("[")) {
            // Sometimes OpenAI-style tools send a JSON string of object; try quoting and parsing
            raw = "{" + raw + "}";
        }

        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tool arguments: {}", raw, e);

            // Fallback: treat the raw (or inner) string as a direct query
            String inner = raw;
            if (raw.length() >= 2 && raw.startsWith("{") && raw.endsWith("}")) {
                inner = raw.substring(1, raw.length() - 1).trim();
            }
            inner = trimWrappingQuotes(inner);
            if (inner.isEmpty()) return new HashMap<>();

            return Map.of("query", inner);
        }
    }

    private String trimWrappingQuotes(String val) {
        if (val == null) return "";
        String out = val.trim();
        if (out.length() >= 2) {
            boolean doubleQuoteWrapped = out.startsWith("\"") && out.endsWith("\"");
            boolean singleQuoteWrapped = out.startsWith("'") && out.endsWith("'");
            if (doubleQuoteWrapped || singleQuoteWrapped) {
                out = out.substring(1, out.length() - 1).trim();
            }
        }
        return out;
    }

    private int parseTopK(Object val) {
        if (val == null) return -1;
        if (val instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String buildToolResultContent(String query, List<MedicalDocument> docs) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("results", docs.stream()
            .map(doc -> Map.of(
                "disease", doc.getDiseaseName(),
                "department", doc.getDepartment(),
                "summary", truncateContent(doc.getContent(), 300)
            ))
            .toList());
        return safeJson(payload);
    }

    private String safeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize payload", e);
            return "{}";
        }
    }

    private static class ToolCallState {
        private final String id;
        private final int index;
        private String modelId; // id provided by the model (first chunk)
        private String name;
        private final StringBuilder arguments = new StringBuilder();

        ToolCallState(String id, int index) {
            this.id = id;
            this.index = index;
        }

        String effectiveId() {
            if (modelId != null && !modelId.isEmpty()) {
                return modelId;
            }
            return id;
        }
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
    
    private List<Map<String, Object>> loadChatHistory(String conversationId) {
        List<String> historyJson = redisTemplate.opsForList().range("chat:history:" + conversationId, 0, -1);
        if (historyJson == null || historyJson.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> messages = new ArrayList<>();
        for (String json : historyJson) {
            try {
                Map<String, String> map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
                String role = map.get("role");
                String content = map.get("content");
                if ("user".equals(role) || "assistant".equals(role)) {
                    messages.add(Map.of("role", role, "content", content));
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
