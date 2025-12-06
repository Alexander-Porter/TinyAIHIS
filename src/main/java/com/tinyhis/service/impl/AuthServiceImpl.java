package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.LoginRequest;
import com.tinyhis.dto.LoginResponse;
import com.tinyhis.dto.PatientRegisterRequest;
import com.tinyhis.entity.PatientInfo;
import com.tinyhis.entity.SysUser;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.PatientInfoMapper;
import com.tinyhis.mapper.SysUserMapper;
import com.tinyhis.service.AuthService;
import com.tinyhis.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication Service Implementation
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PatientInfoMapper patientInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @Override
    public PatientInfo registerPatient(PatientRegisterRequest request) {
        // Check if phone already exists
        LambdaQueryWrapper<PatientInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientInfo::getPhone, request.getPhone());
        if (patientInfoMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该手机号已注册");
        }

        PatientInfo patient = new PatientInfo();
        patient.setName(request.getName());
        patient.setIdCard(request.getIdCard());
        patient.setPhone(request.getPhone());
        patient.setPassword(passwordEncoder.encode(request.getPassword()));
        patient.setGender(request.getGender());
        patient.setAge(request.getAge());

        patientInfoMapper.insert(patient);
        patient.setPassword(null); // Don't return password
        return patient;
    }

    @Override
    public LoginResponse patientLogin(LoginRequest request) {
        LambdaQueryWrapper<PatientInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientInfo::getPhone, request.getUsername());
        PatientInfo patient = patientInfoMapper.selectOne(wrapper);

        if (patient == null) {
            throw new BusinessException("用户不存在");
        }

        // In demo mode, skip password check
        if (!demoMode) {
            if (!passwordEncoder.matches(request.getPassword(), patient.getPassword())) {
                throw new BusinessException("密码错误");
            }
        }

        String token = jwtUtils.generateToken(patient.getPatientId(), patient.getPhone(), "PATIENT", "patient");

        return new LoginResponse(token, patient.getPatientId(), patient.getPhone(), 
                                 patient.getName(), "PATIENT", null);
    }

    @Override
    public LoginResponse staffLogin(LoginRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // In demo mode, skip password check
        if (!demoMode) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException("密码错误");
            }
        }

        String token = jwtUtils.generateToken(user.getUserId(), user.getUsername(), user.getRole(), "staff");
        }

        String token = jwtUtils.generateToken(user.getUserId(), user.getUsername(), user.getRole(), "staff");

        return new LoginResponse(token, user.getUserId(), user.getUsername(), 
                                 user.getRealName(), user.getRole(), user.getDeptId());
    }

    @Override
    public PatientInfo getPatientById(Long patientId) {
        PatientInfo patient = patientInfoMapper.selectById(patientId);
        if (patient != null) {
            patient.setPassword(null);
        }
        return patient;
    }

    @Override
    public SysUser getStaffById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
