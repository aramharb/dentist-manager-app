package com.example.demo.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.BookingDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.BookingRequestService;
import com.example.demo.service.MessagingAccessDeniedException;

import jakarta.validation.Valid;

/** Appointment requests sent from the client space: only the cabinet's secretaries handle them. */
@RestController
@RequestMapping("/api/booking-requests")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
public class BookingRequestController {
    private final BookingRequestService service;

    public BookingRequestController(BookingRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<BookingDto.Pending> pending(Principal principal) {
        return service.pending(secretary(principal));
    }

    @GetMapping("/count")
    public Map<String, Long> count(Principal principal) {
        return Map.of("pending", service.pendingCount(secretary(principal)));
    }

    @PostMapping("/{id}/confirm")
    public BookingDto.Confirmed confirm(@PathVariable Long id, @RequestBody(required = false) BookingDto.ConfirmRequest body,
            Principal principal) {
        return service.confirm(id, body, secretary(principal));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id, @Valid @RequestBody(required = false) BookingDto.RejectRequest body,
            Principal principal) {
        service.reject(id, body, secretary(principal));
        return ResponseEntity.noContent().build();
    }

    private ClinicPrincipal secretary(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Only a secretary can handle client appointment requests.");
        }
        return actor;
    }
}
