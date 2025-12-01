package com.tinyhis.controller;

import com.tinyhis.dto.*;
import com.tinyhis.entity.Registration;
import com.tinyhis.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Registration Controller (挂号)
 */
@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Create registration (appointment)
     */
    @PostMapping("/create")
    public Result<Registration> createRegistration(@Valid @RequestBody RegistrationRequest request) {
        Registration registration = registrationService.createRegistration(request);
        return Result.success(registration);
    }

    /**
     * Get registration by ID
     */
    @GetMapping("/{regId}")
    public Result<Registration> getRegistration(@PathVariable Long regId) {
        Registration registration = registrationService.getRegistrationById(regId);
        return Result.success(registration);
    }

    /**
     * Get registrations by patient
     */
    @GetMapping("/patient/{patientId}")
    public Result<List<RegistrationDetailDTO>> getPatientRegistrations(@PathVariable Long patientId) {
        List<RegistrationDetailDTO> registrations = registrationService.getPatientRegistrationDetails(patientId);
        return Result.success(registrations);
    }

    /**
     * Pay for registration (模拟缴费)
     */
    @PostMapping("/pay/{regId}")
    public Result<Registration> payRegistration(@PathVariable Long regId) {
        Registration registration = registrationService.payRegistration(regId);
        return Result.success(registration);
    }

    /**
     * Check in with GPS
     */
    @PostMapping("/checkin")
    public Result<Registration> checkIn(@Valid @RequestBody CheckInRequest request) {
        Registration registration = registrationService.checkIn(request);
        return Result.success(registration);
    }

    /**
     * Cancel registration
     */
    @PostMapping("/cancel/{regId}")
    public Result<Boolean> cancelRegistration(@PathVariable Long regId) {
        boolean result = registrationService.cancelRegistration(regId);
        return Result.success(result);
    }
}
