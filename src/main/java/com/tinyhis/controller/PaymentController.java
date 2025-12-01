package com.tinyhis.controller;

import com.tinyhis.dto.PaymentRequest;
import com.tinyhis.dto.PaymentResponse;
import com.tinyhis.dto.Result;
import com.tinyhis.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Payment Controller (模拟缴费)
 * 统一的缴费接口，支持挂号费、药费、检查费缴费
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 统一支付接口
     */
    @PostMapping("/pay")
    public Result<PaymentResponse> pay(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return Result.success(response);
    }

    /**
     * 挂号费缴费
     */
    @PostMapping("/registration/{regId}")
    public Result<PaymentResponse> payRegistration(@PathVariable Long regId) {
        PaymentResponse response = paymentService.payRegistration(regId);
        return Result.success(response);
    }

    /**
     * 药费缴费 (按病历记录ID)
     */
    @PostMapping("/prescription/record/{recordId}")
    public Result<PaymentResponse> payPrescriptions(@PathVariable Long recordId) {
        PaymentResponse response = paymentService.payPrescriptions(recordId);
        return Result.success(response);
    }

    /**
     * 检查费缴费
     */
    @PostMapping("/lab/{orderId}")
    public Result<PaymentResponse> payLabOrder(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.payLabOrder(orderId);
        return Result.success(response);
    }
}
