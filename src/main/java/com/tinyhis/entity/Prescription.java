package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Prescription Entity
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
     * Status codes:
     * 0 - pending payment
     * 1 - paid
     * 2 - dispensed
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
