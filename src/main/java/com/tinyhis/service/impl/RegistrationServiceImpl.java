package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.CheckInRequest;
import com.tinyhis.dto.RegistrationRequest;
import com.tinyhis.entity.Registration;
import com.tinyhis.entity.Schedule;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.RegistrationMapper;
import com.tinyhis.service.QueueService;
import com.tinyhis.service.RegistrationService;
import com.tinyhis.service.ScheduleService;
import com.tinyhis.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Registration Service Implementation
 */
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationMapper registrationMapper;
    private final ScheduleService scheduleService;
    private final QueueService queueService;

    @Value("${hospital.latitude}")
    private double hospitalLat;

    @Value("${hospital.longitude}")
    private double hospitalLon;

    @Value("${hospital.check-in-radius}")
    private double checkInRadius;

    private static final BigDecimal REGISTRATION_FEE = new BigDecimal("50.00");

    @Override
    @Transactional
    public Registration createRegistration(RegistrationRequest request) {
        Schedule schedule = scheduleService.getScheduleById(request.getScheduleId());
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }

        if (schedule.getCurrentCount() >= schedule.getMaxQuota()) {
            throw new BusinessException("该时段已约满");
        }

        // Check if already registered for this schedule
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getPatientId, request.getPatientId())
               .eq(Registration::getScheduleId, request.getScheduleId())
               .ne(Registration::getStatus, 5); // Not cancelled
        if (registrationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("您已预约该时段");
        }

        // Increment schedule count
        if (!scheduleService.incrementCount(request.getScheduleId())) {
            throw new BusinessException("预约失败，号源不足");
        }

        Registration registration = new Registration();
        registration.setPatientId(request.getPatientId());
        registration.setDoctorId(schedule.getDoctorId());
        registration.setScheduleId(request.getScheduleId());
        registration.setStatus(0); // Pending payment
        registration.setQueueNumber(schedule.getCurrentCount());
        registration.setFee(REGISTRATION_FEE);

        registrationMapper.insert(registration);
        return registration;
    }

    @Override
    public Registration getRegistrationById(Long regId) {
        return registrationMapper.selectById(regId);
    }

    @Override
    public List<Registration> getRegistrationsByPatient(Long patientId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getPatientId, patientId)
               .orderByDesc(Registration::getCreateTime);
        return registrationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Registration payRegistration(Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        if (registration.getStatus() != 0) {
            throw new BusinessException("该挂号已支付或状态异常");
        }

        // 模拟缴费成功
        registration.setStatus(1); // Paid, waiting for check-in
        registrationMapper.updateById(registration);
        return registration;
    }

    @Override
    @Transactional
    public Registration checkIn(CheckInRequest request) {
        Registration registration = registrationMapper.selectById(request.getRegId());
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        if (registration.getStatus() != 1) {
            throw new BusinessException("请先完成缴费或状态异常");
        }

        // Check GPS location
        boolean withinRadius = GeoUtils.isWithinRadius(
            request.getLatitude(), request.getLongitude(),
            hospitalLat, hospitalLon, checkInRadius
        );

        if (!withinRadius) {
            throw new BusinessException("距离医院过远，无法签到");
        }

        // Update status to waiting
        registration.setStatus(2); // Checked in, waiting
        registrationMapper.updateById(registration);

        // Add to queue
        queueService.addToQueue(registration.getDoctorId(), registration.getRegId());

        return registration;
    }

    @Override
    public List<Registration> getWaitingQueue(Long doctorId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getDoctorId, doctorId)
               .eq(Registration::getStatus, 2) // Waiting
               .orderByAsc(Registration::getQueueNumber);
        return registrationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Registration callNext(Long doctorId) {
        // Get next from queue
        Long regId = queueService.getNextFromQueue(doctorId);
        if (regId == null) {
            // Fallback to database query
            List<Registration> waiting = getWaitingQueue(doctorId);
            if (waiting.isEmpty()) {
                throw new BusinessException("当前没有等待的患者");
            }
            regId = waiting.getFirst().getRegId();
        }

        Registration registration = registrationMapper.selectById(regId);
        if (registration != null) {
            registration.setStatus(3); // In consultation
            registrationMapper.updateById(registration);

            // Remove from queue and broadcast
            queueService.removeFromQueue(doctorId, regId);
        }

        return registration;
    }

    @Override
    @Transactional
    public Registration completeConsultation(Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        registration.setStatus(4); // Completed
        registrationMapper.updateById(registration);
        return registration;
    }

    @Override
    @Transactional
    public boolean cancelRegistration(Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            return false;
        }

        if (registration.getStatus() >= 3) {
            throw new BusinessException("就诊中或已完成的挂号无法取消");
        }

        registration.setStatus(5); // Cancelled
        registrationMapper.updateById(registration);
        return true;
    }
}
