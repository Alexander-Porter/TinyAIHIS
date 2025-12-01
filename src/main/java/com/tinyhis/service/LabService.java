package com.tinyhis.service;

import com.tinyhis.dto.LabResultRequest;
import com.tinyhis.entity.LabOrder;

import java.util.List;

/**
 * Lab Service Interface
 */
public interface LabService {

    /**
     * Get pending lab orders
     */
    List<LabOrder> getPendingOrders();

    /**
     * Get lab order by ID
     */
    LabOrder getOrderById(Long orderId);

    /**
     * Pay for lab order (模拟缴费)
     */
    LabOrder payLabOrder(Long orderId);

    /**
     * Submit lab result
     */
    LabOrder submitResult(LabResultRequest request);

    /**
     * Get lab orders by patient
     */
    List<LabOrder> getOrdersByPatient(Long patientId);
}
