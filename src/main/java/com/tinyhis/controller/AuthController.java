package com.tinyhis.controller;

import com.tinyhis.dto.*;
import com.tinyhis.entity.PatientInfo;
import com.tinyhis.entity.SysUser;
import com.tinyhis.service.AuthService;
import com.tinyhis.mapper.SysUserMapper;
import com.tinyhis.mapper.PatientInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserMapper sysUserMapper;
    private final PatientInfoMapper patientInfoMapper;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    /**
     * Get demo info
     */
    @GetMapping("/demo-info")
    public Result<Map<String, Object>> getDemoInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("isDemo", demoMode);
        if (demoMode) {
            // Get all staff
            List<SysUser> staff = sysUserMapper.selectList(new QueryWrapper<SysUser>().eq("status", 1));
            // Get some patients (limit 5)
            List<PatientInfo> patients = patientInfoMapper.selectList(new QueryWrapper<PatientInfo>().last("LIMIT 5"));
            
            info.put("staff", staff);
            info.put("patients", patients);
        }
        return Result.success(info);
    }


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
