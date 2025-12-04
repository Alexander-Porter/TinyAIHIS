package com.tinyhis.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Drug Usage Report DTO for statistics export
 */
@Data
public class DrugUsageReportDTO {

    @ExcelProperty("科室名称")
    private String deptName;

    @ExcelProperty("药品名称")
    private String drugName;

    @ExcelProperty("使用总量")
    private Integer totalQuantity;

    @ExcelProperty("开药次数")
    private Integer totalTimes;
}
