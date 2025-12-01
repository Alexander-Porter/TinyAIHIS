package com.tinyhis.controller;

import com.tinyhis.dto.Result;
import com.tinyhis.entity.Registration;
import com.tinyhis.service.QueueService;
import com.tinyhis.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Doctor Workstation Controller
 */
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final RegistrationService registrationService;
    private final QueueService queueService;

    /**
     * Get waiting queue for doctor
     */
    @GetMapping("/queue/{doctorId}")
    public Result<List<Registration>> getWaitingQueue(@PathVariable Long doctorId) {
        List<Registration> queue = registrationService.getWaitingQueue(doctorId);
        return Result.success(queue);
    }

    /**
     * Call next patient
     */
    @PostMapping("/callNext/{doctorId}")
    public Result<Registration> callNext(@PathVariable Long doctorId) {
        Registration registration = registrationService.callNext(doctorId);
        return Result.success(registration);
    }

    /**
     * Complete consultation
     */
    @PostMapping("/complete/{regId}")
    public Result<Registration> completeConsultation(@PathVariable Long regId) {
        Registration registration = registrationService.completeConsultation(regId);
        return Result.success(registration);
    }
}
