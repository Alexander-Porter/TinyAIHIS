package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lab Order Entity
 */
@Data
@TableName("lab_order")
public class LabOrder {

    @TableId(type = IdType.AUTO)
    private Long orderId;
    
    private Long recordId;
    private String itemName;
    private BigDecimal price;
    /**
     * Status codes:
     * 0 - pending payment
     * 1 - paid/pending examination
     * 2 - completed
     */
    private Integer status;
    private String resultText;
    private String resultImages; // JSON array of image URLs
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
