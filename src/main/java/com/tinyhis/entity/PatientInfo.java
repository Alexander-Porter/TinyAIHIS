package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Patient Information Entity
 */
@Data
@TableName("patient_info")
public class PatientInfo {

    @TableId(type = IdType.AUTO)
    private Long patientId;
    
    private String name;
    private String idCard;
    private String phone;
    private String password;
    private Integer gender; // 0-female, 1-male
    private Integer age;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
