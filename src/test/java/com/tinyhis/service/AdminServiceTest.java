package com.tinyhis.service;

import com.tinyhis.entity.Department;
import com.tinyhis.entity.Schedule;
import com.tinyhis.entity.SysUser;
import com.tinyhis.mapper.DepartmentMapper;
import com.tinyhis.mapper.ScheduleMapper;
import com.tinyhis.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用例12: 基础数据维护
 * 用例13: 排班管理
 * 用例15: 数据查询
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "medical.knowledge.vector-path=target/vector-index-admin-test",
    "medical.knowledge.path=target/medical-knowledge-admin-test",
    "spring.datasource.url=jdbc:h2:mem:tinyhis-admin-test;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class AdminServiceTest {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        ValueOperations valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    /**
     * 用例12: 科室管理
     */
    @Test
    void testDepartmentManagement() {
        // 1. 添加科室
        Department dept = new Department();
        dept.setDeptName("测试科室" + System.currentTimeMillis());
        dept.setLocation("门诊楼3层");
        dept.setDescription("测试科室描述");
        dept.setStatus(1);
        departmentMapper.insert(dept);
        assertNotNull(dept.getDeptId());
        
        // 2. 查询科室
        Department retrieved = scheduleService.getDepartmentById(dept.getDeptId());
        assertEquals(dept.getDeptName(), retrieved.getDeptName());
        
        // 3. 查询所有科室
        List<Department> allDepts = scheduleService.getAllDepartments();
        assertFalse(allDepts.isEmpty());
        
        // 4. 修改科室
        dept.setLocation("门诊楼4层");
        departmentMapper.updateById(dept);
        Department updated = departmentMapper.selectById(dept.getDeptId());
        assertEquals("门诊楼4层", updated.getLocation());
    }

    /**
     * 用例12: 医生账号管理
     */
    @Test
    void testDoctorManagement() {
        // 1. 添加医生
        SysUser doctor = new SysUser();
        doctor.setUsername("testdoc_" + System.currentTimeMillis());
        doctor.setPassword("$2a$10$encrypted"); // 加密后的密码
        doctor.setRealName("测试医生");
        doctor.setRole("DOCTOR"); // 字符串类型
        doctor.setDeptId(1L);
        doctor.setPhone("13800138000");
        doctor.setStatus(1);
        sysUserMapper.insert(doctor);
        assertNotNull(doctor.getUserId());
        
        // 2. 查询科室医生
        List<SysUser> doctors = scheduleService.getDoctorsByDept(1L);
        assertNotNull(doctors);
        
        // 3. 修改医生信息
        doctor.setPhone("13900139000");
        sysUserMapper.updateById(doctor);
        SysUser updated = sysUserMapper.selectById(doctor.getUserId());
        assertEquals("13900139000", updated.getPhone());
        
        // 4. 停用账号
        doctor.setStatus(0);
        sysUserMapper.updateById(doctor);
        SysUser disabled = sysUserMapper.selectById(doctor.getUserId());
        assertEquals(0, disabled.getStatus());
    }

    /**
     * 用例13: 排班管理
     */
    @Test
    void testScheduleManagement() {
        // 1. 创建排班
        Schedule schedule = new Schedule();
        schedule.setDoctorId(2L);
        schedule.setDeptId(1L);  // 必填字段
        schedule.setRoomId(1L);
        schedule.setScheduleDate(LocalDate.now().plusDays(7));
        schedule.setShiftType("AM"); // 字符串类型
        schedule.setMaxQuota(30);
        schedule.setCurrentCount(0);
        schedule.setStatus(1);
        
        Schedule saved = scheduleService.saveSchedule(schedule);
        assertNotNull(saved);
        assertNotNull(saved.getScheduleId());
        
        // 2. 查询排班
        Schedule retrieved = scheduleService.getScheduleById(saved.getScheduleId());
        assertEquals(30, retrieved.getMaxQuota());
        
        // 3. 修改排班限号
        retrieved.setMaxQuota(35);
        Schedule updated = scheduleService.saveSchedule(retrieved);
        assertEquals(35, updated.getMaxQuota());
        
        // 4. 查询排班列表
        var scheduleList = scheduleService.getScheduleList(
            null, 
            LocalDate.now(), 
            LocalDate.now().plusDays(14)
        );
        assertNotNull(scheduleList);
    }

    /**
     * 用例15: Dashboard统计
     */
    @Test
    void testDashboardStats() {
        var stats = dashboardService.getDashboardStats();
        assertNotNull(stats);
        // Dashboard返回Map类型的统计数据
        assertTrue(stats instanceof java.util.Map);
    }
}
