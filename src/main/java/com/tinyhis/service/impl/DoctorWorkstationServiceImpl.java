package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.VisitDetailDTO;
import com.tinyhis.entity.*;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.*;
import com.tinyhis.service.DoctorWorkstationService;
import com.tinyhis.service.EmrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorWorkstationServiceImpl implements DoctorWorkstationService {

    private final RegistrationMapper registrationMapper;
    private final ScheduleMapper scheduleMapper;
    private final ConsultingRoomMapper consultingRoomMapper;
    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final PatientInfoMapper patientInfoMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final EmrService emrService;

    @Override
    public List<VisitDetailDTO> getTodayPatients(Long doctorId) {
        // Get today's schedules for this doctor
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Schedule> scheduleWrapper = new LambdaQueryWrapper<>();
        scheduleWrapper.eq(Schedule::getDoctorId, doctorId)
                .eq(Schedule::getScheduleDate, today);
        List<Schedule> todaySchedules = scheduleMapper.selectList(scheduleWrapper);

        if (todaySchedules.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> scheduleIds = todaySchedules.stream().map(Schedule::getScheduleId).toList();

        // 找出急诊排班的 scheduleId

        // Get ALL registrations for these schedules today (exclude cancelled and
        // unpaid)
        // 状态 1=待签到, 2=候诊中, 3=就诊中, 4=已完成
        // 急诊的状态 1 也算候诊中（急诊无需签到，缴费后直接候诊）
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();

        // 获取今日该排班的所有患者（除了取消的和待缴费的）
        wrapper.in(Registration::getScheduleId, scheduleIds)
                .in(Registration::getStatus, 1, 2, 3, 4, 6); // 包含已完成的(4)和已暂停的(6)

        // 排序：就诊中(3)优先，然后候诊(1,2)，然后暂停(6)，最后已完成(4)
        // 使用 CASE WHEN 自定义排序: 3 -> 1, 1/2 -> 2, 6 -> 3, 4 -> 4
        wrapper.last(
                "ORDER BY CASE status WHEN 3 THEN 1 WHEN 1 THEN 2 WHEN 2 THEN 2 WHEN 6 THEN 3 ELSE 4 END, queue_number ASC");

        List<Registration> registrations = registrationMapper.selectList(wrapper);

        // 转换为详细的DTO对象
        return registrations.stream()
                .map(reg -> buildVisitDetailDTO(reg, true))
                .toList();
    }

    @Override
    public VisitDetailDTO getVisitDetail(Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }
        return buildVisitDetailDTO(registration, isToday(registration));
    }

    @Override
    public List<VisitDetailDTO> getPatientHistory(Long patientId, Long doctorId) {
        // 获取该患者已完成的挂号记录
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getPatientId, patientId)
                .eq(Registration::getStatus, 4); // 已完成

        // 允许查看所有医生/科室的历史记录
        // if (doctorId != null) {
        //     wrapper.eq(Registration::getDoctorId, doctorId);
        // }

        wrapper.orderByDesc(Registration::getCreateTime);

        List<Registration> registrations = registrationMapper.selectList(wrapper);

        return registrations.stream()
                .map(reg -> buildVisitDetailDTO(reg, false))
                .toList();
    }

    @Override
    @Transactional
    public Registration pauseConsultation(Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        if (registration.getStatus() != 3) {
            throw new BusinessException("只有就诊中的患者可以暂停");
        }

        registration.setStatus(6); // 设置为已暂停
        registrationMapper.updateById(registration);

        log.info("Paused consultation for registration {}", regId);
        return registration;
    }

    @Override
    @Transactional
    public Registration resumeConsultation(Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        if (registration.getStatus() != 2 && registration.getStatus() != 6) {
            throw new BusinessException("只有候诊中或暂停的患者可以恢复就诊");
        }

        registration.setStatus(3); // 就诊中
        registrationMapper.updateById(registration);

        log.info("Resumed consultation for registration {}", regId);
        return registration;
    }

    private VisitDetailDTO buildVisitDetailDTO(Registration reg, boolean isToday) {
        VisitDetailDTO dto = new VisitDetailDTO();

        // 复制挂号信息
        dto.setRegId(reg.getRegId());
        dto.setPatientId(reg.getPatientId());
        dto.setDoctorId(reg.getDoctorId());
        dto.setScheduleId(reg.getScheduleId());
        dto.setStatus(reg.getStatus());
        dto.setQueueNumber(reg.getQueueNumber());
        dto.setFee(reg.getFee());
        dto.setCreateTime(reg.getCreateTime());
        dto.setIsToday(isToday);

        // 获取患者信息
        PatientInfo patient = patientInfoMapper.selectById(reg.getPatientId());
        if (patient != null) {
            dto.setPatientName(patient.getName());
            dto.setGender(patient.getGender());
            dto.setPhone(patient.getPhone());
            dto.setIdCard(patient.getIdCard());
            dto.setAge(patient.getAge());
        }

        // 获取排班信息
        Schedule schedule = scheduleMapper.selectById(reg.getScheduleId());
        if (schedule != null) {
            dto.setScheduleDate(schedule.getScheduleDate().toString());
            dto.setShiftType(schedule.getShiftType());

            // 获取诊室信息
            if (schedule.getRoomId() != null) {
                ConsultingRoom room = consultingRoomMapper.selectById(schedule.getRoomId());
                if (room != null) {
                    dto.setRoomId(room.getRoomId());
                    dto.setRoomName(room.getRoomName());
                    dto.setRoomLocation(room.getLocation());
                }
            }

            // 获取医生信息
            SysUser doctor = sysUserMapper.selectById(schedule.getDoctorId());
            if (doctor != null) {
                dto.setDoctorName(doctor.getRealName());

                // 获取科室信息
                if (doctor.getDeptId() != null) {
                    Department dept = departmentMapper.selectById(doctor.getDeptId());
                    if (dept != null) {
                        dto.setDeptName(dept.getDeptName());
                    }
                }
            }
        }

        // 获取本次就诊的病历
        LambdaQueryWrapper<MedicalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(MedicalRecord::getRegId, reg.getRegId());
        MedicalRecord record = medicalRecordMapper.selectOne(recordWrapper);
        dto.setMedicalRecord(record);

        // 如果存在病历，获取处方和检查单
        if (record != null) {
            dto.setPrescriptions(emrService.getPrescriptionDetails(record.getRecordId()));
            dto.setLabOrders(emrService.getLabOrdersByRecord(record.getRecordId()));
        } else {
            dto.setPrescriptions(new ArrayList<>());
            dto.setLabOrders(new ArrayList<>());
        }

        return dto;
    }

    private boolean isToday(Registration reg) {
        if (reg.getScheduleId() == null)
            return false;

        Schedule schedule = scheduleMapper.selectById(reg.getScheduleId());
        if (schedule == null)
            return false;

        return LocalDate.now().equals(schedule.getScheduleDate());
    }
}
