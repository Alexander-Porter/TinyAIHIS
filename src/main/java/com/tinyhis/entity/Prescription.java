package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 处方实体类
 */
@Data
@TableName("prescription")
public class Prescription {

    @TableId(type = IdType.AUTO)
    private Long presId;
    
    private Long recordId;
    private Long drugId;
    private Integer quantity;
    private String usageInstruction;
    /**
     * 状态码：
     * 0 - 待支付
     * 1 - 已支付
     * 2 - 已发药
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
