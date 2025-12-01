package com.tinyhis.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG-based Triage Service using SiliconFlow API (OpenAI compatible)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagTriageService {

    private final MedicalKnowledgeBase knowledgeBase;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${triage.rag.enabled:true}")
    private boolean ragEnabled;
    
    @Value("${triage.rag.top-k:3}")
    private int topK;
    
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.siliconflow.cn/v1}")
    private String baseUrl;
    
    @Value("${spring.ai.openai.chat.options.model:Qwen/Qwen2.5-7B-Instruct}")
    private String model;

    /**
     * Perform RAG-based triage
     * @param symptoms Patient symptoms description
     * @param bodyPart Body part affected (optional)
     * @return Triage recommendation with department suggestion
     */
    public TriageRecommendation triage(String symptoms, String bodyPart) {
        if (!ragEnabled || apiKey == null || apiKey.isEmpty()) {
            return fallbackTriage(symptoms, bodyPart);
        }

        try {
            // Build query from symptoms and body part
            String query = buildQuery(symptoms, bodyPart);
            
            // Retrieve relevant documents
            List<MedicalDocument> relevantDocs = knowledgeBase.search(query, topK);
            
            // Build context from retrieved documents
            String context = buildContext(relevantDocs);
            
            // Generate recommendation using LLM
            String prompt = buildPrompt(context, symptoms, bodyPart);
            
            String response = callLLM(prompt);
            
            return parseResponse(response, relevantDocs);
            
        } catch (Exception e) {
            log.error("RAG triage failed, falling back to rule-based", e);
            return fallbackTriage(symptoms, bodyPart);
        }
    }
    
    private String callLLM(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(message));
        body.put("temperature", 0.7);
        body.put("max_tokens", 500);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/chat/completions",
            request,
            Map.class
        );
        
        if (response.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.getFirst();
                Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                if (messageObj != null) {
                    return (String) messageObj.get("content");
                }
            }
        }
        
        throw new RuntimeException("Failed to get LLM response");
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
            
            请直接给出推荐科室和简短的推荐理由。格式如下：
            科室：[科室名称]
            理由：[简短理由，不超过50字]
            可能疾病：[可能的疾病名称，如有多个用逗号分隔]
            """.formatted(
                context,
                symptoms != null ? symptoms : "未提供",
                bodyPart != null ? bodyPart : "未提供");
    }

    private TriageRecommendation parseResponse(String response, List<MedicalDocument> docs) {
        TriageRecommendation rec = new TriageRecommendation();
        
        // Parse department
        if (response.contains("科室：") || response.contains("科室:")) {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.startsWith("科室：") || line.startsWith("科室:")) {
                    rec.setDepartment(line.replaceFirst("科室[：:]\\s*", "").trim());
                } else if (line.startsWith("理由：") || line.startsWith("理由:")) {
                    rec.setReason(line.replaceFirst("理由[：:]\\s*", "").trim());
                } else if (line.startsWith("可能疾病：") || line.startsWith("可能疾病:")) {
                    rec.setPossibleDiseases(line.replaceFirst("可能疾病[：:]\\s*", "").trim());
                }
            }
        } else {
            // Fallback parsing
            rec.setDepartment(inferDepartmentFromDocs(docs));
            rec.setReason(response.length() > 100 ? response.substring(0, 100) : response);
        }
        
        // Set department ID based on name
        rec.setDeptId(getDeptId(rec.getDepartment()));
        rec.setReferenceDocs(docs.stream().map(MedicalDocument::getDiseaseName).toList());
        
        return rec;
    }

    private String inferDepartmentFromDocs(List<MedicalDocument> docs) {
        if (docs.isEmpty()) {
            return "内科";
        }
        // Use the department from the most relevant document
        return docs.getFirst().getDepartment();
    }

    private Long getDeptId(String deptName) {
        if (deptName == null) return 1L;
        
        return switch (deptName) {
            case "内科", "呼吸内科", "神经内科" -> 1L;
            case "外科" -> 2L;
            case "消化内科" -> 3L;
            case "心内科" -> 4L;
            case "骨科" -> 5L;
            case "儿科" -> 6L;
            case "妇产科" -> 7L;
            case "眼科" -> 8L;
            case "皮肤科" -> 9L;
            default -> 1L;
        };
    }

    private TriageRecommendation fallbackTriage(String symptoms, String bodyPart) {
        TriageRecommendation rec = new TriageRecommendation();
        String query = buildQuery(symptoms, bodyPart);
        
        // Simple rule-based fallback
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
}
