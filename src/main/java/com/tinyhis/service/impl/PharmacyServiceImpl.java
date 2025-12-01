package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.entity.DrugDict;
import com.tinyhis.entity.Prescription;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.DrugDictMapper;
import com.tinyhis.mapper.PrescriptionMapper;
import com.tinyhis.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Pharmacy Service Implementation
 */
@Service
@RequiredArgsConstructor
public class PharmacyServiceImpl implements PharmacyService {

    private final DrugDictMapper drugDictMapper;
    private final PrescriptionMapper prescriptionMapper;

    @Override
    public List<DrugDict> getAllDrugs() {
        LambdaQueryWrapper<DrugDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DrugDict::getStatus, 1);
        return drugDictMapper.selectList(wrapper);
    }

    @Override
    public DrugDict getDrugById(Long drugId) {
        return drugDictMapper.selectById(drugId);
    }

    @Override
    public List<DrugDict> searchDrugs(String keyword) {
        LambdaQueryWrapper<DrugDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(DrugDict::getName, keyword)
               .eq(DrugDict::getStatus, 1);
        return drugDictMapper.selectList(wrapper);
    }

    @Override
    public List<Prescription> getPaidPrescriptions() {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getStatus, 1) // Paid, waiting for dispensing
               .orderByAsc(Prescription::getCreateTime);
        return prescriptionMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Prescription payPrescription(Long presId) {
        Prescription prescription = prescriptionMapper.selectById(presId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }

        if (prescription.getStatus() != 0) {
            throw new BusinessException("该处方已支付或状态异常");
        }

        // 模拟缴费成功
        prescription.setStatus(1); // Paid
        prescriptionMapper.updateById(prescription);
        return prescription;
    }

    @Override
    @Transactional
    public List<Prescription> payPrescriptionsByRecord(Long recordId) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getRecordId, recordId)
               .eq(Prescription::getStatus, 0); // Pending payment
        List<Prescription> prescriptions = prescriptionMapper.selectList(wrapper);

        List<Prescription> result = new ArrayList<>();
        for (Prescription p : prescriptions) {
            p.setStatus(1); // Paid
            prescriptionMapper.updateById(p);
            result.add(p);
        }
        return result;
    }

    @Override
    @Transactional
    public Prescription dispense(Long presId) {
        Prescription prescription = prescriptionMapper.selectById(presId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }

        if (prescription.getStatus() != 1) {
            throw new BusinessException("该处方未缴费或已发药");
        }

        // Deduct stock
        DrugDict drug = drugDictMapper.selectById(prescription.getDrugId());
        if (drug != null) {
            if (drug.getStockQuantity() < prescription.getQuantity()) {
                throw new BusinessException("库存不足: " + drug.getName());
            }
            drug.setStockQuantity(drug.getStockQuantity() - prescription.getQuantity());
            drugDictMapper.updateById(drug);
        }

        prescription.setStatus(2); // Dispensed
        prescriptionMapper.updateById(prescription);
        return prescription;
    }

    @Override
    @Transactional
    public DrugDict updateStock(Long drugId, Integer quantity) {
        DrugDict drug = drugDictMapper.selectById(drugId);
        if (drug == null) {
            throw new BusinessException("药品不存在");
        }
        drug.setStockQuantity(drug.getStockQuantity() + quantity);
        drugDictMapper.updateById(drug);
        return drug;
    }

    @Override
    @Transactional
    public DrugDict addDrug(DrugDict drug) {
        drug.setStatus(1);
        drugDictMapper.insert(drug);
        return drug;
    }
}
