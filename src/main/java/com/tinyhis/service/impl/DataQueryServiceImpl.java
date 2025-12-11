package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tinyhis.entity.*;
import com.tinyhis.mapper.*;
import com.tinyhis.service.DataQueryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataQueryServiceImpl implements DataQueryService {

    private final RegistrationMapper registrationMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final LabOrderMapper labOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final ScheduleMapper scheduleMapper;
    private final DrugDictMapper drugDictMapper;
    private final CheckItemMapper checkItemMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PatientInfoMapper patientInfoMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> queryData(
            String type, int page, int size,
            LocalDate startDate, LocalDate endDate,
            Long deptId, Long doctorId, String role,
            Long drugId, Integer status, String keyword,
            boolean excludeChief) {

        return switch (type) {
            case "registration" -> queryRegistrations(page, size, startDate, endDate, deptId, doctorId, status);
            case "prescription" ->
                queryPrescriptions(page, size, startDate, endDate, deptId, doctorId, drugId, status, excludeChief);
            case "lab" -> queryLabOrders(page, size, startDate, endDate, deptId, doctorId, status);
            case "user" -> queryUsers(page, size, deptId, role, status, keyword);
            case "department" -> queryDepartments(page, size, status, keyword);
            case "schedule" -> querySchedules(page, size, startDate, endDate, deptId, doctorId);
            case "drug" -> queryDrugs(page, size, status, keyword);
            case "checkItem" -> queryCheckItems(page, size, status, keyword);
            default -> Map.of("list", List.of(), "total", 0);
        };
    }

    private Map<String, Object> queryRegistrations(int page, int size, LocalDate startDate, LocalDate endDate,
            Long deptId, Long doctorId, Integer status) {
        // 首先获取符合日期范围的排班ID
        Set<Long> scheduleIds = null;
        Map<Long, Schedule> scheduleMap = new HashMap<>();

        LambdaQueryWrapper<Schedule> scheduleWrapper = new LambdaQueryWrapper<>();
        if (startDate != null)
            scheduleWrapper.ge(Schedule::getScheduleDate, startDate);
        if (endDate != null)
            scheduleWrapper.le(Schedule::getScheduleDate, endDate);
        if (deptId != null)
            scheduleWrapper.eq(Schedule::getDeptId, deptId);
        if (doctorId != null)
            scheduleWrapper.eq(Schedule::getDoctorId, doctorId);

        List<Schedule> schedules = scheduleMapper.selectList(scheduleWrapper);
        scheduleIds = schedules.stream().map(Schedule::getScheduleId).collect(Collectors.toSet());
        schedules.forEach(s -> scheduleMap.put(s.getScheduleId(), s));

        if (scheduleIds.isEmpty() && (startDate != null || endDate != null || deptId != null || doctorId != null)) {
            return Map.of("list", List.of(), "total", 0, "aggregation", Map.of());
        }

        // 查询挂号记录
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        if (scheduleIds != null && !scheduleIds.isEmpty()) {
            wrapper.in(Registration::getScheduleId, scheduleIds);
        }
        if (status != null)
            wrapper.eq(Registration::getStatus, status);
        wrapper.orderByDesc(Registration::getCreateTime);

        Page<Registration> pageResult = registrationMapper.selectPage(new Page<>(page, size), wrapper);

        // 获取相关数据
        Set<Long> patientIds = pageResult.getRecords().stream().map(Registration::getPatientId)
                .collect(Collectors.toSet());
        Set<Long> doctorIds = pageResult.getRecords().stream().map(Registration::getDoctorId)
                .collect(Collectors.toSet());

        Map<Long, String> patientNames = getPatientNames(patientIds);
        Map<Long, SysUser> doctorMap = getDoctorMap(doctorIds);
        Map<Long, String> deptNames = getDeptNames();

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getRegId());
            map.put("regId", r.getRegId());
            map.put("patientName", patientNames.getOrDefault(r.getPatientId(), "患者#" + r.getPatientId()));
            SysUser doctor = doctorMap.get(r.getDoctorId());
            map.put("doctorName", doctor != null ? doctor.getRealName() : "");
            map.put("deptName", doctor != null ? deptNames.get(doctor.getDeptId()) : "");
            Schedule sch = scheduleMap.get(r.getScheduleId());
            map.put("scheduleDate", sch != null ? sch.getScheduleDate().format(DATE_FMT) : "");
            map.put("shiftType", sch != null ? getShiftLabel(sch.getShiftType()) : "");
            map.put("queueNumber", r.getQueueNumber());
            map.put("fee", r.getFee());
            map.put("status", r.getStatus());
            map.put("statusText", getRegistrationStatusText(r.getStatus()));
            map.put("createTime", r.getCreateTime() != null ? r.getCreateTime().format(DATETIME_FMT) : "");
            return map;
        }).toList();

        return Map.of("list", list, "total", pageResult.getTotal(), "aggregation", Map.of());
    }

    private Map<String, Object> queryPrescriptions(int page, int size, LocalDate startDate, LocalDate endDate,
            Long deptId, Long doctorId, Long drugId, Integer status, boolean excludeChief) {

        // 获取日期范围内的病历记录
        LambdaQueryWrapper<MedicalRecord> recordWrapper = new LambdaQueryWrapper<>();
        if (startDate != null)
            recordWrapper.ge(MedicalRecord::getCreateTime, startDate.atStartOfDay());
        if (endDate != null)
            recordWrapper.le(MedicalRecord::getCreateTime, endDate.plusDays(1).atStartOfDay());
        if (doctorId != null)
            recordWrapper.eq(MedicalRecord::getDoctorId, doctorId);

        List<MedicalRecord> records = medicalRecordMapper.selectList(recordWrapper);
        Set<Long> recordIds = records.stream().map(MedicalRecord::getRecordId).collect(Collectors.toSet());
        Map<Long, MedicalRecord> recordMap = records.stream()
                .collect(Collectors.toMap(MedicalRecord::getRecordId, r -> r));

        if (recordIds.isEmpty() && (startDate != null || endDate != null || doctorId != null)) {
            return Map.of("list", List.of(), "total", 0, "aggregation", Map.of("totalAmount", 0.0));
        }

        // 按科室过滤，如果需要则排除主任医师
        Map<Long, SysUser> doctorMap = getDoctorMapAll();
        if (deptId != null || excludeChief) {
            final Set<Long> filteredDoctorIds = doctorMap.values().stream()
                    .filter(u -> deptId == null || deptId.equals(u.getDeptId()))
                    .filter(u -> !excludeChief || !"CHIEF".equals(u.getRole()))
                    .map(SysUser::getUserId)
                    .collect(Collectors.toSet());

            recordIds = records.stream()
                    .filter(r -> filteredDoctorIds.contains(r.getDoctorId()))
                    .map(MedicalRecord::getRecordId)
                    .collect(Collectors.toSet());
        }

        if (recordIds.isEmpty()) {
            return Map.of("list", List.of(), "total", 0, "aggregation", Map.of("totalAmount", 0.0));
        }

        // 查询处方
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Prescription::getRecordId, recordIds);
        if (drugId != null)
            wrapper.eq(Prescription::getDrugId, drugId);
        if (status != null)
            wrapper.eq(Prescription::getStatus, status);
        wrapper.orderByDesc(Prescription::getCreateTime);

        Page<Prescription> pageResult = prescriptionMapper.selectPage(new Page<>(page, size), wrapper);

        // 获取药品信息
        Map<Long, DrugDict> drugMap = getDrugMap();
        Map<Long, String> patientNames = getPatientNames(
                records.stream().map(MedicalRecord::getPatientId).collect(Collectors.toSet()));
        Map<Long, String> deptNames = getDeptNames();

        // 计算总金额
        double totalAmount = 0.0;
        for (Prescription p : pageResult.getRecords()) {
            DrugDict drug = drugMap.get(p.getDrugId());
            if (drug != null) {
                totalAmount += drug.getPrice().doubleValue() * p.getQuantity();
            }
        }

        final double finalTotal = totalAmount;

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getPresId());
            map.put("presId", p.getPresId());
            MedicalRecord record = recordMap.get(p.getRecordId());
            map.put("patientName", record != null ? patientNames.getOrDefault(record.getPatientId(), "") : "");
            SysUser doctor = record != null ? doctorMap.get(record.getDoctorId()) : null;
            map.put("doctorName", doctor != null ? doctor.getRealName() : "");
            map.put("deptName", doctor != null ? deptNames.get(doctor.getDeptId()) : "");
            DrugDict drug = drugMap.get(p.getDrugId());
            map.put("drugName", drug != null ? drug.getName() : "");
            map.put("spec", drug != null ? drug.getSpec() : "");
            map.put("quantity", p.getQuantity());
            map.put("price", drug != null ? drug.getPrice() : BigDecimal.ZERO);
            map.put("amount",
                    drug != null ? drug.getPrice().multiply(BigDecimal.valueOf(p.getQuantity())) : BigDecimal.ZERO);
            map.put("status", p.getStatus());
            map.put("statusText", getPrescriptionStatusText(p.getStatus()));
            map.put("createTime", p.getCreateTime() != null ? p.getCreateTime().format(DATETIME_FMT) : "");
            return map;
        }).toList();

        return Map.of("list", list, "total", pageResult.getTotal(), "aggregation", Map.of("totalAmount", finalTotal));
    }

    private Map<String, Object> queryLabOrders(int page, int size, LocalDate startDate, LocalDate endDate, Long deptId,
            Long doctorId, Integer status) {
        // Get medical records
        LambdaQueryWrapper<MedicalRecord> recordWrapper = new LambdaQueryWrapper<>();
        if (startDate != null)
            recordWrapper.ge(MedicalRecord::getCreateTime, startDate.atStartOfDay());
        if (endDate != null)
            recordWrapper.le(MedicalRecord::getCreateTime, endDate.plusDays(1).atStartOfDay());
        if (doctorId != null)
            recordWrapper.eq(MedicalRecord::getDoctorId, doctorId);

        List<MedicalRecord> records = medicalRecordMapper.selectList(recordWrapper);
        Set<Long> recordIds = records.stream().map(MedicalRecord::getRecordId).collect(Collectors.toSet());
        Map<Long, MedicalRecord> recordMap = records.stream()
                .collect(Collectors.toMap(MedicalRecord::getRecordId, r -> r));

        if (recordIds.isEmpty() && (startDate != null || endDate != null || doctorId != null)) {
            return Map.of("list", List.of(), "total", 0, "aggregation", Map.of());
        }

        // Filter by department
        Map<Long, SysUser> doctorMap = getDoctorMapAll();
        if (deptId != null) {
            Set<Long> deptDoctorIds = doctorMap.values().stream()
                    .filter(u -> deptId.equals(u.getDeptId()))
                    .map(SysUser::getUserId)
                    .collect(Collectors.toSet());
            recordIds = records.stream()
                    .filter(r -> deptDoctorIds.contains(r.getDoctorId()))
                    .map(MedicalRecord::getRecordId)
                    .collect(Collectors.toSet());
        }

        if (recordIds.isEmpty()) {
            return Map.of("list", List.of(), "total", 0, "aggregation", Map.of());
        }

        LambdaQueryWrapper<LabOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LabOrder::getRecordId, recordIds);
        if (status != null)
            wrapper.eq(LabOrder::getStatus, status);
        wrapper.orderByDesc(LabOrder::getCreateTime);

        Page<LabOrder> pageResult = labOrderMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, String> patientNames = getPatientNames(
                records.stream().map(MedicalRecord::getPatientId).collect(Collectors.toSet()));
        Map<Long, String> deptNames = getDeptNames();

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(o -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", o.getOrderId());
            map.put("orderId", o.getOrderId());
            MedicalRecord record = recordMap.get(o.getRecordId());
            map.put("patientName", record != null ? patientNames.getOrDefault(record.getPatientId(), "") : "");
            SysUser doctor = record != null ? doctorMap.get(record.getDoctorId()) : null;
            map.put("doctorName", doctor != null ? doctor.getRealName() : "");
            map.put("deptName", doctor != null ? deptNames.get(doctor.getDeptId()) : "");
            map.put("itemName", o.getItemName());
            map.put("price", o.getPrice());
            map.put("status", o.getStatus());
            map.put("statusText", getLabStatusText(o.getStatus()));
            map.put("resultText", o.getResultText());
            map.put("createTime", o.getCreateTime() != null ? o.getCreateTime().format(DATETIME_FMT) : "");
            return map;
        }).toList();

        return Map.of("list", list, "total", pageResult.getTotal(), "aggregation", Map.of());
    }

    private Map<String, Object> queryUsers(int page, int size, Long deptId, String role, Integer status,
            String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null)
            wrapper.eq(SysUser::getDeptId, deptId);
        if (StringUtils.hasText(role))
            wrapper.eq(SysUser::getRole, role);
        if (status != null)
            wrapper.eq(SysUser::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> pageResult = sysUserMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> deptNames = getDeptNames();

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getUserId());
            map.put("userId", u.getUserId());
            map.put("username", u.getUsername());
            map.put("realName", u.getRealName());
            map.put("role", u.getRole());
            map.put("roleText", getRoleText(u.getRole()));
            map.put("deptId", u.getDeptId());
            map.put("deptName", deptNames.get(u.getDeptId()));
            map.put("phone", u.getPhone());
            map.put("status", u.getStatus());
            map.put("statusText", u.getStatus() == 1 ? "启用" : "禁用");
            map.put("createTime", u.getCreateTime() != null ? u.getCreateTime().format(DATETIME_FMT) : "");
            return map;
        }).toList();

        return Map.of("list", list, "total", pageResult.getTotal());
    }

    private Map<String, Object> queryDepartments(int page, int size, Integer status, String keyword) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        if (status != null)
            wrapper.eq(Department::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Department::getDeptName, keyword);
        }

        Page<Department> pageResult = departmentMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", d.getDeptId());
            map.put("deptId", d.getDeptId());
            map.put("deptName", d.getDeptName());
            map.put("location", d.getLocation());
            map.put("description", d.getDescription());
            map.put("status", d.getStatus());
            map.put("statusText", d.getStatus() == 1 ? "启用" : "禁用");
            return map;
        }).toList();

        return Map.of("list", list, "total", pageResult.getTotal());
    }

    private Map<String, Object> querySchedules(int page, int size, LocalDate startDate, LocalDate endDate, Long deptId,
            Long doctorId) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null)
            wrapper.ge(Schedule::getScheduleDate, startDate);
        if (endDate != null)
            wrapper.le(Schedule::getScheduleDate, endDate);
        if (deptId != null)
            wrapper.eq(Schedule::getDeptId, deptId);
        if (doctorId != null)
            wrapper.eq(Schedule::getDoctorId, doctorId);
        wrapper.orderByDesc(Schedule::getScheduleDate);

        Page<Schedule> pageResult = scheduleMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, SysUser> doctorMap = getDoctorMapAll();
        Map<Long, String> deptNames = getDeptNames();

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getScheduleId());
            map.put("scheduleId", s.getScheduleId());
            SysUser doctor = doctorMap.get(s.getDoctorId());
            map.put("doctorName", doctor != null ? doctor.getRealName() : "");
            map.put("deptName", deptNames.get(s.getDeptId()));
            map.put("scheduleDate", s.getScheduleDate().format(DATE_FMT));
            map.put("shiftType", getShiftLabel(s.getShiftType()));
            map.put("currentCount", s.getCurrentCount());
            map.put("maxQuota", s.getMaxQuota());
            map.put("status", s.getStatus());
            map.put("statusText", s.getStatus() == 1 ? "正常" : "停诊");
            return map;
        }).toList();

        return Map.of("list", list, "total", pageResult.getTotal());
    }

    private Map<String, Object> queryDrugs(int page, int size, Integer status, String keyword) {
        LambdaQueryWrapper<DrugDict> wrapper = new LambdaQueryWrapper<>();
        if (status != null)
            wrapper.eq(DrugDict::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DrugDict::getName, keyword);
        }

        Page<DrugDict> pageResult = drugDictMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", d.getDrugId());
            map.put("drugId", d.getDrugId());
            map.put("name", d.getName());
            map.put("spec", d.getSpec());
            map.put("price", d.getPrice());
            map.put("stockQuantity", d.getStockQuantity());
            map.put("unit", d.getUnit());
            map.put("manufacturer", d.getManufacturer());
            map.put("status", d.getStatus());
            map.put("statusText", d.getStatus() == 1 ? "启用" : "禁用");
            return map;
        }).toList();

        return Map.of("list", list, "total", pageResult.getTotal());
    }

    private Map<String, Object> queryCheckItems(int page, int size, Integer status, String keyword) {
        LambdaQueryWrapper<CheckItem> wrapper = new LambdaQueryWrapper<>();
        if (status != null)
            wrapper.eq(CheckItem::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CheckItem::getItemName, keyword);
        }

        Page<CheckItem> pageResult = checkItemMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getItemId());
            map.put("itemId", c.getItemId());
            map.put("itemName", c.getItemName());
            map.put("itemCode", c.getItemCode());
            map.put("price", c.getPrice());
            map.put("category", c.getCategory());
            map.put("description", c.getDescription());
            map.put("status", c.getStatus());
            map.put("statusText", c.getStatus() == 1 ? "启用" : "禁用");
            return map;
        }).toList();

        return Map.of("list", list, "total", pageResult.getTotal());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void exportData(String type, LocalDate startDate, LocalDate endDate, Long deptId, Long doctorId,
            String role, Long drugId, Integer status, String keyword, boolean excludeChief,
            HttpServletResponse response) {

        // 查询所有数据（不分页）
        Map<String, Object> result = queryData(type, 1, 10000, startDate, endDate, deptId, doctorId, role, drugId,
                status, keyword, excludeChief);
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            if (!list.isEmpty()) {
                // 表头行
                Row headerRow = sheet.createRow(0);
                List<String> headers = new ArrayList<>(list.get(0).keySet());
                for (int i = 0; i < headers.size(); i++) {
                    headerRow.createCell(i).setCellValue(headers.get(i));
                }

                // 数据行
                for (int i = 0; i < list.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Map<String, Object> item = list.get(i);
                    for (int j = 0; j < headers.size(); j++) {
                        Object val = item.get(headers.get(j));
                        row.createCell(j).setCellValue(val != null ? val.toString() : "");
                    }
                }
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + type + "_export.xlsx");
            workbook.write(response.getOutputStream());

        } catch (IOException e) {
            log.error("Export failed", e);
        }
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // 今日挂号数
        LambdaQueryWrapper<Registration> regWrapper = new LambdaQueryWrapper<>();
        regWrapper.ge(Registration::getCreateTime, todayStart).lt(Registration::getCreateTime, todayEnd);
        long todayRegistrations = registrationMapper.selectCount(regWrapper);

        // 医生总数
        LambdaQueryWrapper<SysUser> doctorWrapper = new LambdaQueryWrapper<>();
        doctorWrapper.in(SysUser::getRole, "DOCTOR", "CHIEF");
        long doctors = sysUserMapper.selectCount(doctorWrapper);

        // Today's prescriptions
        LambdaQueryWrapper<Prescription> presWrapper = new LambdaQueryWrapper<>();
        presWrapper.ge(Prescription::getCreateTime, todayStart).lt(Prescription::getCreateTime, todayEnd);
        long todayPrescriptions = prescriptionMapper.selectCount(presWrapper);

        // Today's lab orders
        LambdaQueryWrapper<LabOrder> labWrapper = new LambdaQueryWrapper<>();
        labWrapper.ge(LabOrder::getCreateTime, todayStart).lt(LabOrder::getCreateTime, todayEnd);
        long todayLabOrders = labOrderMapper.selectCount(labWrapper);

        return Map.of(
                "todayRegistrations", todayRegistrations,
                "doctors", doctors,
                "todayPrescriptions", todayPrescriptions,
                "todayLabOrders", todayLabOrders);
    }

    // 辅助方法
    private Map<Long, String> getPatientNames(Set<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty())
            return Map.of();
        LambdaQueryWrapper<PatientInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PatientInfo::getPatientId, patientIds);
        return patientInfoMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(PatientInfo::getPatientId, PatientInfo::getName));
    }

    private Map<Long, SysUser> getDoctorMap(Set<Long> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty())
            return Map.of();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getUserId, doctorIds);
        return sysUserMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u));
    }

    private Map<Long, SysUser> getDoctorMapAll() {
        return sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u));
    }

    private Map<Long, String> getDeptNames() {
        return departmentMapper.selectList(null).stream()
                .collect(Collectors.toMap(Department::getDeptId, Department::getDeptName));
    }

    private Map<Long, DrugDict> getDrugMap() {
        return drugDictMapper.selectList(null).stream()
                .collect(Collectors.toMap(DrugDict::getDrugId, d -> d));
    }

    private String getRegistrationStatusText(Integer status) {
        if (status == null)
            return "";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "候诊中";
            case 3 -> "就诊中";
            case 4 -> "已完成";
            case 5 -> "已取消";
            default -> "未知";
        };
    }

    private String getPrescriptionStatusText(Integer status) {
        if (status == null)
            return "";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发药";
            default -> "未知";
        };
    }

    private String getLabStatusText(Integer status) {
        if (status == null)
            return "";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "待检查";
            case 2 -> "已完成";
            default -> "未知";
        };
    }

    private String getRoleText(String role) {
        if (role == null)
            return "";
        return switch (role) {
            case "ADMIN" -> "管理员";
            case "CHIEF" -> "主任医师";
            case "DOCTOR" -> "医生";
            case "PHARMACY" -> "药房";
            case "LAB" -> "检验科";
            default -> role;
        };
    }

    private String getShiftLabel(String shiftType) {
        if (shiftType == null)
            return "";
        return switch (shiftType.toUpperCase()) {
            case "AM" -> "上午";
            case "PM" -> "下午";
            case "ER" -> "急诊";
            default -> shiftType;
        };
    }
}
