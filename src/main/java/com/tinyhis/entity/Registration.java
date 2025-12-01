package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registration Entity
 */
@Data
@TableName("registration")
public class Registration {

    @TableId(type = IdType.AUTO)
    private Long regId;
    
    private Long patientId;
    private Long doctorId;
    private Long scheduleId;
    /**
     * Status codes:
     * 0 - pending payment
     * 1 - paid/waiting check-in
     * 2 - checked in/waiting
     * 3 - in consultation
     * 4 - completed
     * 5 - cancelled
     */
    private Integer status;
    private Integer queueNumber;
    private BigDecimal fee;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
