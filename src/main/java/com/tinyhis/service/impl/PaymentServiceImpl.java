package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.dto.PaymentRequest;
import com.tinyhis.dto.PaymentResponse;
import com.tinyhis.entity.*;
import com.tinyhis.exception.BusinessException;
import com.tinyhis.mapper.*;
import com.tinyhis.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/**
 * Payment Service Implementation (模拟缴费)
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RegistrationMapper registrationMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final LabOrderMapper labOrderMapper;
    private final DrugDictMapper drugDictMapper;
    @Value("${app.registration.fee:50.00}")
    private BigDecimal registrationFee;
    @Value("${app.lab.default-price:100.00}")
    private BigDecimal defaultLabFee;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        switch (request.getPaymentType().toUpperCase()) {
            case "REGISTRATION":
                if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
                    throw new BusinessException("请选择要支付的挂号");
                }
                return payRegistration(request.getItemIds().get(0));
            case "PRESCRIPTION":
                if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
                    throw new BusinessException("请选择要支付的处方");
                }
                return payPrescriptions(request.getItemIds().get(0));
            case "LAB":
                if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
                    throw new BusinessException("请选择要支付的检查单");
                }
                return payLabOrder(request.getItemIds().get(0));
            default:
                throw new BusinessException("不支持的支付类型");
        }
    }

    @Override
    @Transactional
    public PaymentResponse payRegistration(Long regId) {
        Registration registration = registrationMapper.selectById(regId);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        if (registration.getStatus() != 0) {
            throw new BusinessException("该挂号已支付或状态异常");
        }

        // 模拟缴费成功
        registration.setStatus(1); // 已支付，等待就诊
        registrationMapper.updateById(registration);

        BigDecimal amount = registration.getFee() != null ? registration.getFee() : registrationFee;
        return new PaymentResponse(true, "挂号费支付成功", amount, "REGISTRATION");
    }

    @Override
    @Transactional
    public PaymentResponse payPrescriptions(Long recordId) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getRecordId, recordId)
               .eq(Prescription::getStatus, 0); // 待支付
        List<Prescription> prescriptions = prescriptionMapper.selectList(wrapper);

        if (prescriptions.isEmpty()) {
            throw new BusinessException("没有待支付的处方");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Prescription p : prescriptions) {
            // 计算处方价格
            DrugDict drug = drugDictMapper.selectById(p.getDrugId());
            if (drug != null) {
                BigDecimal itemPrice = drug.getPrice().multiply(new BigDecimal(p.getQuantity()));
                totalAmount = totalAmount.add(itemPrice);
            }
            
            // 更新状态为已支付
            p.setStatus(1);
            prescriptionMapper.updateById(p);
        }

        return new PaymentResponse(true, "药费支付成功", totalAmount, "PRESCRIPTION");
    }

    @Override
    @Transactional
    public PaymentResponse payLabOrder(Long orderId) {
        LabOrder order = labOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("检查单不存在");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException("该检查单已支付或状态异常");
        }

        // 模拟缴费成功
        order.setStatus(1); // 已支付，待检查
        labOrderMapper.updateById(order);

        BigDecimal amount = order.getPrice() != null ? order.getPrice() : defaultLabFee;
        return new PaymentResponse(true, "检查费支付成功", amount, "LAB");
    }
}
