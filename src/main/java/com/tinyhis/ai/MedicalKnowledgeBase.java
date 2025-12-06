package com.tinyhis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
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

    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
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

    public List<MedicalDocument> getAllDocuments() {
        return documents.values().stream()
                .map(this::copyMetadata)
                .toList();
    }

    public MedicalDocument getDocument(String id) {
        MedicalDocument meta = documents.get(id);
        if (meta == null) return null;
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
        try {
            float[] queryVector = embedText(query);
            if (queryVector == null || searcherManager == null) return List.of();

            searcherManager.maybeRefresh();
            IndexSearcher searcher = searcherManager.acquire();
            try {
                KnnFloatVectorQuery knnQuery = new KnnFloatVectorQuery("embedding", normalize(queryVector), topK);
                var topDocs = searcher.search(knnQuery, topK);
                List<MedicalDocument> results = new ArrayList<>();
                var storedFields = searcher.storedFields();
                for (var sd : topDocs.scoreDocs) {
                    Document luceneDoc = storedFields.document(sd.doc);
                    String id = luceneDoc.get("id");
                    MedicalDocument doc = documents.get(id);
                    if (doc != null) {
                        String content = loadContent(id, doc.getContent());
                        results.add(copyWithContent(doc, content));
                    }
                }
                return results;
            } finally {
                searcherManager.release(searcher);
            }
        } catch (Exception e) {
            log.warn("Vector search failed, returning empty result", e);
            return List.of();
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

            embeddingExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
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
            log.info("Loaded {} medical documents into knowledge base from {}", documents.size(), ensureTrailingSlash(knowledgePath));
        } catch (IOException e) {
            log.warn("No medical knowledge files found at path: {}", knowledgePath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Embedding tasks interrupted", e);
        }
    }

    private MedicalDocument loadDocument(Resource resource) throws IOException {
        String filename = resource.getFilename();
        if (filename == null) return null;

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

    private void openVectorIndex() throws IOException {
        Path path = Path.of(stripFileScheme(ensureTrailingSlash(vectorIndexPath)));
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }
        var dir = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(new KeywordAnalyzer());
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
            if (content == null || content.isBlank()) return;
            float[] embedding = embedText(buildEmbedText(doc.getDiseaseName(), content));
            if (embedding == null) return;
            indexVector(doc, embedding);
        } catch (Exception e) {
            log.warn("Failed to index vector for {}", doc.getId(), e);
        }
    }

    private boolean hasVector(String id) throws IOException {
        if (searcherManager == null) return false;
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
        if (indexWriter == null) return;
        float[] normalized = normalize(vector);
        Document luceneDoc = new Document();
        luceneDoc.add(new StringField("id", doc.getId(), Field.Store.YES));
        luceneDoc.add(new StoredField("diseaseName", doc.getDiseaseName()));
        luceneDoc.add(new KnnFloatVectorField("embedding", normalized));
        indexWriter.updateDocument(new Term("id", doc.getId()), luceneDoc);
        indexWriter.commit();
        searcherManager.maybeRefresh();
    }

    private String buildEmbedText(String diseaseName, String content) {
        String raw = diseaseName + "\n" + content;
        // Keep input under SiliconFlow 8k token limit (Chinese ~1 char/token); trim aggressively to avoid 413.
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
        float len = VectorUtil.dotProduct(vector, vector);
        if (len == 0) return vector;
        float norm = (float) Math.sqrt(len);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }
}
