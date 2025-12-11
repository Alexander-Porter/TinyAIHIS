package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.LabOrderView;
import com.tinyhis.dto.LabResultRequest;
import com.tinyhis.entity.LabOrder;
import com.tinyhis.entity.MedicalRecord;
import com.tinyhis.entity.PatientInfo;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.LabOrderMapper;
import com.tinyhis.mapper.MedicalRecordMapper;
import com.tinyhis.mapper.PatientInfoMapper;
import com.tinyhis.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 检验服务实现类
 */
@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

    private final LabOrderMapper labOrderMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PatientInfoMapper patientInfoMapper;

    @Override
    public List<LabOrderView> getOrders(String status) {
        String normalized = status == null ? "pending" : status.toLowerCase();

        LambdaQueryWrapper<LabOrder> wrapper = new LambdaQueryWrapper<>();
        switch (normalized) {
            case "completed" -> wrapper.eq(LabOrder::getStatus, 2);
            case "unpaid" -> wrapper.eq(LabOrder::getStatus, 0);
            case "all" -> { /* no status filter */ }
            default -> wrapper.eq(LabOrder::getStatus, 1);
        }

        wrapper.orderByAsc(LabOrder::getCreateTime);
        List<LabOrder> orders = labOrderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> recordIds = orders.stream()
                .map(LabOrder::getRecordId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, MedicalRecord> recordMap = recordIds.isEmpty()
                ? Collections.emptyMap()
                : medicalRecordMapper.selectBatchIds(recordIds).stream()
                    .collect(Collectors.toMap(MedicalRecord::getRecordId, r -> r));

        Set<Long> patientIds = recordMap.values().stream()
                .map(MedicalRecord::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, PatientInfo> patientMap = patientIds.isEmpty()
                ? Collections.emptyMap()
                : patientInfoMapper.selectBatchIds(patientIds).stream()
                    .collect(Collectors.toMap(PatientInfo::getPatientId, p -> p));

        List<LabOrderView> result = new ArrayList<>(orders.size());
        for (LabOrder order : orders) {
            LabOrderView view = new LabOrderView();
            view.setOrderId(order.getOrderId());
            view.setRecordId(order.getRecordId());
            view.setItemName(order.getItemName());
            view.setPrice(order.getPrice());
            view.setStatus(order.getStatus());
            view.setResultText(order.getResultText());
            view.setResultImages(order.getResultImages());
            view.setCreateTime(order.getCreateTime());
            view.setUpdateTime(order.getUpdateTime());

            MedicalRecord record = recordMap.get(order.getRecordId());
            if (record != null) {
                PatientInfo patient = patientMap.get(record.getPatientId());
                if (patient != null) {
                    view.setPatientName(patient.getName());
                    view.setGender(patient.getGender());
                    view.setAge(patient.getAge());
                }
            }

            result.add(view);
        }
        return result;
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
        order.setStatus(2); // 已完成
        labOrderMapper.updateById(order);

        return order;
    }

    @Override
    public List<LabOrder> getOrdersByPatient(Long patientId) {
        // 首先获取该患者的所有就诊记录
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
