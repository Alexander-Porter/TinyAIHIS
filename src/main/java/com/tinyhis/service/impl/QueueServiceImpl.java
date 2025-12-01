package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.QueueInfo;
import com.tinyhis.entity.Department;
import com.tinyhis.entity.PatientInfo;
import com.tinyhis.entity.Registration;
import com.tinyhis.entity.SysUser;
import com.tinyhis.mapper.DepartmentMapper;
import com.tinyhis.mapper.PatientInfoMapper;
import com.tinyhis.mapper.RegistrationMapper;
import com.tinyhis.mapper.SysUserMapper;
import com.tinyhis.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queue Service Implementation
 * Uses in-memory queue (in production, use Redis)
 */
@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RegistrationMapper registrationMapper;
    private final PatientInfoMapper patientInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;

    // In-memory queue for each doctor
    private final Map<Long, ConcurrentLinkedQueue<Long>> doctorQueues = new ConcurrentHashMap<>();

    @Override
    public void addToQueue(Long doctorId, Long regId) {
        doctorQueues.computeIfAbsent(doctorId, k -> new ConcurrentLinkedQueue<>()).add(regId);
        
        // Get department and broadcast
        SysUser doctor = sysUserMapper.selectById(doctorId);
        if (doctor != null && doctor.getDeptId() != null) {
            broadcastQueueUpdate(doctor.getDeptId());
        }
    }

    @Override
    public void removeFromQueue(Long doctorId, Long regId) {
        ConcurrentLinkedQueue<Long> queue = doctorQueues.get(doctorId);
        if (queue != null) {
            queue.remove(regId);
        }
        
        // Get department and broadcast
        SysUser doctor = sysUserMapper.selectById(doctorId);
        if (doctor != null && doctor.getDeptId() != null) {
            broadcastQueueUpdate(doctor.getDeptId());
        }
    }

    @Override
    public Long getNextFromQueue(Long doctorId) {
        ConcurrentLinkedQueue<Long> queue = doctorQueues.get(doctorId);
        if (queue != null) {
            return queue.poll();
        }
        return null;
    }

    @Override
    public QueueInfo getQueueInfo(Long deptId) {
        QueueInfo info = new QueueInfo();
        info.setDeptId(deptId);

        Department dept = departmentMapper.selectById(deptId);
        if (dept != null) {
            info.setDeptName(dept.getDeptName());
        }

        // Get doctors in this department
        LambdaQueryWrapper<SysUser> doctorWrapper = new LambdaQueryWrapper<>();
        doctorWrapper.eq(SysUser::getDeptId, deptId)
                     .in(SysUser::getRole, "DOCTOR", "CHIEF");
        List<SysUser> doctors = sysUserMapper.selectList(doctorWrapper);

        List<QueueInfo.WaitingPatient> waitingList = new ArrayList<>();
        QueueInfo.CurrentPatient currentPatient = null;

        for (SysUser doctor : doctors) {
            // Get current patient (status = 3)
            LambdaQueryWrapper<Registration> currentWrapper = new LambdaQueryWrapper<>();
            currentWrapper.eq(Registration::getDoctorId, doctor.getUserId())
                         .eq(Registration::getStatus, 3);
            Registration current = registrationMapper.selectOne(currentWrapper);

            if (current != null && currentPatient == null) {
                PatientInfo patient = patientInfoMapper.selectById(current.getPatientId());
                currentPatient = new QueueInfo.CurrentPatient();
                currentPatient.setRegId(current.getRegId());
                currentPatient.setQueueNumber(current.getQueueNumber());
                currentPatient.setPatientName(patient != null ? patient.getName() : "");
                currentPatient.setRoomNumber("1号诊室");
            }

            // Get waiting patients (status = 2)
            LambdaQueryWrapper<Registration> waitingWrapper = new LambdaQueryWrapper<>();
            waitingWrapper.eq(Registration::getDoctorId, doctor.getUserId())
                         .eq(Registration::getStatus, 2)
                         .orderByAsc(Registration::getQueueNumber);
            List<Registration> waitingRegs = registrationMapper.selectList(waitingWrapper);

            for (Registration reg : waitingRegs) {
                PatientInfo patient = patientInfoMapper.selectById(reg.getPatientId());
                QueueInfo.WaitingPatient wp = new QueueInfo.WaitingPatient();
                wp.setRegId(reg.getRegId());
                wp.setQueueNumber(reg.getQueueNumber());
                wp.setPatientName(patient != null ? patient.getName() : "");
                waitingList.add(wp);
            }
        }

        info.setCurrent(currentPatient);
        info.setWaiting(waitingList);
        return info;
    }

    @Override
    public void broadcastQueueUpdate(Long deptId) {
        QueueInfo info = getQueueInfo(deptId);
        messagingTemplate.convertAndSend("/topic/queue/" + deptId, info);
    }
}
