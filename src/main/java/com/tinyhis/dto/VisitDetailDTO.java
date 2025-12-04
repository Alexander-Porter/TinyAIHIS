package com.tinyhis.dto;

import com.tinyhis.entity.LabOrder;
import com.tinyhis.entity.MedicalRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete visit detail DTO for doctor workstation
 * Contains registration info, medical record, prescriptions, and lab orders
 */
@Data
public class VisitDetailDTO {
    // Registration info
    private Long regId;
    private Long patientId;
    private Long doctorId;
    private Long scheduleId;
    private Integer status;
    private Integer queueNumber;
    private BigDecimal fee;
    private LocalDateTime createTime;
    
    // Patient info
    private String patientName;
    private Integer gender;
    private Integer age;
    private String phone;
    private String idCard;
    
    // Schedule info
    private String scheduleDate;
    private String shiftType;
    private String deptName;
    private String doctorName;
    
    // Consulting Room info
    private Long roomId;
    private String roomName;
    private String roomLocation;
    
    // Medical record (this visit)
    private MedicalRecord medicalRecord;
    
    // Prescriptions for this visit
    private List<PrescriptionDetailDTO> prescriptions;
    
    // Lab orders for this visit
    private List<LabOrder> labOrders;
    
    // Whether this is today's visit
    private Boolean isToday;
}
