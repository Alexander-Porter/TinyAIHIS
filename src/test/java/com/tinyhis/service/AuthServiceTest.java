package com.tinyhis.service;

import com.tinyhis.dto.PatientRegisterRequest;
import com.tinyhis.entity.PatientInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用例1: 用户注册测试
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "medical.knowledge.vector-path=target/vector-index-auth-test",
    "medical.knowledge.path=target/medical-knowledge-auth-test",
    "spring.datasource.url=jdbc:h2:mem:tinyhis-auth-test;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void testPatientRegistration() {
        PatientRegisterRequest request = new PatientRegisterRequest();
        request.setName("测试患者" + System.currentTimeMillis());
        request.setPhone("138" + (System.currentTimeMillis() % 100000000));
        request.setPassword("Test123");
        request.setGender(1);
        request.setAge(30);

        PatientInfo patient = authService.registerPatient(request);

        assertNotNull(patient);
        assertNotNull(patient.getPatientId());
        assertEquals(request.getName(), patient.getName());
    }
}
