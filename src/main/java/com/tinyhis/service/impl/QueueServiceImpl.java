package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.QueueInfo;
import com.tinyhis.entity.*;
import com.tinyhis.mapper.*;
import com.tinyhis.service.QueueService;
import lombok.RequiredArgsConstructor;


import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 候诊队列服务实现
 * 当前实现使用内存队列（横向扩展场景建议改用 Redis 或消息中间件以支持多实例共享）。
 */
@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RegistrationMapper registrationMapper;
    private final PatientInfoMapper patientInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final ScheduleMapper scheduleMapper;
    private final ConsultingRoomMapper consultingRoomMapper;

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
        
        // 获取今天科室所有医生的急诊排班
        LocalDate today = LocalDate.now();
        List<Long> doctorIds = doctors.stream().map(SysUser::getUserId).toList();
        Set<Long> erScheduleIds = new HashSet<>();
        if (!doctorIds.isEmpty()) {
            LambdaQueryWrapper<Schedule> scheduleWrapper = new LambdaQueryWrapper<>();
            scheduleWrapper.in(Schedule::getDoctorId, doctorIds)
                           .eq(Schedule::getScheduleDate, today)
                           .eq(Schedule::getShiftType, "ER");
            List<Schedule> erSchedules = scheduleMapper.selectList(scheduleWrapper);
            erScheduleIds = erSchedules.stream()
                    .map(Schedule::getScheduleId)
                    .collect(Collectors.toSet());
        }

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
                
                // 获取诊室信息
                Registration reg = current;
                Schedule schedule = scheduleMapper.selectById(reg.getScheduleId());
                System.out.println("Schedule fetched for current patient: " + schedule);


                if (schedule != null && schedule.getRoomId() != null) {
                    ConsultingRoom room = consultingRoomMapper.selectById(schedule.getRoomId());
                    System.out.println("Consulting room fetched: " + room);
                    if (room != null) {
                        currentPatient.setRoomNumber(room.getRoomName());
                    } else {
                        currentPatient.setRoomNumber("诊室");
                    }
                } else {
                    currentPatient.setRoomNumber("诊室");
                }
            }

            // Get waiting patients (status = 2 or status = 1 for ER)
            LambdaQueryWrapper<Registration> waitingWrapper = new LambdaQueryWrapper<>();
            waitingWrapper.eq(Registration::getDoctorId, doctor.getUserId());
            
            final Set<Long> finalErScheduleIds = erScheduleIds;
            if (erScheduleIds.isEmpty()) {
                waitingWrapper.eq(Registration::getStatus, 2);
            } else {
                // 状态2 或 (状态1且是急诊排班)
                waitingWrapper.and(w -> w.eq(Registration::getStatus, 2)
                                         .or(q -> q.eq(Registration::getStatus, 1)
                                                   .in(Registration::getScheduleId, finalErScheduleIds)));
            }
            
            waitingWrapper.orderByAsc(Registration::getQueueNumber);
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
