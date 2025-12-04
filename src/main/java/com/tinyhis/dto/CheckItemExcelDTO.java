package com.tinyhis.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Check Item Excel DTO for import/export
 */
@Data
public class CheckItemExcelDTO {

    @ExcelProperty("项目名称")
    private String itemName;

    @ExcelProperty("项目编码")
    private String itemCode;

    @ExcelProperty("价格")
    private BigDecimal price;

    @ExcelProperty("分类")
    private String category;

    @ExcelProperty("描述")
    private String description;
}
