package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Department Entity
 */
@Data
@TableName("department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Long deptId;
    
    private String deptName;
    private String location;
    private String screenId; // Associated screen device ID
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
