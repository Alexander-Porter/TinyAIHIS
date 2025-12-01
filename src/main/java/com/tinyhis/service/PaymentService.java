package com.tinyhis.service;

import com.tinyhis.dto.PaymentRequest;
import com.tinyhis.dto.PaymentResponse;

/**
 * Payment Service Interface (模拟缴费)
 */
public interface PaymentService {

    /**
     * Process payment (模拟缴费)
     */
    PaymentResponse processPayment(PaymentRequest request);

    /**
     * Pay for registration
     */
    PaymentResponse payRegistration(Long regId);

    /**
     * Pay for all prescriptions in a medical record
     */
    PaymentResponse payPrescriptions(Long recordId);

    /**
     * Pay for lab order
     */
    PaymentResponse payLabOrder(Long orderId);
}
