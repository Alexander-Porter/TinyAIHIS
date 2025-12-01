package com.tinyhis.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.tinyhis.dto.DrugExcelDTO;
import com.tinyhis.entity.DrugDict;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.DrugDictMapper;
import com.tinyhis.service.ExcelService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel Service Implementation for drug import/export
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelServiceImpl implements ExcelService {

    private final DrugDictMapper drugDictMapper;

    @Override
    @Transactional
    public List<DrugExcelDTO> importDrugs(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }

        List<DrugExcelDTO> importedDrugs = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), DrugExcelDTO.class, new ReadListener<DrugExcelDTO>() {
                @Override
                public void invoke(DrugExcelDTO data, AnalysisContext context) {
                    // Convert DTO to entity and save
                    DrugDict drug = new DrugDict();
                    drug.setName(data.getName());
                    drug.setSpec(data.getSpec());
                    drug.setPrice(data.getPrice());
                    drug.setStockQuantity(data.getStockQuantity());
                    drug.setUnit(data.getUnit());
                    drug.setManufacturer(data.getManufacturer());
                    drug.setStatus(1);
                    
                    drugDictMapper.insert(drug);
                    importedDrugs.add(data);
                    
                    log.debug("Imported drug: {}", data.getName());
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Drug import completed, total: {} records", importedDrugs.size());
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new BusinessException("文件读取失败: " + e.getMessage());
        }

        return importedDrugs;
    }

    @Override
    public void exportDrugs(HttpServletResponse response) {
        try {
            // Set response headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("药品列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // Query all drugs
            List<DrugDict> drugs = drugDictMapper.selectList(null);
            
            // Convert to DTOs
            List<DrugExcelDTO> dtos = drugs.stream().map(drug -> {
                DrugExcelDTO dto = new DrugExcelDTO();
                dto.setName(drug.getName());
                dto.setSpec(drug.getSpec());
                dto.setPrice(drug.getPrice());
                dto.setStockQuantity(drug.getStockQuantity());
                dto.setUnit(drug.getUnit());
                dto.setManufacturer(drug.getManufacturer());
                return dto;
            }).toList();

            // Write to Excel
            EasyExcel.write(response.getOutputStream(), DrugExcelDTO.class)
                    .sheet("药品列表")
                    .doWrite(dtos);
                    
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }
}
