package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 挂号实体类
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
     * 状态码：
     * 0 - 待支付
     * 1 - 已支付/待签到
     * 2 - 已签到/候诊中
     * 3 - 就诊中
     * 4 - 已完成
     * 5 - 已取消
     * 6 - 已暂停/待回诊
     */
    private Integer status;
    private Integer queueNumber;
    private BigDecimal fee;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
