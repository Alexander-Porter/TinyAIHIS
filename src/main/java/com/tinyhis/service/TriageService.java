package com.tinyhis.service;

import com.tinyhis.dto.TriageRequest;
import com.tinyhis.dto.TriageResult;

/**
 * AI Triage Service Interface
 */
public interface TriageService {

    /**
     * Perform AI-based triage based on body part and symptoms
     */
    TriageResult triage(TriageRequest request);
}
