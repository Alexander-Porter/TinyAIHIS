package com.tinyhis.service;

import com.tinyhis.dto.DrugExcelDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Excel Service Interface for import/export
 */
public interface ExcelService {

    /**
     * Import drugs from Excel file
     */
    List<DrugExcelDTO> importDrugs(MultipartFile file);

    /**
     * Export drugs to Excel file
     */
    void exportDrugs(HttpServletResponse response);

    /**
     * Import check items from Excel file
     */
    List<com.tinyhis.dto.CheckItemExcelDTO> importCheckItems(MultipartFile file);

    /**
     * Export check items to Excel file
     */
    void exportCheckItems(HttpServletResponse response);

    /**
     * Export drug usage report
     */
    void exportDrugUsageReport(HttpServletResponse response, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, java.util.List<Long> deptIds);
}
