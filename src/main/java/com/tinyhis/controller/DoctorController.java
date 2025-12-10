package com.tinyhis.controller;

import com.tinyhis.dto.Result;
import com.tinyhis.dto.VisitDetailDTO;
import com.tinyhis.entity.Registration;
import com.tinyhis.service.DoctorWorkstationService;
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
    private final DoctorWorkstationService doctorWorkstationService;

    /**
     * Get waiting queue for doctor (status=2: checked in/waiting)
     */
    @GetMapping("/queue/{doctorId}")
    public Result<List<Registration>> getWaitingQueue(@PathVariable Long doctorId) {
        List<Registration> queue = registrationService.getWaitingQueue(doctorId);
        return Result.success(queue);
    }

    /**
     * Get all active patients for doctor today (status 2,3 = checked in or in
     * consultation)
     */
    @GetMapping("/patients/{doctorId}")
    public Result<List<VisitDetailDTO>> getTodayPatients(@PathVariable Long doctorId) {
        List<VisitDetailDTO> patients = doctorWorkstationService.getTodayPatients(doctorId);
        return Result.success(patients);
    }

    /**
     * Get complete visit detail for a specific registration
     */
    @GetMapping("/visit/{regId}")
    public Result<VisitDetailDTO> getVisitDetail(@PathVariable Long regId) {
        VisitDetailDTO detail = doctorWorkstationService.getVisitDetail(regId);
        return Result.success(detail);
    }

    /**
     * Get patient's visit history (past visits with this doctor)
     */
    @GetMapping("/history/{patientId}")
    public Result<List<VisitDetailDTO>> getPatientHistory(
            @PathVariable Long patientId,
            @RequestParam(required = false) Long doctorId) {
        List<VisitDetailDTO> history = doctorWorkstationService.getPatientHistory(patientId, doctorId);
        return Result.success(history);
    }

    /**
     * Pause current consultation (patient needs to do lab tests)
     * Changes status from 3 (in consultation) back to 2 (waiting)
     */
    @PostMapping("/pause/{regId}")
    public Result<Registration> pauseConsultation(@PathVariable Long regId) {
        Registration registration = doctorWorkstationService.pauseConsultation(regId);
        return Result.success(registration);
    }

    /**
     * Resume consultation (patient returned after lab tests)
     * Changes status from 2 (waiting) to 3 (in consultation)
     */
    @PostMapping("/resume/{regId}")
    public Result<Registration> resumeConsultation(@PathVariable Long regId) {
        Registration registration = doctorWorkstationService.resumeConsultation(regId);
        return Result.success(registration);
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
     * Call specific patient
     */
    @PostMapping("/call/{doctorId}/{regId}")
    public Result<Registration> callSpecificPatient(@PathVariable Long doctorId, @PathVariable Long regId) {
        Registration registration = registrationService.callSpecificPatient(doctorId, regId);
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
