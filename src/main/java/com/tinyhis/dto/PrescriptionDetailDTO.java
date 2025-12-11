package com.tinyhis.dto;

import com.tinyhis.entity.Prescription;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class PrescriptionDetailDTO extends Prescription {
    private String drugName;
    private String drugSpec; // Specification
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String patientName;
}
