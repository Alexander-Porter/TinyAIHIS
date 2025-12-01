package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Medical Record Entity
 */
@Data
@TableName("medical_record")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long recordId;
    
    private Long regId;
    private Long patientId;
    private Long doctorId;
    private String symptom; // Chief complaint
    private String diagnosis;
    private String content; // Medical history details
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
