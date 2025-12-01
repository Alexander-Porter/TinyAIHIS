package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * System User Entity (Doctor, Chief, Admin, Pharmacy, Lab)
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long userId;
    
    private String username;
    private String password;
    private String realName;
    private String role; // DOCTOR, CHIEF, ADMIN, PHARMACY, LAB
    private Long deptId;
    private String phone;
    private Integer status; // 0-disabled, 1-enabled
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
