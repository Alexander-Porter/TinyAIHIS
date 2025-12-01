package com.tinyhis.service;

import com.tinyhis.dto.EmrRequest;
import com.tinyhis.entity.EmrTemplate;
import com.tinyhis.entity.MedicalRecord;
import com.tinyhis.entity.Prescription;
import com.tinyhis.entity.LabOrder;

import java.util.List;

/**
 * EMR (Electronic Medical Record) Service Interface
 */
public interface EmrService {

    /**
     * Save medical record with prescriptions and lab orders
     */
    MedicalRecord saveEmr(EmrRequest request, Long doctorId);

    /**
     * Get medical record by ID
     */
    MedicalRecord getRecordById(Long recordId);

    /**
     * Get medical records by patient
     */
    List<MedicalRecord> getRecordsByPatient(Long patientId);

    /**
     * Get medical record by registration
     */
    MedicalRecord getRecordByRegId(Long regId);

    /**
     * Get prescriptions by record
     */
    List<Prescription> getPrescriptionsByRecord(Long recordId);

    /**
     * Get lab orders by record
     */
    List<LabOrder> getLabOrdersByRecord(Long recordId);

    /**
     * Get EMR templates by department
     */
    List<EmrTemplate> getTemplatesByDept(Long deptId);

    /**
     * Save EMR template
     */
    EmrTemplate saveTemplate(EmrTemplate template);

    /**
     * Delete EMR template
     */
    boolean deleteTemplate(Long tplId);
}
