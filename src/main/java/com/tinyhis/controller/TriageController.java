package com.tinyhis.controller;

import com.tinyhis.dto.Result;
import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;
import com.tinyhis.service.TriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI Triage Controller
 */
@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    /**
     * AI-based triage recommendation
     */
    @PostMapping("/recommend")
    public Result<TriageResult> triage(@RequestBody TriageRequest request) {
        TriageResult result = triageService.triage(request);
        return Result.success(result);
    }
}
