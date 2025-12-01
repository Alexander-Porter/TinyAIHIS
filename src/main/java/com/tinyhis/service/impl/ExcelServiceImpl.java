package com.tinyhis.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.tinyhis.dto.CheckItemExcelDTO;
import com.tinyhis.dto.DrugExcelDTO;
import com.tinyhis.dto.DrugUsageReportDTO;
import com.tinyhis.entity.CheckItem;
import com.tinyhis.entity.DrugDict;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.CheckItemMapper;
import com.tinyhis.mapper.DrugDictMapper;
import com.tinyhis.mapper.PrescriptionMapper;
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
import java.time.LocalDateTime;
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
    private final CheckItemMapper checkItemMapper;
    private final PrescriptionMapper prescriptionMapper;

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

    @Override
    @Transactional
    public List<CheckItemExcelDTO> importCheckItems(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }

        List<CheckItemExcelDTO> importedItems = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), CheckItemExcelDTO.class, new ReadListener<CheckItemExcelDTO>() {
                @Override
                public void invoke(CheckItemExcelDTO data, AnalysisContext context) {
                    CheckItem item = new CheckItem();
                    item.setItemName(data.getItemName());
                    item.setItemCode(data.getItemCode());
                    item.setPrice(data.getPrice());
                    item.setCategory(data.getCategory());
                    item.setDescription(data.getDescription());
                    item.setStatus(1);
                    
                    checkItemMapper.insert(item);
                    importedItems.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Check item import completed, total: {} records", importedItems.size());
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new BusinessException("文件读取失败: " + e.getMessage());
        }

        return importedItems;
    }

    @Override
    public void exportCheckItems(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("检查项目列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<CheckItem> items = checkItemMapper.selectList(null);
            
            List<CheckItemExcelDTO> dtos = items.stream().map(item -> {
                CheckItemExcelDTO dto = new CheckItemExcelDTO();
                dto.setItemName(item.getItemName());
                dto.setItemCode(item.getItemCode());
                dto.setPrice(item.getPrice());
                dto.setCategory(item.getCategory());
                dto.setDescription(item.getDescription());
                return dto;
            }).toList();

            EasyExcel.write(response.getOutputStream(), CheckItemExcelDTO.class)
                    .sheet("检查项目列表")
                    .doWrite(dtos);
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportDrugUsageReport(HttpServletResponse response, LocalDateTime startDate, LocalDateTime endDate, List<Long> deptIds) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("药品使用统计", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<DrugUsageReportDTO> report = prescriptionMapper.getDrugUsageReport(startDate, endDate, deptIds);

            EasyExcel.write(response.getOutputStream(), DrugUsageReportDTO.class)
                    .sheet("药品使用统计")
                    .doWrite(report);
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }
}
