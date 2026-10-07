package com.example.demo.controller;

import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentResponse;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.StaffActionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200", "http://localhost:4201", "http://127.0.0.1:4201", "http://localhost:4202", "http://127.0.0.1:4202"})
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final StaffActionService staffActionService;

    public AppointmentController(AppointmentService appointmentService, StaffActionService staffActionService) {
        this.appointmentService = appointmentService;
        this.staffActionService = staffActionService;
    }

    @GetMapping("/api/appointments")
    public List<AppointmentResponse> search(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) LocalDate date,
                                            @RequestParam(required = false) AppointmentStatus status,
                                            @RequestParam(required = false) Long doctorId,
                                            Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        Long effectiveDoctorId = doctorScope(actor, doctorId);
        return appointmentService.search(q, date, status, effectiveDoctorId);
    }

    @GetMapping("/api/appointments/{id}")
    public AppointmentResponse findById(@PathVariable Long id, Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        AppointmentResponse response = appointmentService.findById(id);
        authorizeAppointment(actor, response);
        return response;
    }

    @PostMapping("/api/appointments")
    @Transactional
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request,
            Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        authorizeRequestedProvider(actor, request);
        AppointmentResponse response = appointmentService.create(request);
        staffActionService.recordAction(actor, StaffActionService.APPOINTMENT_CREATED, StaffActionService.APPOINTMENT,
                response.id(), null, null, response);
        return ResponseEntity.created(URI.create("/api/appointments/" + response.id())).body(response);
    }

    @PutMapping("/api/appointments/{id}")
    @Transactional
    public AppointmentResponse update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request,
            Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        AppointmentResponse before = appointmentService.findByIdForUpdate(id);
        authorizeAppointment(actor, before);
        authorizeRequestedProvider(actor, request);
        AppointmentResponse after = appointmentService.update(id, request);
        staffActionService.recordAction(actor, StaffActionService.APPOINTMENT_UPDATED, StaffActionService.APPOINTMENT,
                id, null, before, after);
        return after;
    }

    @PatchMapping("/api/appointments/{id}/cancel")
    @Transactional
    public AppointmentResponse cancel(@PathVariable Long id, Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        AppointmentResponse before = appointmentService.findByIdForUpdate(id);
        authorizeAppointment(actor, before);
        AppointmentResponse after = appointmentService.cancel(id);
        if (before.status() != AppointmentStatus.CANCELLED) {
            staffActionService.recordAction(actor, StaffActionService.APPOINTMENT_CANCELLED,
                    StaffActionService.APPOINTMENT, id, null, before, after);
        }
        return after;
    }

    @DeleteMapping("/api/appointments/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        AppointmentResponse before = appointmentService.findByIdForUpdate(id);
        authorizeAppointment(actor, before);
        appointmentService.deleteCancelled(id);
        return ResponseEntity.noContent().build();
    }

    private Long doctorScope(ClinicPrincipal actor, Long requestedDoctorId) {
        if (actor.hasRole("doctor")) {
            if (requestedDoctorId != null && !actor.userId().equals(requestedDoctorId)) {
                throw new MessagingAccessDeniedException("Doctors can only access their own appointments.");
            }
            return actor.userId();
        }
        return requestedDoctorId;
    }

    private void authorizeAppointment(ClinicPrincipal actor, AppointmentResponse appointment) {
        if (actor.hasRole("doctor") && !actor.userId().equals(appointment.providerUserId())) {
            throw new MessagingAccessDeniedException("Doctors can only access their own appointments.");
        }
    }

    private void authorizeRequestedProvider(ClinicPrincipal actor, AppointmentRequest request) {
        if (actor.hasRole("doctor") && !actor.userId().equals(request.providerUserId())) {
            throw new MessagingAccessDeniedException("Doctors can only manage their own appointments.");
        }
    }

    private ClinicPrincipal requireStaff(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor") && !actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Only doctors and secretaries can change appointments.");
        }
        return actor;
    }
}
