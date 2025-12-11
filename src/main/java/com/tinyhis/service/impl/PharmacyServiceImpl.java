package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.PrescriptionDetailDTO;
import com.tinyhis.entity.DrugDict;
import com.tinyhis.entity.MedicalRecord;
import com.tinyhis.entity.PatientInfo;
import com.tinyhis.entity.Prescription;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.DrugDictMapper;
import com.tinyhis.mapper.MedicalRecordMapper;
import com.tinyhis.mapper.PatientInfoMapper;
import com.tinyhis.mapper.PrescriptionMapper;
import com.tinyhis.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pharmacy Service Implementation
 */
@Service
@RequiredArgsConstructor
public class PharmacyServiceImpl implements PharmacyService {

    private final DrugDictMapper drugDictMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PatientInfoMapper patientInfoMapper;

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
    public List<PrescriptionDetailDTO> getPaidPrescriptions() {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getStatus, 1) // 已支付，等待配药
               .orderByAsc(Prescription::getCreateTime);
        List<Prescription> prescriptions = prescriptionMapper.selectList(wrapper);
        
        if (prescriptions.isEmpty()) {
            return new ArrayList<>();
        }

        List<PrescriptionDetailDTO> result = new ArrayList<>();
        
        // 批量获取ID
        List<Long> drugIds = prescriptions.stream().map(Prescription::getDrugId).distinct().collect(Collectors.toList());
        List<Long> recordIds = prescriptions.stream().map(Prescription::getRecordId).distinct().collect(Collectors.toList());
        
        Map<Long, DrugDict> drugMap = new java.util.HashMap<>();
        if (!drugIds.isEmpty()) {
             drugMap = drugDictMapper.selectBatchIds(drugIds).stream()
                .collect(Collectors.toMap(DrugDict::getDrugId, d -> d));
        }
                
        Map<Long, MedicalRecord> recordMap = new java.util.HashMap<>();
        Map<Long, PatientInfo> patientMap = new java.util.HashMap<>();
        
        if (!recordIds.isEmpty()) {
            List<MedicalRecord> records = medicalRecordMapper.selectBatchIds(recordIds);
            recordMap = records.stream()
                    .collect(Collectors.toMap(MedicalRecord::getRecordId, r -> r));
            
            List<Long> patientIds = records.stream().map(MedicalRecord::getPatientId).distinct().collect(Collectors.toList());
            if (!patientIds.isEmpty()) {
                patientMap = patientInfoMapper.selectBatchIds(patientIds).stream()
                    .collect(Collectors.toMap(PatientInfo::getPatientId, p -> p));
            }
        }

        for (Prescription p : prescriptions) {
            PrescriptionDetailDTO dto = new PrescriptionDetailDTO();
            BeanUtils.copyProperties(p, dto);
            
            DrugDict drug = drugMap.get(p.getDrugId());
            if (drug != null) {
                dto.setDrugName(drug.getName());
                dto.setDrugSpec(drug.getSpec());
                dto.setUnitPrice(drug.getPrice());
            }
            
            MedicalRecord record = recordMap.get(p.getRecordId());
            if (record != null) {
                PatientInfo patient = patientMap.get(record.getPatientId());
                if (patient != null) {
                    dto.setPatientName(patient.getName());
                }
            }
            
            result.add(dto);
        }
        
        return result;
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
        prescription.setStatus(1); // 已支付
        prescriptionMapper.updateById(prescription);
        return prescription;
    }

    @Override
    @Transactional
    public List<Prescription> payPrescriptionsByRecord(Long recordId) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getRecordId, recordId)
               .eq(Prescription::getStatus, 0); // 待支付
        List<Prescription> prescriptions = prescriptionMapper.selectList(wrapper);

        List<Prescription> result = new ArrayList<>();
        for (Prescription p : prescriptions) {
            p.setStatus(1); // 已支付
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

        // 扣减库存
        DrugDict drug = drugDictMapper.selectById(prescription.getDrugId());
        if (drug != null) {
            if (drug.getStockQuantity() < prescription.getQuantity()) {
                throw new BusinessException("库存不足: " + drug.getName());
            }
            drug.setStockQuantity(drug.getStockQuantity() - prescription.getQuantity());
            drugDictMapper.updateById(drug);
        }

        prescription.setStatus(2); // 已发药
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
