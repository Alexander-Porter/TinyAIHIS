package com.tinyhis.integration;

import com.tinyhis.dto.CheckInRequest;
import com.tinyhis.dto.RegistrationRequest;
import com.tinyhis.entity.Registration;
import com.tinyhis.mapper.RegistrationMapper;
import com.tinyhis.service.DoctorWorkstationService;
import com.tinyhis.service.RegistrationService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PatientJourneyTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private DoctorWorkstationService doctorWorkstationService;

    @Autowired
    private RegistrationMapper registrationMapper;

    @MockBean
    private StringRedisTemplate redisTemplate;

    private static Long regId;

    @Test
    @Order(1)
    void testRegistration() {
        // Mock Redis
        ListOperations listOps = mock(ListOperations.class);
        ValueOperations valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(0L);

        RegistrationRequest req = new RegistrationRequest();
        req.setPatientId(1L);
        req.setScheduleId(1L);

        Registration dummy = registrationService.createRegistration(req);
        assertNotNull(dummy);

        // Simulating Async Consumer: Manually insert record into DB
        Registration realReg = new Registration();
        realReg.setPatientId(1L);
        realReg.setScheduleId(1L);
        realReg.setDoctorId(dummy.getDoctorId()); // 2L from schedule 1
        realReg.setStatus(0);
        realReg.setFee(new BigDecimal("50.00"));
        realReg.setQueueNumber(1);

        registrationMapper.insert(realReg);

        assertNotNull(realReg.getRegId());
        regId = realReg.getRegId();
    }

    @Test
    @Order(2)
    void testPaymentAndCheckIn() {
        assertNotNull(regId, "Registration ID should be set");

        // Mock Redis for queue operations (addToQueue)
        ListOperations listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        ValueOperations valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Pay
        Registration paidReg = registrationService.payRegistration(regId);
        // Expect status 1 or 2
        assertTrue(paidReg.getStatus() >= 1);

        if (paidReg.getStatus() == 1) {
            // Check In
            CheckInRequest checkInReq = new CheckInRequest();
            checkInReq.setRegId(regId);

            Registration checkedInReg = registrationService.checkIn(checkInReq);
            assertEquals(2, checkedInReg.getStatus());
        }
    }

    @Test
    @Order(3)
    void testDoctorCallAndConsultation() {
        assertNotNull(regId);

        ListOperations listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);

        // Doctor calls
        Registration callingReg = registrationService.callSpecificPatient(2L, regId);
        assertEquals(3, callingReg.getStatus());

        // Complete
        Registration completedReg = registrationService.completeConsultation(regId);
        assertEquals(4, completedReg.getStatus());
    }
}
