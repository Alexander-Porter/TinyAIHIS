package com.tinyhis.service;

import com.tinyhis.dto.PrescriptionDetailDTO;
import com.tinyhis.entity.DrugDict;
import com.tinyhis.entity.Prescription;

import java.util.List;

/**
 * Pharmacy Service Interface
 */
public interface PharmacyService {

    /**
     * Get all drugs
     */
    List<DrugDict> getAllDrugs();

    /**
     * Get drug by ID
     */
    DrugDict getDrugById(Long drugId);

    /**
     * Search drugs by name
     */
    List<DrugDict> searchDrugs(String keyword);

    /**
     * Get paid prescriptions waiting for dispensing
     */
    List<PrescriptionDetailDTO> getPaidPrescriptions();

    /**
     * Pay for prescription (模拟缴费)
     */
    Prescription payPrescription(Long presId);

    /**
     * Pay all prescriptions by record (模拟缴费)
     */
    List<Prescription> payPrescriptionsByRecord(Long recordId);

    /**
     * Dispense prescription (发药)
     */
    Prescription dispense(Long presId);

    /**
     * Update drug stock
     */
    DrugDict updateStock(Long drugId, Integer quantity);

    /**
     * Add new drug
     */
    DrugDict addDrug(DrugDict drug);
}
