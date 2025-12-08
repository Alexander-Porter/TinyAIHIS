package com.tinyhis.controller;

import com.tinyhis.dto.LabOrderView;
import com.tinyhis.dto.LabResultRequest;
import com.tinyhis.dto.Result;
import com.tinyhis.entity.LabOrder;
import com.tinyhis.service.LabService;
import com.tinyhis.annotation.CheckUserAccess;
import com.tinyhis.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Lab Controller (检验科)
 */
@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Get lab orders with status filter
     */
    @GetMapping("/orders")
    public Result<List<LabOrderView>> getOrders(@RequestParam(defaultValue = "pending") String status) {
        List<LabOrderView> orders = labService.getOrders(status);
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
     * Upload image for lab result
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadResultImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            String ext = "";
            if (StringUtils.hasText(original) && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/" + filename;
            return Result.success(url);
        } catch (IOException e) {
            throw new BusinessException("图片上传失败，请重试");
        }
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
    @CheckUserAccess(paramName = "patientId")
    public Result<List<LabOrder>> getPatientOrders(@PathVariable Long patientId) {
        List<LabOrder> orders = labService.getOrdersByPatient(patientId);
        return Result.success(orders);
    }
}
