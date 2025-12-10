package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyhis.mapper.RegistrationMapper;
import com.tinyhis.mapper.SysUserMapper;
import com.tinyhis.service.DashboardService;
import com.tinyhis.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final RegistrationMapper registrationMapper;
    private final SysUserMapper sysUserMapper;
    private final com.tinyhis.mapper.PrescriptionMapper prescriptionMapper;
    private final com.tinyhis.mapper.LabOrderMapper labOrderMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_KEY = "dashboard:stats";
    private static final long CACHE_EXPIRE_HOURS = 1;

    @Override
    public Map<String, Object> getDashboardStats() {
        // Try getting from cache
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (Exception e) {
            log.warn("Failed to read dashboard stats from cache", e);
        }

        // Calculate and cache
        Map<String, Object> stats = calculateStats();
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(stats), CACHE_EXPIRE_HOURS,
                    TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to write dashboard stats to cache", e);
        }

        return stats;
    }

    @Override
    public void refreshDashboardStats() {
        redisTemplate.delete(CACHE_KEY);
        getDashboardStats();
    }

    private Map<String, Object> calculateStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);

        // 1. 7-day flow stats (Registrations per day)
        // This is a simplified query. Ideally use a custom mapper query for
        // aggregation.
        // Assuming we can just query all regs for last 7 days and aggregate in Java for
        // simplicity if volume is low,
        // or use @Select in mapper.
        // For TinyHIS, let's assume low volume and aggregate in memory or use a simple
        // loop implementation.

        List<Map<String, Object>> flowStats = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = sevenDaysAgo.plusDays(i);
            QueryWrapper<com.tinyhis.entity.Registration> wrapper = new QueryWrapper<>();
            // Removed invalid column reference 'visit_date'. Using create_time range check
            // below.
            // Checking Registration.java from memory/view_file...
            // It had scheduling/visit info. Usually 'visit_date' or linked schedule date.
            // Let's assume we query by `visit_date` column if it exists or join schedule.
            // Wait, Registration usually has scheduleId. Schedule has date.
            // Let's use `create_time` as proxy for "Registration Flow" or link to schedule.
            // Ideally: Count registrations where visit_date matches.
            // Let's use a simpler approach:
            // QueryWrapper.ge("create_time", date.atStartOfDay()).lt("create_time",
            // date.plusDays(1).atStartOfDay())
            wrapper.ge("create_time", date.atStartOfDay())
                    .lt("create_time", date.plusDays(1).atStartOfDay());
            Long count = registrationMapper.selectCount(wrapper);

            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.toString());
            dayStat.put("count", count);
            flowStats.add(dayStat);
        }
        stats.put("flowStats", flowStats);

        // 2. Top Doctors (Most registrations in last 7 days)
        // Group by doctorId
        QueryWrapper<com.tinyhis.entity.Registration> topDocWrapper = new QueryWrapper<>();
        topDocWrapper.ge("create_time", sevenDaysAgo.atStartOfDay());
        // We really need aggregation here.
        // Let's doing it in memory: fetch basic fields
        topDocWrapper.select("doctor_id");
        List<com.tinyhis.entity.Registration> regs = registrationMapper.selectList(topDocWrapper);

        Map<Long, Long> docCounts = regs.stream()
                .collect(Collectors.groupingBy(com.tinyhis.entity.Registration::getDoctorId, Collectors.counting()));

        List<Map<String, Object>> topDoctors = docCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> docStat = new HashMap<>();
                    docStat.put("doctorId", entry.getKey());
                    docStat.put("count", entry.getValue());
                    // Name lookup
                    SysUser doc = sysUserMapper.selectById(entry.getKey());
                    docStat.put("name", doc != null ? doc.getRealName() : "Unknown");
                    return docStat;
                })
                .collect(Collectors.toList());

        stats.put("topDoctors", topDoctors);

        // 3. Summary Stats (for cards)
        // Today Registrations
        QueryWrapper<com.tinyhis.entity.Registration> todayRegWrapper = new QueryWrapper<>();
        todayRegWrapper.ge("create_time", today.atStartOfDay())
                .lt("create_time", today.plusDays(1).atStartOfDay());
        stats.put("todayRegistrations", registrationMapper.selectCount(todayRegWrapper));

        // Total Doctors
        stats.put("doctors", sysUserMapper.selectCount(new QueryWrapper<SysUser>().in("role", "DOCTOR", "CHIEF")));

        // Today Prescriptions (Mock or real if mapper exists)
        // Since we don't have PrescriptionMapper injected yet, let's inject it or use
        // placeholders if necessary.
        // Waiting for dependency injection... NO, we have imports, let's just assume we
        // can get it or return 0 for now to avoid compilation error if mapper is
        // missing.
        // Actually, we don't have PrescriptionMapper injected. Let me check the class
        // fields.
        // Today Prescriptions
        QueryWrapper<com.tinyhis.entity.Prescription> presWrapper = new QueryWrapper<>();
        presWrapper.ge("create_time", today.atStartOfDay())
                .lt("create_time", today.plusDays(1).atStartOfDay());
        stats.put("todayPrescriptions", prescriptionMapper.selectCount(presWrapper));

        // Today Lab Orders
        QueryWrapper<com.tinyhis.entity.LabOrder> labWrapper = new QueryWrapper<>();
        labWrapper.ge("create_time", today.atStartOfDay())
                .lt("create_time", today.plusDays(1).atStartOfDay());
        stats.put("todayLabOrders", labOrderMapper.selectCount(labWrapper));

        return stats;

    }
}
