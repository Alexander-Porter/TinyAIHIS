package com.tinyhis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Consulting Room Entity (诊室)
 */
@Data
@TableName("consulting_room")
public class ConsultingRoom {
    
    @TableId(type = IdType.AUTO)
    private Long roomId;
    
    private String roomName;
    
    private String roomCode;
    
    private String location;
    
    private String description;
    
    /**
     * JSON array of allowed department IDs, e.g. "[1, 2]"
     */
    private String deptIds;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
