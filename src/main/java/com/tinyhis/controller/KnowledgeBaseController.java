package com.tinyhis.controller;

import com.tinyhis.ai.MedicalDocument;
import com.tinyhis.ai.MedicalKnowledgeBase;
import com.tinyhis.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final MedicalKnowledgeBase knowledgeBase;

    @GetMapping("/list")
    public Result<List<MedicalDocument>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department) {
        return Result.success(knowledgeBase.getAllDocuments(keyword, department));
    }

    @GetMapping("/stats")
    public Result<java.util.Map<String, Object>> getStats() {
        return Result.success(knowledgeBase.getStatistics());
    }

    @PostMapping("/import")
    public Result<Integer> batchImport(@RequestParam("files") org.springframework.web.multipart.MultipartFile[] files) {
        int count = 0;
        for (org.springframework.web.multipart.MultipartFile file : files) {
            try {
                String filename = file.getOriginalFilename();
                String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                
                MedicalDocument doc = new MedicalDocument();
                if (filename != null && filename.toLowerCase().endsWith(".json")) {
                    // Try to parse as JSON MedicalDocument
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        doc = mapper.readValue(content, MedicalDocument.class);
                        if (doc.getId() == null) doc.setId(filename);
                    } catch (Exception e) {
                        // Fallback: treat as text
                        doc.setId(filename);
                        doc.setDiseaseName(filename.replace(".json", ""));
                        doc.setContent(content);
                    }
                } else {
                    // Treat as text
                    doc.setId(filename);
                    if (filename != null) {
                        doc.setDiseaseName(filename.replaceAll("\\.[^.]+$", ""));
                    } else {
                        doc.setDiseaseName("Unknown");
                    }
                    doc.setContent(content);
                }
                
                // If department is missing, it will be inferred in addDocument -> (no, we need to infer it before or let KB handle it. 
                // KB.addDocument doesn't automatically infer dept if we construct object here. 
                // Let's manually trigger inference if needed or modify KB to allow inference helper access.
                // KB.addDocument takes the object as is.
                // Let's modify default department if null.
                if (doc.getDepartment() == null) {
                    doc.setDepartment("未分类"); // Or we could duplicate the inference logic, or expose it.
                    // For now, "未分类" or "内科" as default.
                }

                knowledgeBase.addDocument(doc);
                count++;
            } catch (Exception e) {
                // log error but continue
                e.printStackTrace();
            }
        }
        return Result.success(count);
    }

    @GetMapping("/{id}")
    public Result<MedicalDocument> get(@PathVariable String id) {
        return Result.success(knowledgeBase.getDocument(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody MedicalDocument doc) {
        knowledgeBase.addDocument(doc);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody MedicalDocument doc) {
        doc.setId(id);
        knowledgeBase.addDocument(doc); // Overwrite
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        knowledgeBase.deleteDocument(id);
        return Result.success();
    }
}
