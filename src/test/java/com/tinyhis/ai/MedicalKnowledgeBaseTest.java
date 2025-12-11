package com.tinyhis.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicalKnowledgeBaseTest {

    private MedicalKnowledgeBase knowledgeBase;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        knowledgeBase = new MedicalKnowledgeBase();

        // 注入测试用的临时路径
        Path kbPath = tempDir.resolve("knowledge");
        Path vectorPath = tempDir.resolve("vector");
        kbPath.toFile().mkdirs();
        vectorPath.toFile().mkdirs();

        ReflectionTestUtils.setField(knowledgeBase, "knowledgePath", kbPath.toUri().toString());
        ReflectionTestUtils.setField(knowledgeBase, "vectorIndexPath", vectorPath.toUri().toString());
        ReflectionTestUtils.setField(knowledgeBase, "embeddingModel", "BAAI/bge-m3");
        ReflectionTestUtils.setField(knowledgeBase, "defaultKeywordWeight", 0.7f); // 测试时优先使用关键词
                                                                                   // 在没有API密钥时优先使用关键词搜索

        // 初始化（模拟嵌入API或主要依赖关键词搜索）
        knowledgeBase.init();
    }

    @Test
    void testAddAndSearchDocument() {
        MedicalDocument doc = new MedicalDocument();
        doc.setId("test_flu.json");
        doc.setDiseaseName("流行性感冒");
        doc.setDepartment("内科");
        doc.setContent("{\"症状\": \"发热, 咳嗽\", \"治疗\": \"休息, 多喝水\"}");

        knowledgeBase.addDocument(doc);

        // 强制刷新
        try {
            Thread.sleep(1000); // 等待异步索引/提交完成，尽管代码看起来是同步的
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 通过关键词（症状）搜索
        List<MedicalDocument> results = knowledgeBase.hybridSearch("发热", 5, 1.0f); // 关键词权重为1.0

        assertFalse(results.isEmpty(), "应该能通过症状找到文档");
        assertEquals("流行性感冒", results.get(0).getDiseaseName());
    }

    @Test
    void testDepartmentInference() {
        // 如果需要，使用反射访问私有方法，或者通过添加逻辑进行测试
        // inferDepartment是私有的，但如果我们没有设置它，addDocument会调用它
        // 实际上是loadDocument设置了它
        // 让我们测试按科室筛选的搜索功能

        MedicalDocument doc1 = new MedicalDocument();
        doc1.setId("1");
        doc1.setDiseaseName("A");
        doc1.setDepartment("DeptA");
        doc1.setContent("Content A");

        MedicalDocument doc2 = new MedicalDocument();
        doc2.setId("2");
        doc2.setDiseaseName("B");
        doc2.setDepartment("DeptB");
        doc2.setContent("Content B");

        knowledgeBase.addDocument(doc1);
        knowledgeBase.addDocument(doc2);

        List<MedicalDocument> deptAResults = knowledgeBase.getAllDocuments(null, "DeptA");
        assertEquals(1, deptAResults.size());
        assertEquals("A", deptAResults.get(0).getDiseaseName());
    }
}
