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
    public Result<List<MedicalDocument>> list() {
        return Result.success(knowledgeBase.getAllDocuments());
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
