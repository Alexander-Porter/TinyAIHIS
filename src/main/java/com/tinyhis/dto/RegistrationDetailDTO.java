package com.tinyhis.dto;

import com.tinyhis.entity.Registration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegistrationDetailDTO extends Registration {
    private String deptName;
    private String doctorName;
    private String scheduleDate; // yyyy-MM-dd
    private String shift; // 上午/下午
}
