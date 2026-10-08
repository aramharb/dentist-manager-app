package com.example.demo.controller;

import java.net.URI;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.service.PatientService;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.StaffActionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final StaffActionService staffActionService;

    public PatientController(PatientService patientService, StaffActionService staffActionService) {
        this.patientService = patientService;
        this.staffActionService = staffActionService;
    }

    @GetMapping
    public List<PatientResponse> findAll(Principal principal) {
        return patientService.findAll(requireStaff(principal));
    }

    @GetMapping("/{id}")
    public PatientResponse findById(@PathVariable Long id, Principal principal) {
        return patientService.findById(id, requireStaff(principal));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request, Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        PatientResponse response = patientService.create(request, actor);
        staffActionService.recordAction(actor, StaffActionService.PATIENT_CREATED, StaffActionService.PATIENT,
                response.getId(), null, null, response);
        return ResponseEntity.created(URI.create("/api/patients/" + response.getId())).body(response);
    }

    @PutMapping("/{id}")
    @Transactional
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request,
            Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        PatientResponse before = patientService.findById(id, actor);
        PatientResponse after = patientService.update(id, request, actor);
        String actionType = balanceChanged(before, after)
                ? StaffActionService.PATIENT_BALANCE_UPDATED
                : StaffActionService.PATIENT_UPDATED;
        staffActionService.recordAction(actor, actionType, StaffActionService.PATIENT, id, null, before, after);
        return after;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor")) {
            throw new MessagingAccessDeniedException("Only a doctor can delete patients.");
        }
        patientService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    private static boolean balanceChanged(PatientResponse before, PatientResponse after) {
        return amount(before.getExpectedAmount()).compareTo(amount(after.getExpectedAmount())) != 0
                || amount(before.getPaidAmount()).compareTo(amount(after.getPaidAmount())) != 0
                || amount(before.getUnpaidBalance()).compareTo(amount(after.getUnpaidBalance())) != 0;
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ClinicPrincipal requireStaff(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor") && !actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Only doctors and secretaries can change patients.");
        }
        return actor;
    }
}
