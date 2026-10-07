package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.dto.ExpenseDto;
import com.example.demo.dto.MaterialDto;
import com.example.demo.entity.Material;
import com.example.demo.entity.MaterialStatus;
import com.example.demo.repository.MaterialRepository;
import com.example.demo.security.ClinicPrincipal;

class MaterialServiceTests {
    private MaterialRepository materialRepository;
    private ExpenseService expenseService;
    private MaterialService materialService;
    private final ClinicPrincipal secretary = new ClinicPrincipal(4L, "secretary", "secretaire");

    @BeforeEach
    void setUp() {
        materialRepository = mock(MaterialRepository.class);
        expenseService = mock(ExpenseService.class);
        materialService = new MaterialService(materialRepository, expenseService);
        when(materialRepository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createMaterialCreatesExactlyOneLinkedExpense() {
        ExpenseDto.Response expense = mock(ExpenseDto.Response.class);
        when(expense.id()).thenReturn(91L);
        when(expenseService.create(any(ExpenseDto.Request.class))).thenReturn(expense);

        MaterialDto.Response response = materialService.create(validRequest(), secretary);

        assertEquals(91L, response.purchaseExpenseId());
        verify(expenseService).create(any(ExpenseDto.Request.class));
    }

    @Test
    void updateMaterialDoesNotCreateAnotherExpense() {
        Material existing = new Material();
        existing.setPurchaseExpenseId(91L);
        when(materialRepository.findById(7L)).thenReturn(Optional.of(existing));

        MaterialDto.Response response = materialService.update(7L, validRequest(), secretary);

        assertEquals(91L, response.purchaseExpenseId());
        verify(expenseService, never()).create(any(ExpenseDto.Request.class));
    }

    @Test
    void assigningCostToLegacyMaterialCreatesItsFirstExpense() {
        Material existing = new Material();
        when(materialRepository.findById(8L)).thenReturn(Optional.of(existing));
        ExpenseDto.Response expense = mock(ExpenseDto.Response.class);
        when(expense.id()).thenReturn(92L);
        when(expenseService.create(any(ExpenseDto.Request.class))).thenReturn(expense);

        MaterialDto.Response response = materialService.update(8L, validRequest(), secretary);

        assertEquals(92L, response.purchaseExpenseId());
        verify(expenseService).create(any(ExpenseDto.Request.class));
    }

    private MaterialDto.Request validRequest() {
        return new MaterialDto.Request("Composite Resin", "Restorative", 10, "boxes", 3,
                LocalDate.now().plusYears(1), "DentPlus", MaterialStatus.AVAILABLE, "B-100", 2,
                new BigDecimal("125.00"));
    }
}
