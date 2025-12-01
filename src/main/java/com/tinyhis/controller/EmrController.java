package com.tinyhis.controller;

import com.tinyhis.dto.EmrRequest;
import com.tinyhis.dto.PrescriptionDetailDTO;
import com.tinyhis.dto.Result;
import com.tinyhis.entity.*;
import com.tinyhis.service.EmrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EMR (Electronic Medical Record) Controller
 */
@RestController
@RequestMapping("/api/emr")
@RequiredArgsConstructor
public class EmrController {

    private final EmrService emrService;

    /**
     * Save EMR with prescriptions and lab orders
     */
    @PostMapping("/save")
    public Result<MedicalRecord> saveEmr(@Valid @RequestBody EmrRequest request,
                                         @RequestHeader("X-Doctor-Id") Long doctorId) {
        MedicalRecord record = emrService.saveEmr(request, doctorId);
        return Result.success(record);
    }

    /**
     * Get medical record by ID
     */
    @GetMapping("/{recordId}")
    public Result<MedicalRecord> getRecord(@PathVariable Long recordId) {
        MedicalRecord record = emrService.getRecordById(recordId);
        return Result.success(record);
    }

    /**
     * Get medical records by patient
     */
    @GetMapping("/patient/{patientId}")
    public Result<List<MedicalRecord>> getPatientRecords(@PathVariable Long patientId) {
        List<MedicalRecord> records = emrService.getRecordsByPatient(patientId);
        return Result.success(records);
    }

    /**
     * Get medical record by registration
     */
    @GetMapping("/registration/{regId}")
    public Result<MedicalRecord> getRecordByReg(@PathVariable Long regId) {
        MedicalRecord record = emrService.getRecordByRegId(regId);
        return Result.success(record);
    }

    /**
     * Get prescriptions by record
     */
    @GetMapping("/prescriptions/{recordId}")
    public Result<List<PrescriptionDetailDTO>> getPrescriptions(@PathVariable Long recordId) {
        List<PrescriptionDetailDTO> prescriptions = emrService.getPrescriptionDetails(recordId);
        return Result.success(prescriptions);
    }

    /**
     * Get lab orders by record
     */
    @GetMapping("/laborders/{recordId}")
    public Result<List<LabOrder>> getLabOrders(@PathVariable Long recordId) {
        List<LabOrder> orders = emrService.getLabOrdersByRecord(recordId);
        return Result.success(orders);
    }

    /**
     * Get EMR templates by department
     */
    @GetMapping("/templates")
    public Result<List<EmrTemplate>> getTemplates(@RequestParam(required = false) Long deptId) {
        List<EmrTemplate> templates = emrService.getTemplatesByDept(deptId);
        return Result.success(templates);
    }

    /**
     * Save EMR template
     */
    @PostMapping("/template/save")
    public Result<EmrTemplate> saveTemplate(@RequestBody EmrTemplate template) {
        EmrTemplate saved = emrService.saveTemplate(template);
        return Result.success(saved);
    }

    /**
     * Delete EMR template
     */
    @DeleteMapping("/template/{tplId}")
    public Result<Boolean> deleteTemplate(@PathVariable Long tplId) {
        boolean result = emrService.deleteTemplate(tplId);
        return Result.success(result);
    }
}
