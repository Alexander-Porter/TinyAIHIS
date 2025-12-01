package com.tinyhis.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Drug Excel DTO for import/export
 */
@Data
public class DrugExcelDTO {
    
    @ExcelProperty("药品名称")
    private String name;
    
    @ExcelProperty("规格")
    private String spec;
    
    @ExcelProperty("单价")
    private BigDecimal price;
    
    @ExcelProperty("库存数量")
    private Integer stockQuantity;
    
    @ExcelProperty("单位")
    private String unit;
    
    @ExcelProperty("生产厂家")
    private String manufacturer;
}
