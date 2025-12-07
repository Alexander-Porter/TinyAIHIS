package com.tinyhis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Knowledge Search Request DTO
 */
@Data
public class KnowledgeSearchRequest {
    
    @NotBlank(message = "查询内容不能为空")
    private String query;
    
    @Positive(message = "结果数量必须大于0")
    private Integer limit = 5;
}
