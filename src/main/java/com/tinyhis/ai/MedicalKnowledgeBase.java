package com.tinyhis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.VectorUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Medical Knowledge Base for RAG
 * - Loads medical JSON files
 * - Embeds with SiliconFlow BAAI/bge-m3 (1024-d, ~8192 context)
 * - Persists vectors in a lightweight Lucene HNSW index on disk
 */
@Slf4j
@Component
public class MedicalKnowledgeBase {

    private final Map<String, MedicalDocument> documents = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${medical.knowledge.path:file:./medical-knowledge/}")
    private String knowledgePath;

    @Value("${medical.knowledge.vector-path:file:./vector-index/}")
    private String vectorIndexPath;

    @Value("${siliconflow.embedding.model:BAAI/bge-m3}")
    private String embeddingModel;

    @Value("${siliconflow.embedding.url:https://api.siliconflow.cn/v1/embeddings}")
    private String embeddingUrl;

    @Value("${SILICONFLOW_API_KEY:}")
    private String apiKey;

    @Value("${medical.search.keyword-weight:0.3}")
    private float defaultKeywordWeight;

    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
    private Analyzer analyzer;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private ExecutorService embeddingExecutor;
    private final ScheduledExecutorService rateRefillScheduler = new ScheduledThreadPoolExecutor(1);
    private final Semaphore rpmLimiter = new Semaphore(10); // conservative QPS << 2000 RPM
    private final AtomicInteger tokensThisWindow = new AtomicInteger(0);
    private volatile long tokenWindowStartMs = System.currentTimeMillis();

    @PostConstruct
    public void init() {
        try {
            openVectorIndex();
            loadKnowledgeBase();
        } catch (Exception e) {
            log.error("Failed to initialize knowledge base", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (searcherManager != null) {
                searcherManager.close();
            }
            if (indexWriter != null) {
                indexWriter.close();
            }
        } catch (IOException e) {
            log.warn("Error closing vector index", e);
        }
        if (embeddingExecutor != null) {
            embeddingExecutor.shutdownNow();
        }
        rateRefillScheduler.shutdownNow();
    }

    public List<MedicalDocument> getAllDocuments(String keyword, String department) {
        return documents.values().stream()
                .filter(d -> (keyword == null || d.getDiseaseName().contains(keyword)))
                .filter(d -> (department == null || department.isEmpty() || d.getDepartment().equals(department)))
                .map(this::copyMetadata)
                .toList();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documents.size());

        // Count by department
        Map<String, Long> deptCounts = documents.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(MedicalDocument::getDepartment,
                        java.util.stream.Collectors.counting()));
        stats.put("departmentCounts", deptCounts);

        // Vector index stats
        try {
            if (indexWriter != null) {
                stats.put("indexedDocuments", indexWriter.getDocStats().numDocs);
            }
        } catch (Exception e) {
            stats.put("indexedDocuments", 0);
        }

