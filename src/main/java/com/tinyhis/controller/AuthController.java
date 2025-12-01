package com.tinyhis.controller;

import com.tinyhis.dto.*;
import com.tinyhis.entity.PatientInfo;
import com.tinyhis.entity.SysUser;
import com.tinyhis.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Patient registration
     */
    @PostMapping("/patient/register")
    public Result<PatientInfo> registerPatient(@Valid @RequestBody PatientRegisterRequest request) {
        PatientInfo patient = authService.registerPatient(request);
        return Result.success(patient);
    }

    /**
     * Patient login
     */
    @PostMapping("/patient/login")
    public Result<LoginResponse> patientLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.patientLogin(request);
        return Result.success(response);
    }

    /**
     * Staff login (Doctor, Admin, etc.)
     */
    @PostMapping("/staff/login")
    public Result<LoginResponse> staffLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.staffLogin(request);
        return Result.success(response);
    }

    /**
     * Get patient info
     */
    @GetMapping("/patient/{patientId}")
    public Result<PatientInfo> getPatient(@PathVariable Long patientId) {
        PatientInfo patient = authService.getPatientById(patientId);
        return Result.success(patient);
    }

    /**
     * Get staff info
     */
    @GetMapping("/staff/{userId}")
    public Result<SysUser> getStaff(@PathVariable Long userId) {
        SysUser user = authService.getStaffById(userId);
        return Result.success(user);
    }
}
