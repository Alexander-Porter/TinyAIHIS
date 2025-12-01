package com.tinyhis.service;

import com.tinyhis.dto.LoginRequest;
import com.tinyhis.dto.LoginResponse;
import com.tinyhis.dto.PatientRegisterRequest;
import com.tinyhis.entity.PatientInfo;
import com.tinyhis.entity.SysUser;

/**
 * Authentication Service Interface
 */
public interface AuthService {

    /**
     * Patient registration
     */
    PatientInfo registerPatient(PatientRegisterRequest request);

    /**
     * Patient login
     */
    LoginResponse patientLogin(LoginRequest request);

    /**
     * Staff login (Doctor, Admin, etc.)
     */
    LoginResponse staffLogin(LoginRequest request);

    /**
     * Get current patient by ID
     */
    PatientInfo getPatientById(Long patientId);

    /**
     * Get current staff user by ID
     */
    SysUser getStaffById(Long userId);
}
