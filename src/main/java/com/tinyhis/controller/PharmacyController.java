package com.tinyhis.controller;

import com.tinyhis.dto.PrescriptionDetailDTO;
import com.tinyhis.dto.Result;
import com.tinyhis.entity.DrugDict;
import com.tinyhis.entity.Prescription;
import com.tinyhis.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pharmacy Controller (药房)
 */
@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService pharmacyService;

    /**
     * Get all drugs
     */
    @GetMapping("/drugs")
    public Result<List<DrugDict>> getAllDrugs() {
        List<DrugDict> drugs = pharmacyService.getAllDrugs();
        return Result.success(drugs);
    }

    /**
     * Get drug by ID
     */
    @GetMapping("/drug/{drugId}")
    public Result<DrugDict> getDrug(@PathVariable Long drugId) {
        DrugDict drug = pharmacyService.getDrugById(drugId);
        return Result.success(drug);
    }

    /**
     * Search drugs by name
     */
    @GetMapping("/drugs/search")
    public Result<List<DrugDict>> searchDrugs(@RequestParam String keyword) {
        List<DrugDict> drugs = pharmacyService.searchDrugs(keyword);
        return Result.success(drugs);
    }

    /**
     * Get paid prescriptions waiting for dispensing
     */
    @GetMapping("/prescriptions/paid")
    public Result<List<PrescriptionDetailDTO>> getPaidPrescriptions() {
        List<PrescriptionDetailDTO> prescriptions = pharmacyService.getPaidPrescriptions();
        return Result.success(prescriptions);
    }

    /**
     * Pay for single prescription (模拟缴费)
     */
    @PostMapping("/prescription/pay/{presId}")
    public Result<Prescription> payPrescription(@PathVariable Long presId) {
        Prescription prescription = pharmacyService.payPrescription(presId);
        return Result.success(prescription);
    }

    /**
     * Pay for all prescriptions in a record (模拟缴费)
     */
    @PostMapping("/prescriptions/pay/record/{recordId}")
    public Result<List<Prescription>> payPrescriptionsByRecord(@PathVariable Long recordId) {
        List<Prescription> prescriptions = pharmacyService.payPrescriptionsByRecord(recordId);
        return Result.success(prescriptions);
    }

    /**
     * Dispense prescription (发药)
     */
    @PostMapping("/dispense/{presId}")
    public Result<Prescription> dispense(@PathVariable Long presId) {
        Prescription prescription = pharmacyService.dispense(presId);
        return Result.success(prescription);
    }

    /**
     * Update drug stock
     */
    @PostMapping("/drug/stock")
    public Result<DrugDict> updateStock(@RequestParam Long drugId, @RequestParam Integer quantity) {
        DrugDict drug = pharmacyService.updateStock(drugId, quantity);
        return Result.success(drug);
    }

    /**
     * Add new drug
     */
    @PostMapping("/drug/add")
    public Result<DrugDict> addDrug(@RequestBody DrugDict drug) {
        DrugDict saved = pharmacyService.addDrug(drug);
        return Result.success(saved);
    }
}
