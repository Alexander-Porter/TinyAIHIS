package com.tinyhis.service;

import com.tinyhis.dto.LabResultRequest;
import com.tinyhis.dto.LabOrderView;
import com.tinyhis.entity.LabOrder;
import com.tinyhis.mapper.LabOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用例10: 检验结果录入与报告发布
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "medical.knowledge.vector-path=target/vector-index-lab-test",
    "medical.knowledge.path=target/medical-knowledge-lab-test"
})
class LabServiceTest {

    @Autowired
    private LabService labService;

    @Autowired
    private LabOrderMapper labOrderMapper;

    @Test
    void testLabResultEntry() {
        // 1. 创建待检验的检验单 (Status=1 已缴费)
        LabOrder order = new LabOrder();
        order.setRecordId(1L);
        order.setItemName("血常规");
        order.setPrice(new BigDecimal("30.00"));
        order.setStatus(1);
        labOrderMapper.insert(order);
        Long orderId = order.getOrderId();
        assertNotNull(orderId);

        // 2. 查询待检验列表
        List<LabOrderView> pending = labService.getOrders("1");
        assertFalse(pending.isEmpty());
        assertTrue(pending.stream().anyMatch(o -> o.getOrderId().equals(orderId)));

        // 3. 录入结果
        LabResultRequest request = new LabResultRequest();
        request.setOrderId(orderId);
        request.setResultText("白细胞正常");
        
        LabOrder result = labService.submitResult(request);
        
        // 4. 验证
        assertNotNull(result);
        assertEquals(2, result.getStatus()); // 已完成
        assertEquals("白细胞正常", result.getResultText());
    }
}
