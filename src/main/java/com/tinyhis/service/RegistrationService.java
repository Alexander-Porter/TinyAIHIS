package com.tinyhis.service;

import com.tinyhis.dto.CheckInRequest;
import com.tinyhis.dto.RegistrationRequest;
import com.tinyhis.entity.Registration;

import java.util.List;

/**
 * Registration Service Interface
 */
public interface RegistrationService {

    /**
     * Create a new registration (appointment)
     */
    Registration createRegistration(RegistrationRequest request);

    /**
     * Get registration by ID
     */
    Registration getRegistrationById(Long regId);

    /**
     * Get registrations by patient
     */
    List<Registration> getRegistrationsByPatient(Long patientId);

    /**
     * Pay for registration (模拟缴费)
     */
    Registration payRegistration(Long regId);

    /**
     * Check in using GPS location
     */
    Registration checkIn(CheckInRequest request);

    /**
     * Get waiting queue for doctor
     */
    List<Registration> getWaitingQueue(Long doctorId);

    /**
     * Call next patient
     */
    Registration callNext(Long doctorId);

    /**
     * Complete consultation
     */
    Registration completeConsultation(Long regId);

    /**
     * Cancel registration
     */
    boolean cancelRegistration(Long regId);
}
