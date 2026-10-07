package com.example.demo.controller;

import java.security.Principal;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.DoctorWorkingHoursDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.DoctorWorkingHoursService;
import com.example.demo.service.MessagingAccessDeniedException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/doctors/{doctorId}/working-hours")
public class DoctorWorkingHoursController {
    private final DoctorWorkingHoursService hoursService;

    public DoctorWorkingHoursController(DoctorWorkingHoursService hoursService) {
        this.hoursService = hoursService;
    }

    @GetMapping
    public DoctorWorkingHoursDto.Response find(@PathVariable Long doctorId, Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        if (actor.hasRole("doctor") && !actor.userId().equals(doctorId)) {
            throw new MessagingAccessDeniedException("Doctors can only access their own working hours.");
        }
        return hoursService.findByDoctor(doctorId);
    }

    @PutMapping
    @Transactional
    public DoctorWorkingHoursDto.Response update(@PathVariable Long doctorId,
            @Valid @RequestBody DoctorWorkingHoursDto.UpdateRequest request, Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor")) {
            throw new MessagingAccessDeniedException("Only doctors can change working hours.");
        }
        if (!actor.userId().equals(doctorId)) {
            throw new MessagingAccessDeniedException("Doctors can only change their own working hours.");
        }
        return hoursService.update(doctorId, request);
    }

    private ClinicPrincipal requireStaff(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor") && !actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Only doctors and secretaries can change working hours.");
        }
        return actor;
    }
}
