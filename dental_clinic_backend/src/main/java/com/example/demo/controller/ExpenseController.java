package com.example.demo.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.DoctorInsightDto;
import com.example.demo.dto.ExpenseDto;
import com.example.demo.entity.ExpenseStatus;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.ExpenseService;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.StaffActionService;

import jakarta.validation.Valid;

@RestController
public class ExpenseController {
    private final ExpenseService expenseService;
    private final StaffActionService staffActionService;

    public ExpenseController(ExpenseService expenseService, StaffActionService staffActionService) {
        this.expenseService = expenseService;
        this.staffActionService = staffActionService;
    }

    @GetMapping("/api/expenses")
    public List<ExpenseDto.Response> search(@RequestParam(required = false) String q, @RequestParam(required = false) ExpenseStatus status) {
        return expenseService.search(q, status);
    }

    @PostMapping("/api/expenses")
    @Transactional
    public ResponseEntity<ExpenseDto.Response> create(@Valid @RequestBody ExpenseDto.Request request,
            Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        ExpenseDto.Response response = expenseService.create(request);
        staffActionService.recordAction(actor, StaffActionService.EXPENSE_CREATED, StaffActionService.EXPENSE,
                response.id(), null, null, response);
        return ResponseEntity.created(URI.create("/api/expenses/" + response.id())).body(response);
    }

    @PutMapping("/api/expenses/{id}")
    @Transactional
    public ExpenseDto.Response update(@PathVariable Long id, @Valid @RequestBody ExpenseDto.Request request,
            Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        ExpenseDto.Response before = expenseService.findById(id);
        ExpenseDto.Response after = expenseService.update(id, request);
        staffActionService.recordAction(actor, StaffActionService.EXPENSE_UPDATED, StaffActionService.EXPENSE,
                id, null, before, after);
        return after;
    }

    @DeleteMapping("/api/expenses/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        ExpenseDto.Response before = expenseService.findById(id);
        expenseService.delete(id);
        staffActionService.recordAction(actor, StaffActionService.EXPENSE_DELETED, StaffActionService.EXPENSE,
                id, null, before, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/doctor/insights/financial")
    public DoctorInsightDto.FinancialInsight doctorFinancialInsights() {
        return expenseService.doctorFinancialInsights();
    }

    private ClinicPrincipal requireStaff(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor") && !actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Only doctors and secretaries can change expenses.");
        }
        return actor;
    }
}