        return stats;
    }

    public MedicalDocument getDocument(String id) {
        MedicalDocument meta = documents.get(id);
        if (meta == null)
            return null;
        String content = loadContent(meta.getId(), meta.getContent());
        return copyWithContent(meta, content);
    }

    public void addDocument(MedicalDocument doc) {
        boolean existsOnDisk = Files.exists(Path.of(stripFileScheme(ensureTrailingSlash(knowledgePath)), doc.getId()));
        // Keep content only for docs not backed by disk to allow future retrieval
        String cachedContent = existsOnDisk ? null : doc.getContent();
        documents.put(doc.getId(), copyWithContent(doc, cachedContent));
        ensureVectorIndexed(doc, doc.getContent());
    }

    public void deleteDocument(String id) {
        documents.remove(id);
        try {
            if (indexWriter != null) {
                indexWriter.deleteDocuments(new Term("id", id));
                indexWriter.commit();
                searcherManager.maybeRefresh();
            }
        } catch (IOException e) {
            log.warn("Failed to delete vector for {}", id, e);
        }
    }

    /**
     * Search using vector similarity; returns topK docs or empty on failure.
     */
    public List<MedicalDocument> search(String query, int topK) {
        return hybridSearch(query, topK, defaultKeywordWeight);
    }

    /**
     * Hybrid search combining keyword and vector search with configurable weights
     * 
     * @param query         Search query
     * @param topK          Number of results
     * @param keywordWeight Weight for keyword search (0.0-1.0), remaining weight
     *                      goes to vector
     * @return List of matched documents with relevance scoring
     */
    public List<MedicalDocument> hybridSearch(String query, int topK, float keywordWeight) {
        if (searcherManager == null)
            return List.of();

        try {
            searcherManager.maybeRefresh();
            IndexSearcher searcher = searcherManager.acquire();
            try {
                Map<String, Float> docScores = new HashMap<>();

                // 1. Keyword search
                if (keywordWeight > 0) {
                    List<ScoredDoc> keywordResults = keywordSearch(searcher, query, topK * 2);
                    float maxKeywordScore = keywordResults.isEmpty() ? 1.0f : keywordResults.get(0).score;
                    for (ScoredDoc sd : keywordResults) {
                        String id = sd.id;
                        float normalizedScore = maxKeywordScore > 0 ? sd.score / maxKeywordScore : 0;
                        docScores.put(id, normalizedScore * keywordWeight);
                    }
                }

                // 2. Vector search
                float vectorWeight = 1.0f - keywordWeight;
                if (vectorWeight > 0) {
                    float[] queryVector = embedText(query);
                    if (queryVector != null) {
                        KnnFloatVectorQuery knnQuery = new KnnFloatVectorQuery("embedding", normalize(queryVector),
                                topK * 2);
                        var topDocs = searcher.search(knnQuery, topK * 2);
                        float maxVectorScore = topDocs.scoreDocs.length > 0 ? topDocs.scoreDocs[0].score : 1.0f;
                        var storedFields = searcher.storedFields();
                        for (var sd : topDocs.scoreDocs) {
                            Document luceneDoc = storedFields.document(sd.doc);
                            String id = luceneDoc.get("id");
                            float normalizedScore = maxVectorScore > 0 ? sd.score / maxVectorScore : 0;
                            docScores.merge(id, normalizedScore * vectorWeight, Float::sum);
                        }
                    }
                }

                // 3. Sort by combined score and return topK
                return docScores.entrySet().stream()
                        .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                        .limit(topK)
                        .map(entry -> {
                            MedicalDocument doc = documents.get(entry.getKey());
                            if (doc != null) {
                                String content = loadContent(entry.getKey(), doc.getContent());
                                return copyWithContent(doc, content);
                            }
                            return null;
                        })
                        .filter(doc -> doc != null)
                        .toList();

            } finally {
                searcherManager.release(searcher);
            }
        } catch (Exception e) {
            log.warn("Hybrid search failed, returning empty result", e);
            return List.of();
        }
    }

    /**
     * Keyword-only search using multi-field query
     */
    private List<ScoredDoc> keywordSearch(IndexSearcher searcher, String queryText, int topN) throws Exception {
        // Use multi-field parser to search across multiple fields
        String[] fields = { "disease_text", "department_text", "symptoms", "causes", "diagnosis", "treatment",
                "prevention", "content" };
        Map<String, Float> boosts = new HashMap<>();
        boosts.put("disease_text", 3.0f); // Disease name most important
        boosts.put("symptoms", 2.0f); // Symptoms very important
        boosts.put("diagnosis", 1.5f); // Diagnosis important
        boosts.put("treatment", 1.2f); // Treatment relevant
        boosts.put("department_text", 1.0f); // Department relevant
        boosts.put("causes", 1.0f);
        boosts.put("prevention", 0.8f);
        boosts.put("content", 0.5f); // General content least weight

        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);
        parser.setDefaultOperator(QueryParser.Operator.OR);

        Query query = parser.parse(QueryParser.escape(queryText));

        TopDocs topDocs = searcher.search(query, topN);
        List<ScoredDoc> results = new ArrayList<>();
        var storedFields = searcher.storedFields();

        for (ScoreDoc sd : topDocs.scoreDocs) {
            Document doc = storedFields.document(sd.doc);
            String id = doc.get("id");
            if (id != null) {
                results.add(new ScoredDoc(id, sd.score));
            }
        }

        return results;
    }

    private static class ScoredDoc {
        final String id;
        final float score;

        ScoredDoc(String id, float score) {
            this.id = id;
            this.score = score;
        }
    }

    /**
     * Reload knowledge base and rebuild vectors.
     */
    public void reload() {
        documents.clear();
        try {
            if (indexWriter != null) {
                indexWriter.deleteAll();
                indexWriter.commit();
            }
        } catch (IOException e) {
            log.warn("Failed to clear vector index", e);
        }
        loadKnowledgeBase();
    }

    private void loadKnowledgeBase() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String pathPattern = ensureTrailingSlash(knowledgePath) + "*.json";
            Resource[] resources = resolver.getResources(pathPattern);

            embeddingExecutor = Executors
                    .newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
            List<Runnable> embedTasks = new ArrayList<>();

            for (Resource resource : resources) {
                try {
                    MedicalDocument doc = loadDocument(resource);
                    if (doc != null) {
                        embedTasks.add(() -> ensureVectorIndexed(doc, null));
                    }
                } catch (Exception e) {
                    log.warn("Failed to load document: {}", resource.getFilename(), e);
                }
            }

            embedTasks.forEach(embeddingExecutor::execute);
            embeddingExecutor.shutdown();
            embeddingExecutor.awaitTermination(15, TimeUnit.MINUTES);

            if (searcherManager != null) {
                searcherManager.maybeRefresh();
            }
            log.info("Loaded {} medical documents into knowledge base from {}", documents.size(),
                    ensureTrailingSlash(knowledgePath));
        } catch (IOException e) {
            log.warn("No medical knowledge files found at path: {}", knowledgePath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Embedding tasks interrupted", e);
        }
    }

    private MedicalDocument loadDocument(Resource resource) throws IOException {
        String filename = resource.getFilename();
        if (filename == null)
            return null;

        String diseaseName = filename.replaceAll("^\\d+_", "").replace(".json", "");

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            String content = objectMapper.writeValueAsString(root);

            MedicalDocument doc = new MedicalDocument();
            doc.setId(filename);
            doc.setDiseaseName(diseaseName);
            doc.setDepartment(inferDepartment(diseaseName, content));
            doc.setContent(null); // avoid keeping large JSON in memory; load on demand

            documents.put(doc.getId(), doc);
            return doc;
        }
    }

    private String inferDepartment(String diseaseName, String content) {
        // Count occurrences of explicit department names in disease name + content
        String text = (diseaseName == null ? "" : diseaseName) + " " + (content == null ? "" : content);

        String[] departments = new String[] {
                "呼吸内科", "消化内科", "心内科", "神经内科", "内分泌科",
                "血液科", "风湿免疫科", "肿瘤科", "感染科", "传染科",
                "肾内科", "内科", "外科", "骨科", "泌尿外科",
                "普外科", "神经外科", "胸外科", "心外科", "烧伤科",
                "急诊科", "重症医学科", "妇产科", "产科", "妇科",
                "儿科", "新生儿科", "皮肤科", "眼科", "耳鼻喉科",
                "口腔科", "口腔颌面外科"
        };

        String bestDept = "内科";
        int bestCount = 0;

        for (String dept : departments) {
            int count = 0;
            int idx = -1;
            while ((idx = text.indexOf(dept, idx + 1)) >= 0) {
                count++;
            }
            if (count > bestCount) {
                bestCount = count;
                bestDept = dept;
            }
        }

        return bestDept;
    }

    private void openVectorIndex() throws IOException {
        Path path = Path.of(stripFileScheme(ensureTrailingSlash(vectorIndexPath)));
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }
        var dir = FSDirectory.open(path);
        this.analyzer = new SmartChineseAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        this.indexWriter = new IndexWriter(dir, config);
        this.searcherManager = new SearcherManager(indexWriter, null);
    }

    private String ensureTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    private String stripFileScheme(String path) {
        if (path.startsWith("file:")) {
            return path.substring("file:".length());
        }
        return path;
    }

    private void ensureVectorIndexed(MedicalDocument doc, String contentOverride) {
        try {
            if (hasVector(doc.getId())) {
                return;
            }
            String content = contentOverride != null ? contentOverride : loadContent(doc.getId(), doc.getContent());
            if (content == null || content.isBlank())
                return;
            float[] embedding = embedText(buildEmbedText(doc.getDiseaseName(), content));
            // if (embedding == null) return; // Don't return, allow keyword indexing
            indexVector(doc, embedding);
        } catch (Exception e) {
            log.warn("Failed to index vector for {}", doc.getId(), e);
        }
    }

    private boolean hasVector(String id) throws IOException {
        if (searcherManager == null)
            return false;
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            var hits = searcher.search(new TermQuery(new Term("id", id)), 1);
            return hits.totalHits.value() > 0;
        } finally {
            searcherManager.release(searcher);
        }
    }

    private void indexVector(MedicalDocument doc, float[] vector) throws IOException {
        if (indexWriter == null)
            return;
        float[] normalized = normalize(vector);
        Document luceneDoc = new Document();

        // For exact matching and retrieval
        luceneDoc.add(new StringField("id", doc.getId(), Field.Store.YES));
        luceneDoc.add(new StoredField("diseaseName", doc.getDiseaseName()));
        luceneDoc.add(new StoredField("department", doc.getDepartment()));

        // For keyword search - using TextField for full-text search
        luceneDoc.add(new TextField("disease_text", doc.getDiseaseName(), Field.Store.NO));
        luceneDoc.add(new TextField("department_text", doc.getDepartment(), Field.Store.NO));

        // Parse content to extract searchable fields
        String content = loadContent(doc.getId(), doc.getContent());
        if (content != null && !content.isEmpty()) {
            try {
                JsonNode contentNode = objectMapper.readTree(content);
                indexJsonFields(luceneDoc, contentNode);
            } catch (Exception e) {
                // If not JSON, index as plain text
                luceneDoc.add(new TextField("content", content, Field.Store.NO));
            }
        }

        // Vector field for semantic search
        if (normalized != null) {
            luceneDoc.add(new KnnFloatVectorField("embedding", normalized));
        }

        indexWriter.updateDocument(new Term("id", doc.getId()), luceneDoc);
        indexWriter.commit();
        searcherManager.maybeRefresh();
    }

    private void indexJsonFields(Document luceneDoc, JsonNode node) {
        // Index common medical fields
        indexField(luceneDoc, "symptoms", node, "症状", "主要表现", "临床表现");
        indexField(luceneDoc, "causes", node, "病因", "原因");
        indexField(luceneDoc, "diagnosis", node, "诊断", "鉴别诊断");
        indexField(luceneDoc, "treatment", node, "治疗", "治疗方法", "治疗方案");
        indexField(luceneDoc, "prevention", node, "预防", "预防措施");

        // Index all text content for general search
        StringBuilder allText = new StringBuilder();
        extractAllText(node, allText);
        if (allText.length() > 0) {
            luceneDoc.add(new TextField("content", allText.toString(), Field.Store.NO));
        }
    }

    private void indexField(Document luceneDoc, String fieldName, JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode field = node.get(key);
            if (field != null && field.isTextual()) {
                luceneDoc.add(new TextField(fieldName, field.asText(), Field.Store.NO));
            }
        }
    }

    private void extractAllText(JsonNode node, StringBuilder sb) {
        if (node.isTextual()) {
            sb.append(node.asText()).append(" ");
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                extractAllText(item, sb);
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> extractAllText(entry.getValue(), sb));
        }
    }

    private String buildEmbedText(String diseaseName, String content) {
        String raw = diseaseName + "\n" + content;
        // Keep input under SiliconFlow 8k token limit (Chinese ~1 char/token); trim
        // aggressively to avoid 413.
        int maxChars = 8000;
        if (raw.length() > maxChars) {
            return raw.substring(0, maxChars);
        }
        return raw;
    }

    private String loadContent(String id, String fallback) {
        try {
            Path path = Path.of(stripFileScheme(ensureTrailingSlash(knowledgePath)), id);
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        } catch (IOException e) {
            log.warn("Failed to load content for {}", id, e);
        }
        return fallback;
    }

    private MedicalDocument copyWithContent(MedicalDocument source, String content) {
        MedicalDocument copy = new MedicalDocument();
        copy.setId(source.getId());
        copy.setDiseaseName(source.getDiseaseName());
        copy.setDepartment(source.getDepartment());
        copy.setContent(content);
        copy.setEmbedding(source.getEmbedding());
        return copy;
    }

    private MedicalDocument copyMetadata(MedicalDocument source) {
        MedicalDocument copy = new MedicalDocument();
        copy.setId(source.getId());
        copy.setDiseaseName(source.getDiseaseName());
        copy.setDepartment(source.getDepartment());
        copy.setContent(null);
        copy.setEmbedding(null);
        return copy;
    }

    private float[] embedText(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SILICONFLOW_API_KEY not set, skipping embedding");
            return null;
        }

        try {
            acquireRateLimits(estimateTokens(text));

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", embeddingModel);
            payload.put("input", text);
            payload.put("encoding_format", "float");

            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("Embedding request failed: {} - {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                log.warn("Invalid embedding response: {}", response.body());
                return null;
            }
            JsonNode embeddingNode = data.get(0).path("embedding");
            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = embeddingNode.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            log.warn("Failed to embed text", e);
            return null;
        }
    }

    private void acquireRateLimits(int tokens) throws InterruptedException {
        rpmLimiter.acquire();

        while (true) {
            long now = System.currentTimeMillis();
            synchronized (tokensThisWindow) {
                if (now - tokenWindowStartMs >= 60_000) {
                    tokenWindowStartMs = now;
                    tokensThisWindow.set(0);
                }
                if (tokensThisWindow.get() + tokens <= 450_000) {
                    tokensThisWindow.addAndGet(tokens);
                    break;
                }
            }
            Thread.sleep(200);
        }

        rateRefillScheduler.schedule(() -> rpmLimiter.release(), 1, TimeUnit.SECONDS);
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 3);
    }

    private float[] normalize(float[] vector) {
        if (vector == null)
            return null;
        float len = VectorUtil.dotProduct(vector, vector);
        if (len == 0)
            return vector;
        float norm = (float) Math.sqrt(len);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }
}
