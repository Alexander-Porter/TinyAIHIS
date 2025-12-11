package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.EmrRequest;
import com.tinyhis.dto.PrescriptionDetailDTO;
import com.tinyhis.entity.*;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.*;
import com.tinyhis.service.EmrService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EMR Service Implementation
 */
@Service
@RequiredArgsConstructor
public class EmrServiceImpl implements EmrService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final LabOrderMapper labOrderMapper;
    private final EmrTemplateMapper emrTemplateMapper;
    private final RegistrationMapper registrationMapper;
    private final DrugDictMapper drugDictMapper;

    @Override
    @Transactional
    public MedicalRecord saveEmr(EmrRequest request, Long doctorId) {
        Registration registration = registrationMapper.selectById(request.getRegId());
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        // 创建或更新病历
        MedicalRecord record = getRecordByRegId(request.getRegId());
        if (record == null) {
            record = new MedicalRecord();
            record.setRegId(request.getRegId());
            record.setPatientId(registration.getPatientId());
            record.setDoctorId(doctorId);
        }

        record.setSymptom(request.getSymptom());
        record.setDiagnosis(request.getDiagnosis());
        record.setContent(request.getContent());

        if (record.getRecordId() == null) {
            medicalRecordMapper.insert(record);
        } else {
            medicalRecordMapper.updateById(record);
        }

        // 保存处方
        if (request.getPrescriptions() != null && !request.getPrescriptions().isEmpty()) {
            for (EmrRequest.PrescriptionItem item : request.getPrescriptions()) {
                Prescription prescription = new Prescription();
                prescription.setRecordId(record.getRecordId());
                prescription.setDrugId(item.getDrugId());
                prescription.setQuantity(item.getQuantity());
                prescription.setUsageInstruction(item.getUsageInstruction());
                prescription.setStatus(0); // 待支付
                prescriptionMapper.insert(prescription);
            }
        }

        // 保存检查单
        if (request.getLabOrders() != null && !request.getLabOrders().isEmpty()) {
            for (EmrRequest.LabOrderItem item : request.getLabOrders()) {
                LabOrder labOrder = new LabOrder();
                labOrder.setRecordId(record.getRecordId());
                labOrder.setItemName(item.getItemName());
                labOrder.setPrice(item.getPrice());
                labOrder.setStatus(0); // 待支付
                labOrderMapper.insert(labOrder);
            }
        }

        return record;
    }

    @Override
    public MedicalRecord getRecordById(Long recordId) {
        return medicalRecordMapper.selectById(recordId);
    }

    @Override
    public List<MedicalRecord> getRecordsByPatient(Long patientId) {
        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalRecord::getPatientId, patientId)
                .orderByDesc(MedicalRecord::getCreateTime);
        return medicalRecordMapper.selectList(wrapper);
    }

    @Override
    public MedicalRecord getRecordByRegId(Long regId) {
        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalRecord::getRegId, regId);
        return medicalRecordMapper.selectOne(wrapper);
    }

    @Override
    public List<Prescription> getPrescriptionsByRecord(Long recordId) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getRecordId, recordId);
        return prescriptionMapper.selectList(wrapper);
    }

    @Override
    public List<PrescriptionDetailDTO> getPrescriptionDetails(Long recordId) {
        List<Prescription> list = getPrescriptionsByRecord(recordId);
        return list.stream().map(p -> {
            PrescriptionDetailDTO dto = new PrescriptionDetailDTO();
            BeanUtils.copyProperties(p, dto);

            DrugDict drug = drugDictMapper.selectById(p.getDrugId());
            if (drug != null) {
                dto.setDrugName(drug.getName());
                dto.setDrugSpec(drug.getSpec());
                dto.setUnitPrice(drug.getPrice());
                dto.setTotalPrice(drug.getPrice().multiply(new BigDecimal(p.getQuantity())));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<LabOrder> getLabOrdersByRecord(Long recordId) {
        LambdaQueryWrapper<LabOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabOrder::getRecordId, recordId);
        return labOrderMapper.selectList(wrapper);
    }

    @Override
    public List<EmrTemplate> getTemplatesByDept(Long deptId) {
        LambdaQueryWrapper<EmrTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(EmrTemplate::getDeptId).or().eq(EmrTemplate::getDeptId, deptId))
                .eq(EmrTemplate::getStatus, 1);
        return emrTemplateMapper.selectList(wrapper);
    }

    @Override
    public EmrTemplate saveTemplate(EmrTemplate template) {
        if (template.getTplId() == null) {
            template.setStatus(1);
            emrTemplateMapper.insert(template);
        } else {
            emrTemplateMapper.updateById(template);
        }
        return template;
    }

    @Override
    public boolean deleteTemplate(Long tplId) {
        EmrTemplate template = emrTemplateMapper.selectById(tplId);
        if (template == null) {
            return false;
        }
        template.setStatus(0);
        emrTemplateMapper.updateById(template);
        return true;
    }
}
