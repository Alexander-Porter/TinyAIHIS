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
}
