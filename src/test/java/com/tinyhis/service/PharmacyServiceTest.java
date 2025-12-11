package com.tinyhis.service;

import com.tinyhis.dto.PrescriptionDetailDTO;
import com.tinyhis.entity.DrugDict;
import com.tinyhis.entity.Prescription;
import com.tinyhis.mapper.PrescriptionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用例11: 处方审核、发药与库存管理
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "medical.knowledge.vector-path=target/vector-index-pharmacy-test",
    "medical.knowledge.path=target/medical-knowledge-pharmacy-test",
    "spring.datasource.url=jdbc:h2:mem:tinyhis-pharmacy-test;DB_CLOSE_DELAY=-1;MODE=MySQL"
})
class PharmacyServiceTest {

    @Autowired
    private PharmacyService pharmacyService;

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Test
    void testDrugManagement() {
        // 1. 添加药品
        DrugDict drug = new DrugDict();
        drug.setName("测试药品" + System.currentTimeMillis());
        drug.setSpec("100mg*10片");
        drug.setPrice(new BigDecimal("25.00"));
        drug.setStockQuantity(100);
        drug.setUnit("盒");
        drug.setManufacturer("测试药厂");
        drug.setStatus(1);
        
        DrugDict savedDrug = pharmacyService.addDrug(drug);
        assertNotNull(savedDrug);
        assertNotNull(savedDrug.getDrugId());
        
        // 2. 查询药品
        DrugDict retrieved = pharmacyService.getDrugById(savedDrug.getDrugId());
        assertEquals(savedDrug.getName(), retrieved.getName());
        
        // 3. 搜索药品
        List<DrugDict> searchResults = pharmacyService.searchDrugs("测试");
        assertFalse(searchResults.isEmpty());
        
        // 4. 更新库存
        DrugDict updated = pharmacyService.updateStock(savedDrug.getDrugId(), 50);
        assertEquals(150, updated.getStockQuantity()); // 100 + 50
    }

    @Test
    void testPrescriptionDispensing() {
        // 1. 先创建药品
        DrugDict drug = new DrugDict();
        drug.setName("发药测试药品" + System.currentTimeMillis());
        drug.setSpec("50mg*20片");
        drug.setPrice(new BigDecimal("15.00"));
        drug.setStockQuantity(100);
        drug.setUnit("盒");
        drug.setStatus(1);
        DrugDict savedDrug = pharmacyService.addDrug(drug);
        
        // 2. 创建处方 (status=1 已缴费)
        Prescription prescription = new Prescription();
        prescription.setRecordId(1L);
        prescription.setDrugId(savedDrug.getDrugId());
        prescription.setQuantity(5);
        prescription.setUsageInstruction("一日三次，一次一片");
        prescription.setStatus(1); // 已缴费
        prescriptionMapper.insert(prescription);
        Long presId = prescription.getPresId();
        assertNotNull(presId);
        
        // 3. 查询待发药处方
        List<PrescriptionDetailDTO> paidPrescriptions = pharmacyService.getPaidPrescriptions();
        assertTrue(paidPrescriptions.stream().anyMatch(p -> p.getPresId().equals(presId)));
        
        // 4. 发药
        Prescription dispensed = pharmacyService.dispense(presId);
        assertNotNull(dispensed);
        assertEquals(2, dispensed.getStatus()); // 已发药
        
        // 5. 验证库存扣减
        DrugDict afterDispense = pharmacyService.getDrugById(savedDrug.getDrugId());
        assertEquals(95, afterDispense.getStockQuantity()); // 100 - 5
    }

    @Test
    void testGetAllDrugs() {
        List<DrugDict> allDrugs = pharmacyService.getAllDrugs();
        assertNotNull(allDrugs);
    }
}
