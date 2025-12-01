package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Drug Dictionary Entity
 */
@Data
@TableName("drug_dict")
public class DrugDict {

    @TableId(type = IdType.AUTO)
    private Long drugId;
    
    private String name;
    private String spec; // Specification
    private BigDecimal price;
    private Integer stockQuantity;
    private String unit;
    private String manufacturer;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
