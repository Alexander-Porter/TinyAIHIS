package com.tinyhis.service;

import com.tinyhis.dto.VisitDetailDTO;
import com.tinyhis.entity.Registration;

import java.util.List;

/**
 * Doctor Workstation Service
 * Handles complex operations for doctor workstation
 */
public interface DoctorWorkstationService {

    /**
     * Get all active patients for doctor today (status 2,3)
     */
    List<VisitDetailDTO> getTodayPatients(Long doctorId);

    /**
     * Get complete visit detail for a specific registration
     */
    VisitDetailDTO getVisitDetail(Long regId);

    /**
     * Get patient's visit history (past completed visits)
     */
    List<VisitDetailDTO> getPatientHistory(Long patientId, Long doctorId);

    /**
     * Pause current consultation (patient needs to do lab tests)
     */
    Registration pauseConsultation(Long regId);

    /**
     * Resume consultation (patient returned after lab tests)
     */
    Registration resumeConsultation(Long regId);
}
