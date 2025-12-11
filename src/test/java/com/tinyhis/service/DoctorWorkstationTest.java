package com.tinyhis.service;

import com.tinyhis.dto.EmrRequest;
import com.tinyhis.entity.MedicalRecord;
import com.tinyhis.entity.LabOrder;
import com.tinyhis.entity.Prescription;
import com.tinyhis.entity.Registration;
import com.tinyhis.mapper.RegistrationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用例7: 书写电子病历
 * 用例9: 查看检验结果
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "medical.knowledge.vector-path=target/vector-index-doctor-test",
    "medical.knowledge.path=target/medical-knowledge-doctor-test",
    "spring.datasource.url=jdbc:h2:mem:tinyhis-doctor-test;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class DoctorWorkstationTest {

    @Autowired
    private EmrService emrService;


    @Autowired
    private RegistrationMapper registrationMapper;

    @Test
    void testWriteEmr() {
        // 1. 准备挂号记录 (Status=3 就诊中)
        Registration reg = new Registration();
        reg.setPatientId(1L);
        reg.setDoctorId(2L);
        reg.setScheduleId(1L);
        reg.setStatus(3);
        reg.setFee(new BigDecimal("50.00"));
        registrationMapper.insert(reg);
        Long regId = reg.getRegId();
        assertNotNull(regId);

        // 2. 构造 EmrRequest
        EmrRequest request = new EmrRequest();
        request.setRegId(regId);
        request.setSymptom("头痛");
        request.setDiagnosis("感冒");
        request.setContent("详细病史...");

        // 处方
        List<EmrRequest.PrescriptionItem> prescriptions = new ArrayList<>();
        EmrRequest.PrescriptionItem p1 = new EmrRequest.PrescriptionItem();
        p1.setDrugId(1L);
        p1.setQuantity(10);
        p1.setUsageInstruction("一日三次");
        prescriptions.add(p1);
        request.setPrescriptions(prescriptions);

        // 检验单
        List<EmrRequest.LabOrderItem> labOrders = new ArrayList<>();
        EmrRequest.LabOrderItem l1 = new EmrRequest.LabOrderItem();
        l1.setItemName("血常规");
        l1.setPrice(new BigDecimal("20.00"));
        labOrders.add(l1);
        request.setLabOrders(labOrders);

        // 3. 保存
        MedicalRecord record = emrService.saveEmr(request, 2L); // doctorId=2

        assertNotNull(record);
        assertEquals("头痛", record.getSymptom());
        
        // 4. 验证关联数据
        List<Prescription> savedPrescriptions = emrService.getPrescriptionsByRecord(record.getRecordId());
        assertFalse(savedPrescriptions.isEmpty());
        
        List<LabOrder> savedLabOrders = emrService.getLabOrdersByRecord(record.getRecordId());
        assertFalse(savedLabOrders.isEmpty());
    }
}
