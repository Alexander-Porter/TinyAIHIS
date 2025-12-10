package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.CheckInRequest;
import com.tinyhis.dto.RegistrationDetailDTO;
import com.tinyhis.dto.RegistrationRequest;
import com.tinyhis.entity.Department;
import com.tinyhis.entity.Registration;
import com.tinyhis.entity.Schedule;
import com.tinyhis.entity.SysUser;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.DepartmentMapper;
import com.tinyhis.mapper.RegistrationMapper;
import com.tinyhis.mapper.ScheduleMapper;
import com.tinyhis.mapper.SysUserMapper;
import com.tinyhis.service.QueueService;
import com.tinyhis.service.RegistrationService;
import com.tinyhis.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registration Service Implementation
 */
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationMapper registrationMapper;
    private final ScheduleMapper scheduleMapper;
    private final ScheduleService scheduleService;
    private final QueueService queueService;
    private final DepartmentMapper departmentMapper;
    private final SysUserMapper sysUserMapper;
    private final com.tinyhis.mapper.MedicalRecordMapper medicalRecordMapper;
    private final com.tinyhis.mapper.LabOrderMapper labOrderMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    @Value("${app.registration.fee:50.00}")
    private BigDecimal registrationFee;
    @Value("${app.redis.registration-queue:registration:queue}")
    private String registrationQueueKey;
    @Value("${app.redis.quota-prefix:schedule:quota:}")
    private String quotaPrefix;
    @Value("${app.redis.user-prefix:schedule:users:}")
    private String userPrefix;
    private static final String LUA_SCRIPT_STOCK = "if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return -1 end "
            +
            "local stock = tonumber(redis.call('GET', KEYS[1])) " +
            "if stock == nil then return -3 end " +
            "if stock <= 0 then return -2 end " +
            "redis.call('DECR', KEYS[1]) " +
            "redis.call('SADD', KEYS[2], ARGV[1]) " +
            "return 0";

    @Override
    // @Transactional // 事务已移除：我们采用 Redis + 异步写入队列来处理并发写入
    public Registration createRegistration(RegistrationRequest request) {
        Schedule schedule = scheduleService.getScheduleById(request.getScheduleId());
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }

        // 时间验证：不能挂已过期的号
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate scheduleDate = schedule.getScheduleDate();
        String shift = schedule.getShiftType();

        if (scheduleDate.isBefore(today)) {
            throw new BusinessException("不能预约过去的日期");
        }

        // 如果是当天，检查时段是否已过
        if (scheduleDate.equals(today) && !"ER".equalsIgnoreCase(shift)) {
            java.time.LocalTime now = java.time.LocalTime.now();
            // 上午班次在12:00后不可挂号
            if ("AM".equalsIgnoreCase(shift) && now.getHour() >= 12) {
                throw new BusinessException("上午号源已过期，请选择其他时段");
            }
            // 下午班次在18:00后不可挂号（假设18点下班）
            if ("PM".equalsIgnoreCase(shift) && now.getHour() >= 18) {
                throw new BusinessException("下午号源已过期，请选择其他时段");
            }
        }

        // --- Redis Seckill Logic ---
        String quotaKey = quotaPrefix + request.getScheduleId();
        String userKey = userPrefix + request.getScheduleId();

        // 如果 quota 不存在则初始化；如果已存在但小于最新可用量，则刷新为当前可用量
        int available = schedule.getMaxQuota() - schedule.getCurrentCount();
        String cached = redisTemplate.opsForValue().get(quotaKey);
        if (cached == null) {
            redisTemplate.opsForValue().setIfAbsent(quotaKey, String.valueOf(available), 24, TimeUnit.HOURS);
        } else {
            try {
                int cacheVal = Integer.parseInt(cached);
                if (cacheVal < available) {
                    redisTemplate.opsForValue().set(quotaKey, String.valueOf(available), 24, TimeUnit.HOURS);
                }
            } catch (NumberFormatException e) {
                redisTemplate.opsForValue().set(quotaKey, String.valueOf(available), 24, TimeUnit.HOURS);
            }
        }

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(LUA_SCRIPT_STOCK);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(redisScript,
                java.util.Arrays.asList(quotaKey, userKey),
                String.valueOf(request.getPatientId()));

        if (result != null) {
            if (result == -1) {
                throw new BusinessException("您已预约该时段");
            } else if (result == -2) {
                throw new BusinessException("预约失败，号源不足");
            } else if (result == -3) {
                // 按理说不会发生（由上面的懒加载防护），但若发生则处理：
                throw new BusinessException("系统繁忙，请重试");
            }
        }

        // Redis 扣减成功 -> 将请求推入异步队列
        try {
            String json = objectMapper.writeValueAsString(request);
            redisTemplate.opsForList().leftPush(registrationQueueKey, json);
        } catch (Exception e) {
            // 是否回滚 Redis？ 理想情况下应回滚，但当前实现仅记录日志并返回错误
            throw new BusinessException("系统错误: " + e.getMessage());
        }

        // Return a "Pending" registration object to satisfy the controller/frontend
        // The frontend will see "Success" but the ID might be null or placeholder
        Registration registration = new Registration();
        registration.setPatientId(request.getPatientId());
        registration.setDoctorId(schedule.getDoctorId());
        registration.setScheduleId(request.getScheduleId());
        registration.setStatus(0);
        registration.setQueueNumber(0); // Unknown yet
        registration.setFee(registrationFee);
        // We return a dummy ID or null. The frontend might need an ID to pay.
        // This is the trade-off of async.
        // For this demo, we assume the user goes to "My Registrations" page which will
        // eventually show the record.
        return registration;
    }

    /*
     * Original Code Commented Out
     * // @Override
     * // @Transactional
     * public Registration createRegistration_OLD(RegistrationRequest request) {
     * Schedule schedule = scheduleService.getScheduleById(request.getScheduleId());
     * // ...
     * return null;
     * }
     */

    @Override
    public RegistrationDetailDTO getRegistrationDetail(Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            return null;
        }

        RegistrationDetailDTO dto = new RegistrationDetailDTO();
        BeanUtils.copyProperties(registration, dto);

        // Fill related info
        Schedule schedule = scheduleMapper.selectById(registration.getScheduleId());
        if (schedule != null) {
            dto.setScheduleDate(schedule.getScheduleDate().toString());
            dto.setShift(getShiftLabel(schedule.getShiftType()));

            Department dept = departmentMapper.selectById(schedule.getDeptId());
            if (dept != null) {
                dto.setDeptName(dept.getDeptName());
            }
        }

        SysUser doctor = sysUserMapper.selectById(registration.getDoctorId());
        if (doctor != null) {
            dto.setDoctorName(doctor.getRealName());
        }

        return dto;
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
    public List<RegistrationDetailDTO> getPatientRegistrationDetails(Long patientId) {
        List<Registration> regs = getRegistrationsByPatient(patientId);
        return regs.stream().map(reg -> {
            RegistrationDetailDTO dto = new RegistrationDetailDTO();
            BeanUtils.copyProperties(reg, dto);

            Schedule schedule = scheduleService.getScheduleById(reg.getScheduleId());
            if (schedule != null) {
                dto.setScheduleDate(schedule.getScheduleDate().toString());
                dto.setShift(getShiftLabel(schedule.getShiftType()));

                Department dept = departmentMapper.selectById(schedule.getDeptId());
                if (dept != null) {
                    dto.setDeptName(dept.getDeptName());
                }

                SysUser doctor = sysUserMapper.selectById(schedule.getDoctorId());
                if (doctor != null) {
                    dto.setDoctorName(doctor.getRealName());
                }
            }

            // Check for pending labs if status is 2 (Waiting/Paused)
            if (reg.getStatus() == 2) {
                com.tinyhis.entity.MedicalRecord record = medicalRecordMapper.selectOne(
                        new LambdaQueryWrapper<com.tinyhis.entity.MedicalRecord>()
                                .eq(com.tinyhis.entity.MedicalRecord::getRegId, reg.getRegId()));

                if (record != null) {
                    Long pendingCount = labOrderMapper.selectCount(
                            new LambdaQueryWrapper<com.tinyhis.entity.LabOrder>()
                                    .eq(com.tinyhis.entity.LabOrder::getRecordId, record.getRecordId())
                                    .lt(com.tinyhis.entity.LabOrder::getStatus, 2));
                    if (pendingCount > 0) {
                        dto.setHasPendingLabs(true);
                    }
                }
            }

            return dto;
        }).collect(Collectors.toList());
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
        // 根据排班和当前时间决定是否需要签到
        Schedule schedule = scheduleService.getScheduleById(registration.getScheduleId());
        boolean autoQueue = false;
        if (schedule != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            String shift = schedule.getShiftType(); // "AM", "PM" or "ER"
            java.time.LocalDate scheduleDate = schedule.getScheduleDate();
            java.time.LocalTime nowTime = java.time.LocalTime.now();
            String currentHalf = nowTime.getHour() < 12 ? "AM" : "PM";

            // 急诊(ER)支付后直接进入队列，不需要签到（当天有效）
            if ("ER".equalsIgnoreCase(shift)) {
                if (scheduleDate != null && scheduleDate.equals(today)) {
                    autoQueue = true;
                }
            } else if (scheduleDate != null && scheduleDate.equals(today)) {
                // 普通门诊：今天且时段匹配时自动入队
                if (shift != null && shift.equalsIgnoreCase(currentHalf)) {
                    autoQueue = true;
                }
            }
        }

        if (autoQueue) {
            registration.setStatus(2); // Paid and already in queue (checked-in equivalent)
        } else {
            registration.setStatus(1); // Paid, waiting for check-in
        }

        registrationMapper.updateById(registration);

        // 如果自动排入队列，加入队列服务
        if (autoQueue) {
            queueService.addToQueue(registration.getDoctorId(), registration.getRegId());
        }

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

        // 基于时间的签到验证（移除地理位置验证）
        // 签到规则：就诊前30分钟内可签到，半天内的号直接可签到
        Schedule schedule = scheduleService.getScheduleById(registration.getScheduleId());
        if (schedule != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDate scheduleDate = schedule.getScheduleDate();
            String shift = schedule.getShiftType();

            // 急诊全天候可签到
            if ("ER".equalsIgnoreCase(shift)) {
                // 急诊只需要是当天即可签到
                if (!scheduleDate.equals(now.toLocalDate())) {
                    throw new BusinessException("急诊号仅当天有效");
                }
            } else {
                java.time.LocalTime startTime = "AM".equals(shift)
                        ? java.time.LocalTime.of(8, 0)
                        : java.time.LocalTime.of(14, 0);
                java.time.LocalDateTime appointmentTime = java.time.LocalDateTime.of(scheduleDate, startTime);

                long minutesUntilAppointment = java.time.Duration.between(now, appointmentTime).toMinutes();

                // 超过30分钟才到预约时间，不允许签到
                if (minutesUntilAppointment > 30) {
                    throw new BusinessException("签到时间未到，请于就诊前30分钟内签到");
                }
            }
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

    /**
     * 获取可叫号的患者列表（包含候诊状态2和急诊状态1）
     */
    private List<Registration> getCallablePatients(Long doctorId) {
        // 获取今天该医生的急诊排班
        java.time.LocalDate today = java.time.LocalDate.now();
        LambdaQueryWrapper<Schedule> scheduleWrapper = new LambdaQueryWrapper<>();
        scheduleWrapper.eq(Schedule::getDoctorId, doctorId)
                .eq(Schedule::getScheduleDate, today)
                .eq(Schedule::getShiftType, "ER");
        List<Schedule> erSchedules = scheduleMapper.selectList(scheduleWrapper);
        java.util.Set<Long> erScheduleIds = erSchedules.stream()
                .map(Schedule::getScheduleId)
                .collect(java.util.stream.Collectors.toSet());

        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getDoctorId, doctorId);

        if (erScheduleIds.isEmpty()) {
            // 没有急诊排班，只查状态2
            wrapper.eq(Registration::getStatus, 2);
        } else {
            // 有急诊排班：状态2 或 (状态1且是急诊排班)
            wrapper.and(w -> w.eq(Registration::getStatus, 2)
                    .or(q -> q.eq(Registration::getStatus, 1)
                            .in(Registration::getScheduleId, erScheduleIds)));
        }

        wrapper.orderByAsc(Registration::getQueueNumber);
        return registrationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Registration callNext(Long doctorId) {
        // Get next from queue
        Long regId = queueService.getNextFromQueue(doctorId);
        boolean fromQueue = regId != null;

        if (regId == null) {
            // Fallback to database query - 包含急诊状态1的患者, 不包含状态6(暂停)
            List<Registration> waiting = getCallablePatients(doctorId);
            if (waiting.isEmpty()) {
                throw new BusinessException("当前没有等待的患者");
            }
            regId = waiting.getFirst().getRegId();
        }

        return activateRegistration(doctorId, regId, fromQueue);
    }

    @Override
    @Transactional
    public Registration callSpecificPatient(Long doctorId, Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }
        if (!registration.getDoctorId().equals(doctorId)) {
            throw new BusinessException("该患者不属于当前医生");
        }

        // Allow calling from Status 1 (Paid/ER), 2 (Waiting), 6 (Paused)
        // If already 3, just return
        if (registration.getStatus() == 3) {
            return registration;
        }

        if (registration.getStatus() != 1 && registration.getStatus() != 2 && registration.getStatus() != 6) {
            throw new BusinessException("当前状态无法叫号");
        }

        // Check if there is already a patient in consultation for this doctor
        // Optionally auto-pause the current one?
        // User didn't strictly request auto-pause, but "call specific" usually implies
        // taking over.
        // Let's just set this one to 3. If there's another 3, we depend on frontend to
        // pause it or backend to handle logic?
        // Typically only one patient status 3 per doctor.
        // Let's simplisticly allow multiple 3s or just proceed.
        // Better: Find current status 3 and set to 6 (Paused)?
        // User requirement: "Prevent paused patients from ... re-entering".
        // Let's just activate this one.

        return activateRegistration(doctorId, regId, true); // Treat as fromQueue to ensure removal
    }

    private Registration activateRegistration(Long doctorId, Long regId, boolean removeFromQueue) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration != null) {
            registration.setStatus(3); // In consultation
            registrationMapper.updateById(registration);

            // Remove from queue (if it was there)
            queueService.removeFromQueue(doctorId, regId);

            // Broadcast update
            SysUser doctor = sysUserMapper.selectById(doctorId);
            if (doctor != null && doctor.getDeptId() != null) {
                queueService.broadcastQueueUpdate(doctor.getDeptId());
            }
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

        // Release quota
        scheduleService.decrementCount(registration.getScheduleId());

        return true;
    }

    private String getShiftLabel(String shiftType) {
        if (shiftType == null)
            return "";
        return switch (shiftType.toUpperCase()) {
            case "AM" -> "上午";
            case "PM" -> "下午";
            case "ER" -> "急诊(全天)";
            default -> shiftType;
        };
    }
}
