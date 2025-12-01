package com.tinyhis.controller;

import com.tinyhis.dto.LabResultRequest;
import com.tinyhis.dto.Result;
import com.tinyhis.entity.LabOrder;
import com.tinyhis.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lab Controller (检验科)
 */
@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    /**
     * Get pending lab orders
     */
    @GetMapping("/pending")
    public Result<List<LabOrder>> getPendingOrders() {
        List<LabOrder> orders = labService.getPendingOrders();
        return Result.success(orders);
    }

    /**
     * Get lab order by ID
     */
    @GetMapping("/{orderId}")
    public Result<LabOrder> getOrder(@PathVariable Long orderId) {
        LabOrder order = labService.getOrderById(orderId);
        return Result.success(order);
    }

    /**
     * Pay for lab order (模拟缴费)
     */
    @PostMapping("/pay/{orderId}")
    public Result<LabOrder> payLabOrder(@PathVariable Long orderId) {
        LabOrder order = labService.payLabOrder(orderId);
        return Result.success(order);
    }

    /**
     * Submit lab result
     */
    @PostMapping("/result")
    public Result<LabOrder> submitResult(@RequestBody LabResultRequest request) {
        LabOrder order = labService.submitResult(request);
        return Result.success(order);
    }

    /**
     * Get lab orders by patient
     */
    @GetMapping("/patient/{patientId}")
    public Result<List<LabOrder>> getPatientOrders(@PathVariable Long patientId) {
        List<LabOrder> orders = labService.getOrdersByPatient(patientId);
        return Result.success(orders);
    }
}
