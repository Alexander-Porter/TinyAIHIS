package com.tinyhis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Medical Knowledge Base for RAG
 * Loads and indexes medical JSON files for similarity search
 */
@Slf4j
@Component
public class MedicalKnowledgeBase {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Simple in-memory vector store using keyword matching
    // In production, use a proper vector database like Milvus or Pgvector
    private final Map<String, MedicalDocument> documents = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> keywordIndex = new ConcurrentHashMap<>();
    
    @Value("${medical.knowledge.path:classpath:medical-knowledge/}")
    private String knowledgePath;

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    private void loadKnowledgeBase() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(knowledgePath + "*.json");
            
            for (Resource resource : resources) {
                try {
                    loadDocument(resource);
                } catch (Exception e) {
                    log.warn("Failed to load document: {}", resource.getFilename(), e);
                }
            }
            
            log.info("Loaded {} medical documents into knowledge base", documents.size());
        } catch (IOException e) {
            log.warn("No medical knowledge files found at path: {}", knowledgePath);
        }
    }

    private void loadDocument(Resource resource) throws IOException {
        String filename = resource.getFilename();
        if (filename == null) return;
        
        // Extract disease name from filename (e.g., "2184_急性支气管炎.json")
        String diseaseName = filename.replaceAll("^\\d+_", "").replace(".json", "");
        
        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            
            MedicalDocument doc = new MedicalDocument();
            doc.setId(filename);
            doc.setDiseaseName(diseaseName);
            doc.setContent(extractContent(root));
            doc.setDepartment(inferDepartment(diseaseName, doc.getContent()));
            
            documents.put(doc.getId(), doc);
            
            // Build keyword index
            buildIndex(doc);
        }
    }

    private String extractContent(JsonNode root) {
        StringBuilder content = new StringBuilder();
        extractTextRecursively(root, content);
        return content.toString();
    }

    private void extractTextRecursively(JsonNode node, StringBuilder content) {
        if (node.isTextual()) {
            // Strip HTML tags for indexing
            String text = node.asText().replaceAll("<[^>]+>", " ");
            content.append(text).append(" ");
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                extractTextRecursively(child, content);
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if ("title".equals(entry.getKey()) || "detail".equals(entry.getKey()) 
                    || "content".equals(entry.getKey())) {
                    extractTextRecursively(entry.getValue(), content);
                }
            });
        }
    }

    private void buildIndex(MedicalDocument doc) {
        String text = doc.getDiseaseName() + " " + doc.getContent();
        // Simple keyword extraction - split by non-Chinese/non-alphanumeric characters
        String[] words = text.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");
        
        for (String word : words) {
            if (word.length() >= 2) {
                keywordIndex.computeIfAbsent(word.toLowerCase(), k -> new HashSet<>()).add(doc.getId());
            }
        }
    }

    private String inferDepartment(String diseaseName, String content) {
        // Infer department based on disease name and content
        String combined = diseaseName + " " + content;
        
        if (combined.contains("支气管") || combined.contains("肺") || combined.contains("呼吸")) {
            return "呼吸内科";
        } else if (combined.contains("胃") || combined.contains("肠") || combined.contains("消化")) {
            return "消化内科";
        } else if (combined.contains("心") || combined.contains("血压") || combined.contains("心脏")) {
            return "心内科";
        } else if (combined.contains("骨") || combined.contains("关节") || combined.contains("脊")) {
            return "骨科";
        } else if (combined.contains("皮肤") || combined.contains("疹") || combined.contains("痤疮")) {
            return "皮肤科";
        } else if (combined.contains("眼") || combined.contains("视力")) {
            return "眼科";
        } else if (combined.contains("儿童") || combined.contains("小儿") || combined.contains("新生儿")) {
            return "儿科";
        } else if (combined.contains("妇") || combined.contains("产") || combined.contains("月经")) {
            return "妇产科";
        } else if (combined.contains("神经") || combined.contains("头痛") || combined.contains("眩晕")) {
            return "神经内科";
        } else if (combined.contains("肾") || combined.contains("泌尿")) {
            return "泌尿外科";
        } else if (combined.contains("外伤") || combined.contains("手术")) {
            return "外科";
        }
        
        return "内科";
    }

    /**
     * Search for relevant documents based on query
     */
    public List<MedicalDocument> search(String query, int topK) {
        Map<String, Integer> scoreMap = new HashMap<>();
        
        // Tokenize query
        String[] queryWords = query.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");
        
        for (String word : queryWords) {
            if (word.length() >= 2) {
                Set<String> docIds = keywordIndex.get(word.toLowerCase());
                if (docIds != null) {
                    for (String docId : docIds) {
                        scoreMap.merge(docId, 1, Integer::sum);
                    }
                }
            }
        }
        
        // Also do partial matching for better recall
        for (String keyword : keywordIndex.keySet()) {
            for (String queryWord : queryWords) {
                if (queryWord.length() >= 2 && 
                    (keyword.contains(queryWord) || queryWord.contains(keyword))) {
                    Set<String> docIds = keywordIndex.get(keyword);
                    if (docIds != null) {
                        for (String docId : docIds) {
                            scoreMap.merge(docId, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        
        // Sort by score and return top-K
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topK)
                .map(e -> documents.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Get all loaded documents
     */
    public Collection<MedicalDocument> getAllDocuments() {
        return documents.values();
    }

    /**
     * Reload knowledge base
     */
    public void reload() {
        documents.clear();
        keywordIndex.clear();
        loadKnowledgeBase();
    }
}
