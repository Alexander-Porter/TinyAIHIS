package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * EMR Template Entity
 */
@Data
@TableName("emr_template")
public class EmrTemplate {

    @TableId(type = IdType.AUTO)
    private Long tplId;
    
    private Long deptId; // NULL for hospital-wide template
    private Long creatorId;
    private String name;
    private String content;
    private String type; // EMR or PRESCRIPTION
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
