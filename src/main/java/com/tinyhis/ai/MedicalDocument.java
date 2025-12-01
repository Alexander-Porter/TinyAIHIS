package com.tinyhis.ai;

import lombok.Data;

/**
 * Medical Document for RAG
 */
@Data
public class MedicalDocument {
    private String id;
    private String diseaseName;
    private String content;
    private String department;
}
