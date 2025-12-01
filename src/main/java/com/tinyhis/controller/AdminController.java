package com.tinyhis.controller;

import com.tinyhis.dto.DrugExcelDTO;
import com.tinyhis.dto.Result;
import com.tinyhis.service.ExcelService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin Controller for management operations
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ExcelService excelService;

    /**
     * Import drugs from Excel file
     */
    @PostMapping("/drugs/import")
    public Result<List<DrugExcelDTO>> importDrugs(@RequestParam("file") MultipartFile file) {
        List<DrugExcelDTO> imported = excelService.importDrugs(file);
        return Result.success(imported);
    }

    /**
     * Export drugs to Excel file
     */
    @GetMapping("/drugs/export")
    public void exportDrugs(HttpServletResponse response) {
        excelService.exportDrugs(response);
    }
}
