package com.tinyhis.service;

import com.tinyhis.dto.Result;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.Map;

/**
 * Data Query Service for flexible querying and exporting of all entity types.
 * Supports:
 * - Registration records
 * - Prescription records
 * - Lab order records
 * - User data
 * - Department data
 * - Schedule data
 * - Drug data
 * - Check item data
 */
public interface DataQueryService {
    
    /**
     * Query data with flexible filters
     * 
     * @param type Entity type: registration, prescription, lab, user, department, schedule, drug, checkItem
     * @param page Page number (1-based)
     * @param size Page size
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param deptId Optional department filter
     * @param doctorId Optional doctor filter
     * @param role Optional role filter (for users)
     * @param drugId Optional drug filter (for prescriptions)
     * @param status Optional status filter
     * @param keyword Optional keyword search
     * @param excludeChief Whether to exclude chief doctors (for prescription analysis)
     * @return Query result with list, total, and optional aggregation
     */
    Map<String, Object> queryData(
            String type,
            int page,
            int size,
            LocalDate startDate,
            LocalDate endDate,
            Long deptId,
            Long doctorId,
            String role,
            Long drugId,
            Integer status,
            String keyword,
            boolean excludeChief
    );
    
    /**
     * Export data to Excel
     */
    void exportData(
            String type,
            LocalDate startDate,
            LocalDate endDate,
            Long deptId,
            Long doctorId,
            String role,
            Long drugId,
            Integer status,
            String keyword,
            boolean excludeChief,
            HttpServletResponse response
    );
    
    /**
     * Get dashboard statistics
     */
    Map<String, Object> getDashboardStats();
}
