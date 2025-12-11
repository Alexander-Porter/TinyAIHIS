package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检验单实体类
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
     * 状态码：
     * 0 - 待支付
     * 1 - 已支付/待检查
     * 2 - 已完成
     */
    private Integer status;
    private String resultText;
    private String resultImages; // 图片URL的JSON数组
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
