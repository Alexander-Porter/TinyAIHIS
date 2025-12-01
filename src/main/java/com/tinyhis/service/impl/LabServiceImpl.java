package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.LabResultRequest;
import com.tinyhis.entity.LabOrder;
import com.tinyhis.entity.MedicalRecord;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.LabOrderMapper;
import com.tinyhis.mapper.MedicalRecordMapper;
import com.tinyhis.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Lab Service Implementation
 */
@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

    private final LabOrderMapper labOrderMapper;
    private final MedicalRecordMapper medicalRecordMapper;

    @Override
    public List<LabOrder> getPendingOrders() {
        LambdaQueryWrapper<LabOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabOrder::getStatus, 1) // Paid, pending examination
               .orderByAsc(LabOrder::getCreateTime);
        return labOrderMapper.selectList(wrapper);
    }

    @Override
    public LabOrder getOrderById(Long orderId) {
        return labOrderMapper.selectById(orderId);
    }

    @Override
    @Transactional
    public LabOrder payLabOrder(Long orderId) {
        LabOrder order = labOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("检查单不存在");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException("该检查单已支付或状态异常");
        }

        // 模拟缴费成功
        order.setStatus(1); // Paid, pending examination
        labOrderMapper.updateById(order);
        return order;
    }

    @Override
    @Transactional
    public LabOrder submitResult(LabResultRequest request) {
        LabOrder order = labOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("检查单不存在");
        }

        if (order.getStatus() != 1) {
            throw new BusinessException("该检查单未缴费或已完成");
        }

        order.setResultText(request.getResultText());
        order.setResultImages(request.getResultImages());
        order.setStatus(2); // Completed
        labOrderMapper.updateById(order);

        return order;
    }

    @Override
    public List<LabOrder> getOrdersByPatient(Long patientId) {
        // First get all records for this patient
        LambdaQueryWrapper<MedicalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(MedicalRecord::getPatientId, patientId);
        List<MedicalRecord> records = medicalRecordMapper.selectList(recordWrapper);

        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> recordIds = records.stream().map(MedicalRecord::getRecordId).toList();

        LambdaQueryWrapper<LabOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LabOrder::getRecordId, recordIds)
               .orderByDesc(LabOrder::getCreateTime);
        return labOrderMapper.selectList(wrapper);
    }
}
